package com.yali.business.service;

import cn.hutool.core.date.DateTime;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.yali.business.domain.ConfirmOrder;
import com.yali.business.dto.ConfirmOrderMQDto;
import com.yali.business.enums.ConfirmOrderStatusEnum;
import com.yali.business.enums.RocketMQTopicEnum;
import com.yali.business.mapper.ConfirmOrderMapper;
import com.yali.business.req.ConfirmOrderDoReq;
import com.yali.business.req.ConfirmOrderTicketReq;
import com.yali.common.context.LoginMemberContext;
import com.yali.common.exception.BusinessException;
import com.yali.common.exception.BusinessExceptionEnum;
import com.yali.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BeforeConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(BeforeConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Autowired
    private SkTokenService skTokenService;

    // @Resource
    // public RocketMQTemplate rocket
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private ConfirmOrderService confirmOrderService;

    @SentinelResource(value = "beforeDoConfirm", blockHandler = "beforeDoConfirmBlock")
    public Long beforeDoConfirm(ConfirmOrderDoReq req) {
        Long id = null;
        List<ConfirmOrder> createdOrderList = new ArrayList<>();
        // 根据前端传值，加入排队人数
        int lineNumber = Math.max(0, Math.min(req.getLineNumber(), 20));
        for (int i = 0; i < lineNumber + 1; i++) {
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
            createdOrderList.add(confirmOrder);

            id = confirmOrder.getId();
        }

        // 只需要发送一条MQ消息，消费者会按日期和车次批量处理所有INIT订单。
        ConfirmOrderMQDto confirmOrderMQDto = new ConfirmOrderMQDto();
        confirmOrderMQDto.setDate(req.getDate());
        confirmOrderMQDto.setTrainCode(req.getTrainCode());
        confirmOrderMQDto.setLogId(MDC.get("LOG_ID"));
        String reqJson = JSON.toJSONString(confirmOrderMQDto);

        try {
            LOG.info("排队购票，发送mq开始，消息：{}", reqJson);
            rocketMQTemplate.syncSend(RocketMQTopicEnum.CONFIRM_ORDER.getCode(), reqJson, 10000);
            LOG.info("排队购票，发送mq结束");
        } catch (Exception e) {
            LOG.error("排队购票，发送mq失败，标记本次创建的订单为失败：{}", createdOrderList, e);
            createdOrderList.forEach(confirmOrder -> {
                confirmOrder.setStatus(ConfirmOrderStatusEnum.FAILURE.getCode());
                confirmOrder.setUpdateTime(new Date());
                confirmOrderMapper.updateByPrimaryKeySelective(confirmOrder);
            });
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_EXCEPTION);
        }

//        confirmOrderService.doConfirm(confirmOrderMQDto);
        return id;
    }

    /**
     * 降级方法，需包含限流方法的所有参数和BlockException参数
     * @param req
     * @param e
     */
    public void beforeDoConfirmBlock(ConfirmOrderDoReq req, BlockException e) {
        LOG.info("购票请求被限流：{}", req);
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }
}
