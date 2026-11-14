package com.yali.business.dto;

import java.util.Date;

public class ConfirmOrderMQDto {
    /** Outbox 事件 ID，用于追踪可能的重复投递。 */
    private Long eventId;

    /** 对应的确认订单 ID；新版消费者按此 ID 精确处理一笔订单。 */
    private Long orderId;

    /**
     * 日志流程号，用于同转异时，用同一个流水号
     */
    private String logId;

    /**
     * 日期
     */
    private Date date;

    /**
     * 车次编号
     */
    private String trainCode;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTrainCode() {
        return trainCode;
    }

    public void setTrainCode(String trainCode) {
        this.trainCode = trainCode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfirmOrderMQDto{");
        sb.append("eventId=").append(eventId);
        sb.append(", orderId=").append(orderId);
        sb.append(", logId=").append(logId);
        sb.append(", date=").append(date);
        sb.append(", trainCode='").append(trainCode).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
