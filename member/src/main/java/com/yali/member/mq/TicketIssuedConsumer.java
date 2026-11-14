package com.yali.member.mq;

import com.alibaba.fastjson.JSON;
import com.yali.common.dto.TicketIssuedEvent;
import com.yali.member.Service.TicketService;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(consumerGroup = "train-member-ticket-consumer", topic = "TICKET_ISSUED")
public class TicketIssuedConsumer implements RocketMQListener<String> {

    @Resource private TicketService ticketService;

    @Override
    public void onMessage(String body) {
        TicketIssuedEvent event = JSON.parseObject(body, TicketIssuedEvent.class);
        if (event == null || event.getOrderId() == null || event.getTickets() == null
                || event.getTickets().isEmpty()) {
            throw new IllegalArgumentException("车票事件缺少订单或乘客信息");
        }
        ticketService.saveIssued(event);
    }
}
