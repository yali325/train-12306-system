package com.yali.business.mq;

import com.alibaba.fastjson.JSON;
import com.yali.business.dto.ConfirmOrderMQDto;
import com.yali.business.service.ConfirmOrderService;
import jakarta.annotation.Resource;
import org.apache.rocketmq.common.message.MessageExt;
<<<<<<< HEAD
=======
import org.apache.rocketmq.spring.annotation.ConsumeMode;
>>>>>>> d16a315 (完善购票可靠性与高并发出票链路)
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
<<<<<<< HEAD

 @Service
 @RocketMQMessageListener(consumerGroup = "train-confirm-order-consumer", topic = "CONFIRM_ORDER")
=======
import java.nio.charset.StandardCharsets;

 @Service
 @RocketMQMessageListener(consumerGroup = "train-confirm-order-consumer-v3",
         topic = "CONFIRM_ORDER", consumeMode = ConsumeMode.CONCURRENTLY)
>>>>>>> d16a315 (完善购票可靠性与高并发出票链路)
 public class ConfirmOrderConsumer implements RocketMQListener<MessageExt> {

     private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderConsumer.class);

     @Resource
     private ConfirmOrderService confirmOrderService;

     @Override
     public void onMessage(MessageExt messageExt) {
<<<<<<< HEAD
         byte[] body = messageExt.getBody();
         ConfirmOrderMQDto dto = JSON.parseObject(new String(body), ConfirmOrderMQDto.class);
         MDC.put("LOG_ID", dto.getLogId());
         LOG.info("ROCKETMQ收到消息：{}", new String(body));
         confirmOrderService.doConfirm(dto);
=======
         try {
             String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
             ConfirmOrderMQDto dto = JSON.parseObject(body, ConfirmOrderMQDto.class);
             if (dto == null) throw new IllegalArgumentException("购票消息体为空");
             if (dto.getLogId() != null) MDC.put("LOG_ID", dto.getLogId());
             LOG.info("收到购票消息，eventId={}，orderId={}，messageId={}",
                     dto.getEventId(), dto.getOrderId(), messageExt.getMsgId());
             // 同步调用；只有方法正常返回，RocketMQ 才把本次消费视为成功。
             confirmOrderService.doConfirm(dto);
         } finally {
             MDC.remove("LOG_ID");
         }
>>>>>>> d16a315 (完善购票可靠性与高并发出票链路)
     }
 }
