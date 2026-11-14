package com.yali.business.mapper.cust;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PurchaseRequestMapper {
    @Insert("insert into purchase_request(member_id,request_id,order_id,create_time) values(#{memberId},#{requestId},#{orderId},NOW(3))")
    int insert(@Param("memberId") Long memberId, @Param("requestId") String requestId, @Param("orderId") Long orderId);

    @Select("select order_id from purchase_request where member_id=#{memberId} and request_id=#{requestId}")
    Long findOrderId(@Param("memberId") Long memberId, @Param("requestId") String requestId);

}
