package com.yali.business.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "outbox.dispatch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatchScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxDispatchScheduler.class);

    private final ThreadPoolTaskExecutor executor;
    private final OutboxRelayService relayService;

    public OutboxDispatchScheduler(
            @Qualifier("outboxDispatchExecutor") ThreadPoolTaskExecutor executor,
            OutboxRelayService relayService) {
        this.executor = executor;
        this.relayService = relayService;
    }

    @Scheduled(fixedDelayString = "${outbox.dispatch.poll-delay-ms:500}")
    public void submitAvailableWorkers() {
        int available = Math.max(0, executor.getMaxPoolSize() - executor.getActiveCount());
        for (int i = 0; i < available; i++) {
            try {
                executor.execute(relayService::drainReady);
            } catch (TaskRejectedException rejected) {
                // 线程池已满时留在数据库，下一个调度周期再试。
                LOG.debug("Outbox 投递线程池已满，本轮不再提交任务");
                break;
            }
        }
    }
}
