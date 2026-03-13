package com.xzkj.health.service.impl;

import com.xzkj.health.mapper.HeartRateMapper;
import com.xzkj.health.service.HeartRateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 心率监测服务实现 - 增强版
 */
@Slf4j
@Service
public class HeartRateServiceImpl implements HeartRateService {

    @Autowired
    private HeartRateMapper heartRateMapper;

    @Override
    public Map<String, Object> getHeartRateOverview(String startDate, String endDate) {
        try {
            Map<String, Object> stats = heartRateMapper.getHeartRateOverview(startDate, endDate);
            log.info("获取心率概览成功: {}", stats);
            return stats;
        } catch (Exception e) {
            log.error("获取心率概览失败", e);
            throw new RuntimeException("获取心率概览失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getTopUsers(int limit, String startDate, String endDate) {
        try {
            List<Map<String, Object>> topUsers = heartRateMapper.getTopUsers(limit, startDate, endDate);
            log.info("获取TOP{}用户成功，数量: {}", limit, topUsers.size());
            return topUsers;
        } catch (Exception e) {
            log.error("获取TOP用户失败", e);
            throw new RuntimeException("获取TOP用户失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getAgeDistribution() {
        try {
            List<Map<String, Object>> distribution = heartRateMapper.getAgeDistribution();
            log.info("获取年龄段统计成功，分组数: {}", distribution.size());
            return distribution;
        } catch (Exception e) {
            log.error("获取年龄段统计失败", e);
            throw new RuntimeException("获取年龄段统计失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getHeartRateDistribution(String startDate, String endDate) {
        try {
            List<Map<String, Object>> distribution = heartRateMapper.getHeartRateDistributionNew(startDate, endDate);
            log.info("获取心率分布成功，分类数: {}", distribution.size());
            return distribution;
        } catch (Exception e) {
            log.error("获取心率分布失败", e);
            throw new RuntimeException("获取心率分布失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getHeartRateTrend(int days) {
        try {
            List<Map<String, Object>> trendList = heartRateMapper.getHeartRateTrend(days);

            // 转换数据格式为前端需要的格式
            List<String> dates = new ArrayList<>();
            List<Integer> values = new ArrayList<>();

            for (Map<String, Object> item : trendList) {
                String date = (String) item.get("date");
                if (date != null && date.length() >= 10) {
                    // 转换格式: 2023-08-13 -> 8.13
                    String[] parts = date.split("-");
                    if (parts.length >= 3) {
                        dates.add(Integer.parseInt(parts[1]) + "." + Integer.parseInt(parts[2]));
                    }
                } else {
                    dates.add(date);
                }

                Number avg = (Number) item.get("avgHeartRate");
                values.add(avg != null ? avg.intValue() : 0);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("dates", dates);
            result.put("values", values);

            log.info("获取心率趋势成功，天数: {}, 数据点: {}", days, dates.size());
            return result;
        } catch (Exception e) {
            log.error("获取心率趋势失败", e);
            throw new RuntimeException("获取心率趋势失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getRealtimeData(int limit) {
        try {
            List<Map<String, Object>> realtimeList = heartRateMapper.getRealtimeData(limit);
            log.info("获取实时数据成功，数量: {}", realtimeList.size());
            return realtimeList;
        } catch (Exception e) {
            log.error("获取实时数据失败", e);
            throw new RuntimeException("获取实时数据失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getDepartmentStats(String startDate, String endDate) {
        try {
            List<Map<String, Object>> deptStats = heartRateMapper.getDepartmentStats(startDate, endDate);
            log.info("获取部门统计成功，部门数: {}", deptStats.size());
            return deptStats;
        } catch (Exception e) {
            log.error("获取部门统计失败", e);
            throw new RuntimeException("获取部门统计失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getAbnormalRecords(int page, int size) {
        try {
            int offset = (page - 1) * size;

            // 获取异常记录
            List<Map<String, Object>> list = heartRateMapper.getAbnormalRecords(offset, size);

            // 获取总数
            int total = heartRateMapper.countAbnormalRecords();

            Map<String, Object> result = new HashMap<>();
            result.put("list", list);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);

            log.info("获取异常心率记录成功，页码: {}, 每页: {}, 总数: {}", page, size, total);
            return result;
        } catch (Exception e) {
            log.error("获取异常心率记录失败", e);
            throw new RuntimeException("获取异常心率记录失败: " + e.getMessage());
        }
    }

    @Override
    @Deprecated
    public Map<String, Object> getHeartRateStats() {
        return getHeartRateOverview(
            java.time.LocalDate.now().minusDays(29).toString(),
            java.time.LocalDate.now().toString());
    }

    @Override
    public List<Map<String, Object>> getHourlyStats(String startDate, String endDate) {
        try {
            List<Map<String, Object>> data = heartRateMapper.getHourlyStats(startDate, endDate);
            log.info("获取心率逐小时数据成功，{}~{}, count={}", startDate, endDate, data != null ? data.size() : 0);
            return data != null ? data : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取心率逐小时数据失败", e);
            throw new RuntimeException("获取心率逐小时数据失败: " + e.getMessage());
        }
    }
}