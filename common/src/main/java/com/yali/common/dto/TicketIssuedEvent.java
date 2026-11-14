package com.yali.common.dto;

import com.yali.common.req.MemberTicketReq;
import java.util.List;

/** 一笔订单出票后的会员车票快照；重复投递由 member 按订单和乘客幂等保存。 */
public class TicketIssuedEvent {
    private Long eventId;
    private Long orderId;
    private List<MemberTicketReq> tickets;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public List<MemberTicketReq> getTickets() { return tickets; }
    public void setTickets(List<MemberTicketReq> tickets) { this.tickets = tickets; }
}
