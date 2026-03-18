package com.xzkj.health.service.impl;

import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.HeartRateMapper;
import com.xzkj.health.service.HeartRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 心率监测服务实现
 * 异常由 GlobalExceptionHandler 统一处理，Service 层无需 try-catch-rethrow
 */
@Service
public class HeartRateServiceImpl implements HeartRateService {

    @Autowired
    private HeartRateMapper heartRateMapper;

    @Override
    public Map<String, Object> getHeartRateOverview(String startDate, String endDate) {
        // 同月或跨月均路由到 Direct 版本，避免 v_health_record UNION ALL 13 表全扫
        // 跨月时用 UNION ALL 子查询（仅 2 张分区表），Druid wall 可接受
        String tableSource = heartRateTableSourceByRange(startDate, endDate);
        return heartRateMapper.getHeartRateOverviewDirect(tableSource, startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getTopUsers(int limit, String startDate, String endDate) {
        return heartRateMapper.getTopUsers(limit, startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getAgeDistribution(String startDate, String endDate) {
        return heartRateMapper.getAgeDistribution(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getHeartRateDistribution(String startDate, String endDate) {
        return heartRateMapper.getHeartRateDistributionNew(startDate, endDate);
    }

    /** 解析日期范围所跨的分区表表达式（仅取心率趋势所需列） */
    private static String heartRateTableSource(int days) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMM");
        String curMonth  = LocalDate.now().format(fmt);
        String prevMonth = LocalDate.now().minusDays(days).format(fmt);
        if (curMonth.equals(prevMonth)) {
            return "health_record_" + curMonth;
        }
        String cols = "user_code,heart_rate,record_time";
        // 必须用 AS 关键字，否则 Druid SQL 防火墙（SQL Server 模式）拒绝子查询别名
        return "(SELECT " + cols + " FROM health_record_" + prevMonth +
               " UNION ALL SELECT " + cols + " FROM health_record_" + curMonth + ") AS _hr";
    }

    /** 根据显式日期范围计算分区表表达式（带 AS _hr 别名，用于 CTE 内 FROM ${tableSource}） */
    private static String heartRateTableSourceByRange(String startDate, String endDate) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMM");
        String m1 = LocalDate.parse(startDate).format(fmt);
        String m2 = LocalDate.parse(endDate).format(fmt);
        if (m1.equals(m2)) {
            return "health_record_" + m1;
        }
        String cols = "user_code,heart_rate,blood_oxygen,temperature,steps,calories," +
                      "sleep_minutes,blood_pressure_high,blood_pressure_low,pressure,record_time";
        // 必须用 AS 关键字，否则 Druid SQL 防火墙（SQL Server 模式）拒绝子查询别名
        return "(SELECT " + cols + " FROM health_record_" + m1 +
               " UNION ALL SELECT " + cols + " FROM health_record_" + m2 + ") AS _hr";
    }

    /** 根据显式日期范围计算分区表表达式（无别名，用于 mapper 中 FROM ${tableSource} hr 带独立别名的场景） */
    private static String heartRateTableSourceForJoin(String startDate, String endDate) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMM");
        String m1 = LocalDate.parse(startDate).format(fmt);
        String m2 = LocalDate.parse(endDate).format(fmt);
        if (m1.equals(m2)) {
            return "health_record_" + m1;
        }
        String cols = "user_code,heart_rate,blood_oxygen,temperature,steps,calories," +
                      "sleep_minutes,blood_pressure_high,blood_pressure_low,pressure,record_time";
        // 无内嵌别名，mapper SQL 中的 "hr" 或 "AS hr" 会作为唯一别名
        return "(SELECT " + cols + " FROM health_record_" + m1 +
               " UNION ALL SELECT " + cols + " FROM health_record_" + m2 + ")";
    }

    @Override
    public Map<String, Object> getHeartRateTrend(int days) {
        String tblSrc = heartRateTableSource(days);
        return MapValueUtil.convertTrendData(heartRateMapper.getHeartRateTrendDirect(tblSrc, days), "avgHeartRate");
    }

    @Override
    public List<Map<String, Object>> getDepartmentStats(String startDate, String endDate) {
        // 使用 ForJoin 版本（无内嵌别名），因为 mapper SQL 末尾会追加 "hr" 作为别名
        String tblSrc = heartRateTableSourceForJoin(startDate, endDate);
        return heartRateMapper.getDepartmentStatsDirect(tblSrc, startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getHourlyStats(String startDate, String endDate) {
        return MapValueUtil.orEmpty(heartRateMapper.getHourlyStats(startDate, endDate));
    }

    @Override
    public List<Map<String, Object>> getRealtime(int limit) {
        // 近2h始终在当月分区表内
        String curMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return MapValueUtil.orEmpty(heartRateMapper.getRealtimeDirect("health_record_" + curMonth, limit));
    }

    @Override
    public List<Map<String, Object>> getDailyAnomalyCount(String startDate, String endDate) {
        return MapValueUtil.orEmpty(heartRateMapper.getDailyAnomalyCount(startDate, endDate));
    }
}
