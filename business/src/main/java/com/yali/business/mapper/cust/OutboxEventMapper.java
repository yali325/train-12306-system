package com.yali.business.mapper.cust;

import com.yali.business.domain.OutboxEvent;
import org.apache.ibatis.annotations.Param;

public interface OutboxEventMapper {

    int insert(OutboxEvent event);

    OutboxEvent selectReadyForUpdate();

    int markSending(@Param("id") Long id,
                    @Param("claimToken") String claimToken,
                    @Param("leaseSeconds") int leaseSeconds);

    int markSent(@Param("id") Long id, @Param("claimToken") String claimToken);

    int markRetry(@Param("id") Long id,
                  @Param("claimToken") String claimToken,
                  @Param("delaySeconds") long delaySeconds,
                  @Param("lastError") String lastError);
}
