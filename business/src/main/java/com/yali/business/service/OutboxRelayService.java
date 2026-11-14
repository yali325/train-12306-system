package com.yali.business.service;

import com.yali.business.domain.OutboxEvent;
import com.yali.business.mapper.cust.OutboxEventMapper;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * 先在短事务中领取事件，提交后发送 MQ，再按 Broker 确认结果更新事件状态。
 * 发送与更新状态无法原子化，所以允许重复发送，由消费者幂等处理。
 */
@Service
public class OutboxRelayService {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelayService.class);

    @Resource
    private OutboxEventMapper outboxEventMapper;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private final TransactionTemplate claimTransaction;

    @Value("${outbox.dispatch.send-timeout-ms:10000}")
    private long sendTimeoutMs;

    @Value("${outbox.dispatch.lease-seconds:60}")
    private int leaseSeconds;

    public OutboxRelayService(PlatformTransactionManager transactionManager) {
        claimTransaction = new TransactionTemplate(transactionManager);
        claimTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public void drainReady() {
        while (!Thread.currentThread().isInterrupted()) {
            OutboxEvent event = claimOne();
            if (event == null) {
                return;
            }
            // Broker 故障时停止本轮扫描，让失败事件按 next_retry_at 退避。
            if (!sendAndRecord(event)) {
                return;
            }
        }
    }

    private OutboxEvent claimOne() {
        return claimTransaction.execute(status -> {
            OutboxEvent event = outboxEventMapper.selectReadyForUpdate();
            if (event == null) {
                return null;
            }
            String claimToken = UUID.randomUUID().toString();
            if (outboxEventMapper.markSending(event.getId(), claimToken, leaseSeconds) != 1) {
                throw new IllegalStateException("领取 Outbox 事件失败，eventId=" + event.getId());
            }
            event.setClaimToken(claimToken);
            event.setRetryCount(event.getRetryCount() + 1);
            return event;
        });
    }

    private boolean sendAndRecord(OutboxEvent event) {
        try {
            SendResult result = rocketMQTemplate.syncSend(
                    event.getTopic(), event.getPayload(), sendTimeoutMs);
            if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
                throw new IllegalStateException("Broker 未返回 SEND_OK：" + result);
            }
            int changed = outboxEventMapper.markSent(event.getId(), event.getClaimToken());
            if (changed == 0) {
                // 租约已被其他工作者重新领取；它可能再次发送，消费者必须幂等。
                LOG.warn("MQ 已确认但 Outbox 领取权已变化，eventId={}，msgId={}",
                        event.getId(), result.getMsgId());
            } else {
                LOG.info("Outbox 投递成功，eventId={}，orderId={}，msgId={}",
                        event.getId(), event.getOrderId(), result.getMsgId());
            }
            return true;
        } catch (Exception sendError) {
            long delaySeconds = retryDelaySeconds(event.getRetryCount());
            String error = sendError.toString();
            if (error.length() > 500) {
                error = error.substring(0, 500);
            }
            try {
                int changed = outboxEventMapper.markRetry(
                        event.getId(), event.getClaimToken(), delaySeconds, error);
                if (changed == 0) {
                    LOG.warn("发送失败后 Outbox 领取权已变化，eventId={}", event.getId());
                }
            } catch (Exception databaseError) {
                // 状态仍为 SENDING；租约到期后可再次领取。
                LOG.error("无法记录 Outbox 投递失败，等待租约恢复，eventId={}",
                        event.getId(), databaseError);
            }
            LOG.warn("Outbox 投递失败，eventId={}，下次最早 {} 秒后重试",
                    event.getId(), delaySeconds, sendError);
            return false;
        }
    }

    private long retryDelaySeconds(int retryCount) {
        int exponent = Math.min(Math.max(retryCount - 1, 0), 6);
        return Math.min(300L, 5L << exponent);
    }
}
