package com.yali.business.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
<<<<<<< HEAD
import com.alibaba.fastjson.JSON;
import com.yali.business.domain.ConfirmOrder;
import com.yali.business.dto.ConfirmOrderMQDto;
import com.yali.business.enums.ConfirmOrderStatusEnum;
import com.yali.business.enums.RocketMQTopicEnum;
import com.yali.business.mapper.ConfirmOrderMapper;
=======
>>>>>>> d16a315 (完善购票可靠性与高并发出票链路)
import com.yali.business.req.ConfirmOrderDoReq;
import com.yali.common.context.LoginMemberContext;
import com.yali.common.exception.BusinessException;
import com.yali.common.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
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
<<<<<<< HEAD
    private ConfirmOrderMapper confirmOrderMapper;

    @Autowired
    private SkTokenService skTokenService;

    // @Resource
    // public RocketMQTemplate rocket
    @Resource
     public RocketMQTemplate rocketMQTemplate;
=======
    private PurchaseRateService purchaseRateService;
>>>>>>> d16a315 (完善购票可靠性与高并发出票链路)

    @Resource
    private OrderCreateTxService orderCreateTxService;
    @Resource private PurchaseRequestMapper purchaseRequestMapper;

    @SentinelResource(value = "beforeDoConfirm", blockHandler = "beforeDoConfirmBlock")
    public Long beforeDoConfirm(ConfirmOrderDoReq req) {
<<<<<<< HEAD
        Long id = null;
        // 根据前端传值，加入排队人数
        for (int i = 0; i < req.getLineNumber() + 1; i++) {
            req.setMemberId(LoginMemberContext.getId());
            // 校验令牌余量
            boolean validSkToken = skTokenService.validSkToken(req.getDate(), req.getTrainCode(), LoginMemberContext.getId());
            if (validSkToken) {
                LOG.info("令牌校验通过");
            } else {
                LOG.info("令牌校验不通过");
                throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_SK_TOKEN_FAIL);
            }

            Date date = req.getDate();
            String trainCode = req.getTrainCode();
            String start = req.getStart();
            String end = req.getEnd();
            List<ConfirmOrderTicketReq> tickets = req.getTickets();

            // 保存确认订单表，状态初始
            DateTime now = DateTime.now();
            ConfirmOrder confirmOrder = new ConfirmOrder();
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrder.setMemberId(req.getMemberId());
            confirmOrder.setDate(date);
            confirmOrder.setTrainCode(trainCode);
            confirmOrder.setStart(start);
            confirmOrder.setEnd(end);
            confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
            confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
            confirmOrder.setTickets(JSON.toJSONString(tickets));
            confirmOrderMapper.insert(confirmOrder);

            // 发送MQ排队购票
            ConfirmOrderMQDto confirmOrderMQDto = new ConfirmOrderMQDto();
            confirmOrderMQDto.setDate(req.getDate());
            confirmOrderMQDto.setTrainCode(req.getTrainCode());
            confirmOrderMQDto.setLogId(MDC.get("LOG_ID"));
            String reqJson = JSON.toJSONString(confirmOrderMQDto);
             LOG.info("排队购票，发送mq开始，消息：{}", reqJson);
             rocketMQTemplate.convertAndSend(RocketMQTopicEnum.CONFIRM_ORDER.getCode(), reqJson);
             LOG.info("排队购票，发送mq结束");
//            confirmOrderService.doConfirm(confirmOrderMQDto);
            id = confirmOrder.getId();
=======
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
>>>>>>> d16a315 (完善购票可靠性与高并发出票链路)
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
