package com.yali.business.mapper.cust;

import org.apache.ibatis.annotations.Param;

public interface DailyTrainSeatMapperCust {
    int updateSellIfUnchanged(@Param("id") Long id,
                              @Param("oldSell") String oldSell,
                              @Param("newSell") String newSell);
}
