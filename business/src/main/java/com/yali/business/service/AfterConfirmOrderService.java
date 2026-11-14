package com.yali.business.service;

import com.alibaba.fastjson.JSON;
import com.yali.business.domain.ConfirmOrder;
import com.yali.business.domain.ConfirmOrderExample;
import com.yali.business.domain.DailyTrainSeat;
import com.yali.business.domain.DailyTrainTicket;
import com.yali.business.domain.OutboxEvent;
import com.yali.business.enums.ConfirmOrderStatusEnum;
import com.yali.business.enums.RocketMQTopicEnum;
import com.yali.business.mapper.ConfirmOrderMapper;
import com.yali.business.mapper.cust.DailyTrainSeatMapperCust;
import com.yali.business.mapper.cust.DailyTrainTicketMapperCust;
import com.yali.business.mapper.cust.OutboxEventMapper;
import com.yali.business.req.ConfirmOrderTicketReq;
import com.yali.common.dto.TicketIssuedEvent;
import com.yali.common.req.MemberTicketReq;
import com.yali.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class AfterConfirmOrderService {

    @Resource private DailyTrainSeatMapperCust seatMapper;
    @Resource private DailyTrainTicketMapperCust ticketMapper;
    @Resource private ConfirmOrderMapper orderMapper;
    @Resource private OutboxEventMapper outboxMapper;

    /**
     * 一笔订单的业务事实与会员车票事件一起提交。Feign 不进入本地事务和车次锁。
     * @return false 表示此订单已由其他事务领取或处理；没有写入任何库存。
     */
    @Transactional
    public boolean afterDoConfirm(DailyTrainTicket route, List<DailyTrainSeat> selected,
                                  Map<Long, String> originalSell, List<ConfirmOrderTicketReq> passengers,
                                  ConfirmOrder order) {
        ConfirmOrderExample initOnly = new ConfirmOrderExample();
        initOnly.createCriteria().andIdEqualTo(order.getId())
                .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
        ConfirmOrder pending = new ConfirmOrder();
        pending.setStatus(ConfirmOrderStatusEnum.PENDING.getCode());
        pending.setUpdateTime(new Date());
        if (orderMapper.updateByExampleSelective(pending, initOnly) != 1) {
            return false;
        }

        List<MemberTicketReq> memberTickets = new ArrayList<>();
        for (int j = 0; j < selected.size(); j++) {
            DailyTrainSeat seat = selected.get(j);
            String oldSell = originalSell.get(seat.getId());
            if (oldSell == null || seatMapper.updateSellIfUnchanged(
                    seat.getId(), oldSell, seat.getSell()) != 1) {
                throw new IllegalStateException("座位状态已变化，orderId=" + order.getId());
            }

            int startIndex = route.getStartIndex();
            int endIndex = route.getEndIndex();
            char[] bits = seat.getSell().toCharArray();
            int minStart = 0;
            for (int i = startIndex - 1; i >= 0; i--) {
                if (bits[i] == '1') { minStart = i + 1; break; }
            }
            int maxEnd = bits.length;
            for (int i = endIndex; i < bits.length; i++) {
                if (bits[i] == '1') { maxEnd = i; break; }
            }
            int expected = ticketMapper.countAffected(seat.getDate(), seat.getTrainCode(),
                    minStart, endIndex - 1, startIndex + 1, maxEnd);
            int changed = ticketMapper.updateCountBySell(seat.getDate(), seat.getTrainCode(),
                    seat.getSeatType(), minStart, endIndex - 1, startIndex + 1, maxEnd);
            if (expected <= 0 || changed != expected) {
                throw new IllegalStateException("区间余票更新不完整，orderId=" + order.getId());
            }

            MemberTicketReq memberTicket = new MemberTicketReq();
            memberTicket.setMemberId(order.getMemberId());
            memberTicket.setPassengerId(passengers.get(j).getPassengerId());
            memberTicket.setPassengerName(passengers.get(j).getPassengerName());
            memberTicket.setTrainDate(route.getDate());
            memberTicket.setTrainCode(route.getTrainCode());
            memberTicket.setCarriageIndex(seat.getCarriageIndex());
            memberTicket.setSeatRow(seat.getRow());
            memberTicket.setSeatCol(seat.getCol());
            memberTicket.setStartStation(route.getStart());
            memberTicket.setStartTime(route.getStartTime());
            memberTicket.setEndStation(route.getEnd());
            memberTicket.setEndTime(route.getEndTime());
            memberTicket.setSeatType(seat.getSeatType());
            memberTickets.add(memberTicket);
        }

        ConfirmOrderExample pendingOnly = new ConfirmOrderExample();
        pendingOnly.createCriteria().andIdEqualTo(order.getId())
                .andStatusEqualTo(ConfirmOrderStatusEnum.PENDING.getCode());
        ConfirmOrder success = new ConfirmOrder();
        success.setStatus(ConfirmOrderStatusEnum.SUCCESS.getCode());
        success.setUpdateTime(new Date());
        if (orderMapper.updateByExampleSelective(success, pendingOnly) != 1) {
            throw new IllegalStateException("订单成功状态写入失败，orderId=" + order.getId());
        }

        long eventId = SnowUtil.getSnowflakeNextId();
        TicketIssuedEvent message = new TicketIssuedEvent();
        message.setEventId(eventId);
        message.setOrderId(order.getId());
        message.setTickets(memberTickets);
        OutboxEvent event = new OutboxEvent();
        event.setId(eventId);
        event.setOrderId(order.getId());
        event.setEventType("TICKET_ISSUED");
        event.setTopic(RocketMQTopicEnum.TICKET_ISSUED.getCode());
        event.setPayload(JSON.toJSONString(message));
        event.setStatus(OutboxEvent.NEW);
        if (outboxMapper.insert(event) != 1) {
            throw new IllegalStateException("会员车票事件写入失败，orderId=" + order.getId());
        }
        return true;
    }
}
