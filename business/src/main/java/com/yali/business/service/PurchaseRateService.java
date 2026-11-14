package com.yali.business.service;

import cn.hutool.core.date.DateUtil;
import com.yali.common.exception.BusinessException;
import com.yali.common.exception.BusinessExceptionEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.Date;

@Service
public class PurchaseRateService {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> script;
    private final int ratePerSecond;
    private final int burstCapacity;

    public PurchaseRateService(StringRedisTemplate redis,
            @Value("${purchase.admission.rate-per-second}") int ratePerSecond,
            @Value("${purchase.admission.burst-capacity}") int burstCapacity) {
        if (ratePerSecond < 1 || burstCapacity < 1) throw new IllegalArgumentException("购票令牌桶参数必须大于 0");
        this.redis = redis;
        this.ratePerSecond = ratePerSecond;
        this.burstCapacity = burstCapacity;
        this.script = new DefaultRedisScript<>();
        this.script.setLocation(new ClassPathResource("lua/purchase_rate.lua"));
        this.script.setResultType(Long.class);
    }

    public void acquire(Date date, String trainCode) {
        String key = "PURCHASE_RATE:" + DateUtil.formatDate(date) + ":" + trainCode;
        Long allowed = redis.execute(script, Collections.singletonList(key),
                String.valueOf(ratePerSecond), String.valueOf(burstCapacity));
        if (!Long.valueOf(1L).equals(allowed)) {
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_RATE_LIMITED);
        }
    }
}
