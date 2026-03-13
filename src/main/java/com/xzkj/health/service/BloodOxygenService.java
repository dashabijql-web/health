package com.xzkj.health.service;

import java.util.List;
import java.util.Map;

/**
 * 血氧监测服务接口
 */
public interface BloodOxygenService {

    /**
     * 获取血氧统计概览
     */
    Map<String, Object> getBloodOxygenStats(String startDate, String endDate);

    /**
     * 获取血氧趋势数据
     * @param days 天数
     */
    Map<String, Object> getBloodOxygenTrend(Integer days);

    /**
     * 获取血氧分布数据
     */
    List<Map<String, Object>> getBloodOxygenDistribution(String startDate, String endDate);

    /**
     * 获取异常血氧记录
     * @param page 页码
     * @param size 每页条数
     */
    Map<String, Object> getAbnormalRecords(Integer page, Integer size);

    /**
     * 获取TOP异常人员统计
     * @param limit 返回数量
     */
    List<Map<String, Object>> getTopUsers(Integer limit, String startDate, String endDate);

    /**
     * 获取部门血氧统计
     */
    List<Map<String, Object>> getDepartmentStats(String startDate, String endDate);

    /**
     * 获取年龄段血氧分布
     */
    List<Map<String, Object>> getAgeDistribution();

    /**
     * 获取实时血氧数据
     * @param limit 返回数量
     */
    List<Map<String, Object>> getRealtimeData(Integer limit);

    /**
     * 获取指定日期每小时平均血氧
     * @param date 日期字符串 YYYY-MM-DD
     * @return List<{hour, avgBloodOxygen}>
     */
    List<Map<String, Object>> getHourlyStats(String startDate, String endDate);
}