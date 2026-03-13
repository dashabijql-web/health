package com.xzkj.health.service.impl;

import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.BloodOxygenMapper;
import com.xzkj.health.service.BloodOxygenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 血氧监测服务实现
 * 异常由 GlobalExceptionHandler 统一处理
 */
@Service
public class BloodOxygenServiceImpl implements BloodOxygenService {

    @Autowired
    private BloodOxygenMapper bloodOxygenMapper;

    @Override
    public Map<String, Object> getBloodOxygenStats(String startDate, String endDate) {
        Map<String, Object> data = bloodOxygenMapper.getBloodOxygenStats(startDate, endDate);
        Map<String, Object> result = new HashMap<>();
        MapValueUtil.copyIntFields(data, result,
                "avgBloodOxygen", "maxBloodOxygen", "minBloodOxygen",
                "normalCount", "abnormalCount", "totalCount", "normalRate");
        return result;
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

        return result;
    }

    @Override
    public List<Map<String, Object>> getTopUsers(Integer limit, String startDate, String endDate) {
        return MapValueUtil.orEmpty(bloodOxygenMapper.getTopUsers(limit, startDate, endDate));
    }

    @Override
    public List<Map<String, Object>> getDepartmentStats(String startDate, String endDate) {
        return MapValueUtil.orEmpty(bloodOxygenMapper.getDepartmentStats(startDate, endDate));
    }

    @Override
    public List<Map<String, Object>> getAgeDistribution() {
        return MapValueUtil.orEmpty(bloodOxygenMapper.getAgeDistribution());
    }

    @Override
    public List<Map<String, Object>> getHourlyStats(String startDate, String endDate) {
        return MapValueUtil.orEmpty(bloodOxygenMapper.getHourlyStats(startDate, endDate));
    }
}