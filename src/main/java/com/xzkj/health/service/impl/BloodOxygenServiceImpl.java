package com.xzkj.health.service.impl;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.BloodOxygenMapper;
import com.xzkj.health.service.BloodOxygenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 血氧监测服务实现
 */
@Slf4j
@Service
public class BloodOxygenServiceImpl implements BloodOxygenService {

    @Autowired
    private BloodOxygenMapper bloodOxygenMapper;

    @Override
    public Map<String, Object> getBloodOxygenStats(String startDate, String endDate) {
        Map<String, Object> stats = bloodOxygenMapper.getBloodOxygenStats(startDate, endDate);

        // 处理null值，设置默认值
        if (stats == null) {
            stats = new HashMap<>();
        }
        stats.putIfAbsent("avgBloodOxygen", 0);
        stats.putIfAbsent("maxBloodOxygen", 0);
        stats.putIfAbsent("minBloodOxygen", 0);
        stats.putIfAbsent("normalCount", 0);
        stats.putIfAbsent("abnormalCount", 0);
        stats.putIfAbsent("totalCount", 0);
        stats.putIfAbsent("normalRate", 0);

        log.info("获取血氧统计概览: {}", stats);
        return stats;
    }

    @Override
    public Map<String, Object> getBloodOxygenTrend(Integer days) {
        List<Map<String, Object>> rows = bloodOxygenMapper.getBloodOxygenTrend(days);
        return MapValueUtil.convertTrendData(rows, "avgBloodOxygen");
    }

    @Override
    public List<Map<String, Object>> getBloodOxygenDistribution(String startDate, String endDate) {
        List<Map<String, Object>> rawData = bloodOxygenMapper.getBloodOxygenDistribution(startDate, endDate);

        // 计算总数
        int total = 0;
        int lowCount = 0;
        int normalCount = 0;
        int highCount = 0;

        if (rawData != null && !rawData.isEmpty()) {
            for (Map<String, Object> item : rawData) {
                String range = (String) item.get("range");
                int count = ((Number) item.get("count")).intValue();
                total += count;

                if ("<90".equals(range)) {
                    lowCount += count;
                } else if ("≥99".equals(range)) {
                    highCount += count;
                } else {
                    normalCount += count;
                }
            }
        }

        // 转换为前端需要的格式
        List<Map<String, Object>> result = new ArrayList<>();

        if (total > 0) {
            // 血氧偏低 (<90%)
            if (lowCount > 0) {
                Map<String, Object> low = new HashMap<>();
                low.put("name", "血氧偏低");
                low.put("value", Math.round(lowCount * 100.0 / total));
                low.put("color", "#4FC3F7");
                result.add(low);
            }

            // 血氧正常 (90~100%): normalCount(90-99) + highCount(≥99，实为正常优秀)
            int allNormal = normalCount + highCount;
            if (allNormal > 0) {
                Map<String, Object> normal = new HashMap<>();
                normal.put("name", "血氧正常");
                normal.put("value", Math.round(allNormal * 100.0 / total));
                normal.put("color", "#38ef7d");
                result.add(normal);
            }
        }

        log.info("获取血氧分布数据，总数: {}, 偏低: {}, 正常: {}, 偏高: {}", total, lowCount, normalCount, highCount);
        return result;
    }

    @Override
    public Map<String, Object> getAbnormalRecords(Integer page, Integer size) {
        int offset = (page - 1) * size;

        List<Map<String, Object>> records = bloodOxygenMapper.getAbnormalRecords(offset, size);
        Integer total = bloodOxygenMapper.getAbnormalCount();
        return MapValueUtil.buildPageResult(records, total != null ? total : 0, page, size);
    }

    @Override
    public List<Map<String, Object>> getTopUsers(Integer limit, String startDate, String endDate) {
        try {
            List<Map<String, Object>> topUsers = bloodOxygenMapper.getTopUsers(limit, startDate, endDate);
            log.info("获取TOP{}血氧异常人员成功，数量: {}", limit, topUsers != null ? topUsers.size() : 0);
            return topUsers != null ? topUsers : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取TOP用户失败", e);
            throw new RuntimeException("获取TOP用户失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getDepartmentStats(String startDate, String endDate) {
        try {
            List<Map<String, Object>> deptStats = bloodOxygenMapper.getDepartmentStats(startDate, endDate);
            log.info("获取部门统计成功，部门数: {}", deptStats != null ? deptStats.size() : 0);
            return deptStats != null ? deptStats : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取部门统计失败", e);
            throw new RuntimeException("获取部门统计失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getAgeDistribution() {
        try {
            List<Map<String, Object>> ageData = bloodOxygenMapper.getAgeDistribution();
            log.info("获取年龄段分布成功，分组数: {}", ageData != null ? ageData.size() : 0);
            return ageData != null ? ageData : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取年龄段分布失败", e);
            throw new RuntimeException("获取年龄段分布失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getRealtimeData(Integer limit) {
        try {
            List<Map<String, Object>> realtimeData = bloodOxygenMapper.getRealtimeData(limit);
            log.info("获取实时血氧数据成功，数量: {}", realtimeData != null ? realtimeData.size() : 0);
            return realtimeData != null ? realtimeData : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取实时数据失败", e);
            throw new RuntimeException("获取实时数据失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getHourlyStats(String startDate, String endDate) {
        try {
            List<Map<String, Object>> data = bloodOxygenMapper.getHourlyStats(startDate, endDate);
            log.info("获取血氧逐小时数据成功，{}~{}, count={}", startDate, endDate, data != null ? data.size() : 0);
            return data != null ? data : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取血氧逐小时数据失败", e);
            throw new RuntimeException("获取血氧逐小时数据失败: " + e.getMessage());
        }
    }
}