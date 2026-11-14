package com.yali.business.service;

import com.yali.business.domain.ConfirmOrder;
import com.yali.business.domain.ConfirmOrderExample;
import com.yali.business.enums.ConfirmOrderStatusEnum;
import com.yali.business.mapper.ConfirmOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;

@Service
public class OrderTerminalService {
    @Resource private ConfirmOrderMapper orderMapper;

    @Transactional
    public int markEmpty(Long orderId) { return finishInit(orderId, null, ConfirmOrderStatusEnum.EMPTY); }

    @Transactional
    public int cancel(Long orderId, Long memberId) { return finishInit(orderId, memberId, ConfirmOrderStatusEnum.CANCEL); }

    private int finishInit(Long orderId, Long memberId, ConfirmOrderStatusEnum target) {
        ConfirmOrder order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) return 0;
        if (memberId != null && !memberId.equals(order.getMemberId())) return 0;
        ConfirmOrderExample example = new ConfirmOrderExample();
        ConfirmOrderExample.Criteria criteria = example.createCriteria().andIdEqualTo(orderId)
                .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
        if (memberId != null) criteria.andMemberIdEqualTo(memberId);
        ConfirmOrder update = new ConfirmOrder();
        update.setStatus(target.getCode());
        update.setUpdateTime(new Date());
        int changed = orderMapper.updateByExampleSelective(update, example);
        return changed;
    }
}
