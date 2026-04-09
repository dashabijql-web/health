package com.xzkj.health.service;

import java.util.List;
import java.util.Map;

/**
 * 心率监测服务接口
 */
public interface HeartRateService {

    Map<String, Object> getHeartRateOverview(String startDate, String endDate);

    List<Map<String, Object>> getTopUsers(int limit, String startDate, String endDate);

    List<Map<String, Object>> getAgeDistribution(String startDate, String endDate);

    List<Map<String, Object>> getHeartRateDistribution(String startDate, String endDate);

    Map<String, Object> getHeartRateTrend(int days);

    List<Map<String, Object>> getDepartmentStats(String startDate, String endDate);

    List<Map<String, Object>> getHourlyStats(String startDate, String endDate);

    List<Map<String, Object>> getRealtime(int limit);

    List<Map<String, Object>> getDailyAnomalyCount(String startDate, String endDate);
}
