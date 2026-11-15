package com.yali.business.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.yali.business.req.ConfirmOrderDoReq;
import com.yali.common.context.LoginMemberContext;
import com.yali.common.exception.BusinessException;
import com.yali.common.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import com.yali.business.mapper.cust.PurchaseRequestMapper;

@Service
public class BeforeConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(BeforeConfirmOrderService.class);

    @Resource
    private PurchaseRateService purchaseRateService;

    @Resource
    private OrderCreateTxService orderCreateTxService;
    @Resource private PurchaseRequestMapper purchaseRequestMapper;

    @SentinelResource(value = "beforeDoConfirm", blockHandler = "beforeDoConfirmBlock")
    public Long beforeDoConfirm(ConfirmOrderDoReq req) {
        Long memberId = LoginMemberContext.getId();
        String logId = MDC.get("LOG_ID");
        Long existing = purchaseRequestMapper.findOrderId(memberId, req.getRequestId());
        if (existing != null) return existing;
        purchaseRateService.acquire(req.getDate(), req.getTrainCode());
        try {
            Long id = orderCreateTxService.create(req, memberId, logId);
            LOG.info("购票请求已受理，订单和 Outbox 已提交，orderId：{}", id);
            return id;
        } catch (DuplicateKeyException duplicate) {
            // 并发重复提交：唯一请求键只有一笔能创建成功。
            Long orderId = purchaseRequestMapper.findOrderId(memberId, req.getRequestId());
            if (orderId != null) return orderId;
            throw duplicate;
        }
    }

    /**
     * 降级方法，需包含限流方法的所有参数和BlockException参数
     * @param req
     * @param e
     */
    public Long beforeDoConfirmBlock(ConfirmOrderDoReq req, BlockException e) {
        LOG.info("购票请求被限流：{}", req);
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }
}
