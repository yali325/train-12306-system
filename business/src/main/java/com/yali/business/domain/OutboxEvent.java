package com.yali.business.domain;

/**
 * 一条已随业务数据提交、等待后台投递的消息。
 * 数据库中的状态和租约由 OutboxEventMapper 维护。
 */
public class OutboxEvent {

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String NEW = "NEW";

    private Long id;
    private Long orderId;
    private String eventType;
    private String topic;
    private String payload;
    private String status;
    private Integer retryCount;
    private String claimToken;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getClaimToken() {
        return claimToken;
    }

    public void setClaimToken(String claimToken) {
        this.claimToken = claimToken;
    }
}
