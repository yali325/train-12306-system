package com.yali.business.mapper.cust;

import java.util.Date;
import org.apache.ibatis.annotations.Param;

public interface DailyTrainTicketMapperCust {

    int updateCountBySell(@Param("date") Date date, @Param("trainCode") String trainCode,
            @Param("seatTypeCode") String seatTypeCode,
            @Param("minStartIndex") Integer minStartIndex,
            @Param("maxStartIndex") Integer maxStartIndex,
            @Param("minEndIndex") Integer minEndIndex,
            @Param("maxEndIndex") Integer maxEndIndex);

    int countAffected(@Param("date") Date date, @Param("trainCode") String trainCode,
            @Param("minStartIndex") Integer minStartIndex,
            @Param("maxStartIndex") Integer maxStartIndex,
            @Param("minEndIndex") Integer minEndIndex,
            @Param("maxEndIndex") Integer maxEndIndex);
}
