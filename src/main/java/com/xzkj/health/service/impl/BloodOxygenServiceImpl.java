package com.xzkj.health.service.impl;

import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.BloodOxygenMapper;
import com.xzkj.health.service.BloodOxygenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 血氧监测服务实现
 * 异常由 GlobalExceptionHandler 统一处理
 */
@Service
public class BloodOxygenServiceImpl implements BloodOxygenService {

    @Autowired
    private BloodOxygenMapper bloodOxygenMapper;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    /** 根据日期范围路由分区表（单月=表名，跨月=UNION ALL裸子查询不含别名）
     * 不加 AS 末尾别名 — Mapper SQL 中 FROM ${tableSource} AS _bo 自行提供别名
     */
    private String healthSource(String start, String end) {
        String m1 = LocalDate.parse(start).format(MONTH_FMT);
        String m2 = LocalDate.parse(end).format(MONTH_FMT);
        if (m1.equals(m2)) return "health_record_" + m1;
        return "(SELECT user_code,record_time,blood_oxygen FROM health_record_" + m1 +
               " UNION ALL SELECT user_code,record_time,blood_oxygen FROM health_record_" + m2 + ")";
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
    public Map<String, Object> getBloodOxygenStats(String startDate, String endDate) {
        // 优化（Round 15）：路由到分区表，避免 v_health_record UNION ALL 全扫描（6s→预期<500ms）
        String tblSrc = healthSource(startDate, endDate);
        Map<String, Object> data = bloodOxygenMapper.getBloodOxygenStatsDirect(tblSrc, startDate, endDate);
        Map<String, Object> result = new HashMap<>();
        MapValueUtil.copyIntFields(data, result,
                "avgBloodOxygen", "maxBloodOxygen", "minBloodOxygen",
                "normalCount", "abnormalCount", "totalCount", "detectionRate");
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
    public List<Map<String, Object>> getAgeDistribution(String startDate, String endDate) {
        return MapValueUtil.orEmpty(bloodOxygenMapper.getAgeDistribution(startDate, endDate));
    }

    @Override
    public List<Map<String, Object>> getHourlyStats(String startDate, String endDate) {
        return MapValueUtil.orEmpty(bloodOxygenMapper.getHourlyStats(startDate, endDate));
    }

    @Override
    public List<Map<String, Object>> getRealtime(int limit) {
        return MapValueUtil.orEmpty(bloodOxygenMapper.getRealtime(limit));
    }
}