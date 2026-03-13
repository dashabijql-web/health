package com.xzkj.health.service;

import java.util.List;
import java.util.Map;

public interface PressureService {
    Map<String, Object> getOverview(String startDate, String endDate);
    Map<String, Object> getTrend(int days);
    List<Map<String, Object>> getDistribution(String startDate, String endDate);
    List<Map<String, Object>> getTopUsers(int limit, String startDate, String endDate);
    List<Map<String, Object>> getDepartmentStats(String startDate, String endDate);
    Map<String, Object> getAbnormalRecords(int page, int size);
    List<Map<String, Object>> getRealtime(int limit);
    List<Map<String, Object>> getHourlyStats(String date);
}
