package com.yali.common.util;

import cn.hutool.core.util.IdUtil;

/**
 * 封装hutool雪花算法
 */
public class SnowUtil {

    //可以到数据库申请id
    private static long dataCenterId = 1;  //数据中心
    private static long workerId = 1;     //机器标识

    public static long getSnowflakeNextId() {
        return IdUtil.getSnowflake(workerId, dataCenterId).nextId();
    }

    public static String getSnowflakeNextIdStr() {
        return IdUtil.getSnowflake(workerId, dataCenterId).nextIdStr();
    }
}
