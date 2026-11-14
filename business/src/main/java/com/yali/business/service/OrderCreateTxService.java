package com.yali.business.service;

import cn.hutool.core.date.DateTime;
import com.alibaba.fastjson.JSON;
import com.yali.business.domain.ConfirmOrder;
import com.yali.business.domain.OutboxEvent;
import com.yali.business.dto.ConfirmOrderMQDto;
import com.yali.business.enums.ConfirmOrderStatusEnum;
import com.yali.business.enums.RocketMQTopicEnum;
import com.yali.business.mapper.ConfirmOrderMapper;
import com.yali.business.mapper.cust.OutboxEventMapper;
import com.yali.business.mapper.cust.PurchaseRequestMapper;
import com.yali.business.req.ConfirmOrderDoReq;
import com.yali.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单和待发送事件只写同一个 MySQL 事务；这里不访问 RocketMQ。
 */
@Service
public class OrderCreateTxService {

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Resource
    private OutboxEventMapper outboxEventMapper;

    @Resource private PurchaseRequestMapper purchaseRequestMapper;

    @Transactional
    public Long create(ConfirmOrderDoReq req, Long memberId, String logId) {
        DateTime now = DateTime.now();
        Long orderId = SnowUtil.getSnowflakeNextId();
        Long eventId = SnowUtil.getSnowflakeNextId();

        // 唯一请求键、订单和 Outbox 共用一个 MySQL 事务。
        purchaseRequestMapper.insert(memberId, req.getRequestId(), orderId);

        ConfirmOrder order = new ConfirmOrder();
        order.setId(orderId);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order.setMemberId(memberId);
        order.setDate(req.getDate());
        order.setTrainCode(req.getTrainCode());
        order.setStart(req.getStart());
        order.setEnd(req.getEnd());
        order.setDailyTrainTicketId(req.getDailyTrainTicketId());
        order.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
        order.setTickets(JSON.toJSONString(req.getTickets()));
        if (confirmOrderMapper.insert(order) != 1) {
            throw new IllegalStateException("创建确认订单失败");
        }

        ConfirmOrderMQDto message = new ConfirmOrderMQDto();
        message.setEventId(eventId);
        message.setOrderId(orderId);
        message.setDate(req.getDate());
        message.setTrainCode(req.getTrainCode());
        message.setLogId(logId);

        OutboxEvent event = new OutboxEvent();
        event.setId(eventId);
        event.setOrderId(orderId);
        event.setEventType(OutboxEvent.ORDER_CREATED);
        event.setTopic(RocketMQTopicEnum.CONFIRM_ORDER.getCode());
        event.setPayload(JSON.toJSONString(message));
        event.setStatus(OutboxEvent.NEW);
        if (outboxEventMapper.insert(event) != 1) {
            throw new IllegalStateException("创建待发消息失败");
        }

        return orderId;
    }
}
