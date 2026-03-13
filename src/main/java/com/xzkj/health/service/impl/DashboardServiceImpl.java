package com.xzkj.health.service.impl;

import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.DashboardMapper;
import com.xzkj.health.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Dashboard服务实现
 *
 * 规则：startTime/endTime 为 null 时，自动填入当月月初/月末。
 */
@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DashboardMapper dashboardMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 当月月初，格式 yyyy-MM-dd */
    private String monthStart() {
        return LocalDate.now().withDayOfMonth(1).format(DATE_FMT);
    }

    /** 当月月末，格式 yyyy-MM-dd */
    private String monthEnd() {
        LocalDate d = LocalDate.now();
        return d.withDayOfMonth(d.lengthOfMonth()).format(DATE_FMT);
    }

    private String resolve(String val, String fallback) {
        return (val != null && !val.isBlank()) ? val : fallback;
    }

    @Override
    public Map<String, Object> getCurrentMonthCounts(String startTime, String endTime) {
        String s = resolve(startTime, monthStart());
        String e = resolve(endTime,   monthEnd());
        Map<String, Object> data = dashboardMapper.getCountsByRange(s, e);

        Map<String, Object> result = new HashMap<>();
        MapValueUtil.copyIntFields(data, result,
                "heartRate", "bloodOxygen", "sleep", "steps", "temperature", "pressure");
        return result;
    }

    @Override
    public Map<String, Object> getCurrentMonthAverage(String startTime, String endTime) {
        String s = resolve(startTime, monthStart());
        String e = resolve(endTime,   monthEnd());
        Map<String, Object> data = dashboardMapper.getAverageByRange(s, e);

        Map<String, Object> result = new HashMap<>();
        MapValueUtil.copyIntFields(data, result,
                "avgPressure", "avgBloodOxygen", "avgHeartRate", "avgSteps",
                "avgBloodPressureHigh", "avgBloodPressureLow", "avgCalories");
        result.put("avgSleep",       round(MapValueUtil.getDouble(data, "avgSleep"), 1));
        result.put("avgTemperature", round(MapValueUtil.getDouble(data, "avgTemperature"), 1));
        return result;
    }

    @Override
    public List<Map<String, Object>> getDeptTop5(String startTime, String endTime) {
        String s = resolve(startTime, monthStart());
        String e = resolve(endTime,   monthEnd());
        List<Map<String, Object>> list = dashboardMapper.getTop5ByRange(s, e);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : list) {
            Map<String, Object> row = new HashMap<>();
            row.put("userName", item.get("userName"));
            row.put("userCode", item.get("userCode"));
            row.put("count",    MapValueUtil.getInt(item, "count"));
            result.add(row);
        }
        return result;
    }

    @Override
    public Map<String, Object> getDeviceStats(String startTime, String endTime) {
        String s = resolve(startTime, monthStart());
        String e = resolve(endTime,   monthEnd());
        Map<String, Object> data = dashboardMapper.getDeviceStatsByRange(s, e);

        Map<String, Object> result = new HashMap<>();
        MapValueUtil.copyIntFields(data, result,
                "total", "boundDevices", "activeRate", "usageRate", "warningRate", "lowBattery");
        return result;
    }

    @Override
    public Map<String, Object> getWarningRates(String startTime, String endTime) {
        String s = resolve(startTime, monthStart());
        String e = resolve(endTime,   monthEnd());
        List<Map<String, Object>> rates = dashboardMapper.getWarningRatesByRange(s, e);

        List<Map<String, Object>> warningList = new ArrayList<>();
        for (Map<String, Object> rate : rates) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", rate.get("name"));
            item.put("rate", MapValueUtil.getInt(rate, "rate"));
            item.put("icon", rate.get("icon"));
            warningList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("warningRates", warningList);
        return result;
    }

    @Override
    public List<Map<String, Object>> getRecentWarnings(int limit, String startTime, String endTime) {
        String s = resolve(startTime, monthStart());
        String e = resolve(endTime,   monthEnd());
        List<Map<String, Object>> list = dashboardMapper.getWarningsByRange(limit, s, e);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : list) {
            Map<String, Object> warning = new HashMap<>();
            warning.put("type",     item.get("warning_type"));
            warning.put("userName", item.get("real_name"));
            warning.put("empCode",  item.get("emp_code"));  // 员工编码，用于前端去重统计异常人员数
            warning.put("indicator",item.get("indicator_name"));
            warning.put("value",    item.get("indicator_value"));
            warning.put("time",     item.get("create_time"));
            warning.put("level",    item.get("warning_level"));
            warning.put("handled",  item.get("is_handled"));
            result.add(warning);
        }
        return result;
    }

    @Override
    public Map<String, Object> getWarningDistribution(String startTime, String endTime, String groupBy) {
        String s = resolve(startTime, monthStart());
        String e = resolve(endTime,   monthEnd());
        Map<String, Object> result = new HashMap<>();
        if ("hour".equals(groupBy)) {
            List<Map<String, Object>> rows = dashboardMapper.getWarningCountsByHour(s);
            int[] counts = new int[24];
            for (Map<String, Object> row : rows) {
                int h = MapValueUtil.getInt(row, "hour_num");
                if (h >= 0 && h < 24) counts[h] = MapValueUtil.getInt(row, "cnt");
            }
            result.put("labels", java.util.stream.IntStream.range(0, 24).mapToObj(String::valueOf).collect(java.util.stream.Collectors.toList()));
            result.put("counts", java.util.Arrays.stream(counts).boxed().collect(java.util.stream.Collectors.toList()));
        } else {
            List<Map<String, Object>> rows = dashboardMapper.getWarningCountsByDate(s, e);
            List<String> labels = new java.util.ArrayList<>();
            List<Integer> counts = new java.util.ArrayList<>();
            for (Map<String, Object> row : rows) {
                labels.add(String.valueOf(row.get("stat_date")));
                counts.add(MapValueUtil.getInt(row, "cnt"));
            }
            result.put("labels", labels);
            result.put("counts", counts);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getDailyAnomalyRates(int days) {
        String startDate = LocalDate.now().minusDays(days - 1).format(DATE_FMT);
        String endDate   = LocalDate.now().format(DATE_FMT);
        return dashboardMapper.getDailyAnomalyRates(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getDeptHealthCounts(String startTime, String endTime) {
        String s = resolve(startTime, monthStart());
        String e = resolve(endTime,   monthEnd());
        int days = 30;
        try {
            long diff = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.parse(s), java.time.LocalDate.parse(e)) + 1;
            days = (int) diff;
        } catch (Exception ex) {
            log.debug("日期范围解析失败，使用默认30天: {}", ex.getMessage());
        }
        List<Map<String, Object>> list = dashboardMapper.getDeptWarningWithTrend(s, e, days);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : list) {
            Map<String, Object> row = new HashMap<>();
            row.put("name",      item.get("name"));
            row.put("count",     MapValueUtil.getInt(item, "count"));
            row.put("prevCount", MapValueUtil.getInt(item, "prevCount"));
            result.add(row);
        }
        return result;
    }

    @Override
    public Map<String, Object> getDailyHealthTrend(int days) {
        String startDate = LocalDate.now().minusDays(days - 1).format(DATE_FMT);
        String endDate   = LocalDate.now().format(DATE_FMT);
        List<Map<String, Object>> rows = dashboardMapper.getDailyHealthTrend(startDate, endDate);

        // 按日期建索引
        Map<String, Map<String, Object>> byDate = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String d = String.valueOf(row.get("date"));
            byDate.put(d, row);
        }

        // 生成完整日期序列（避免缺天）
        DateTimeFormatter shortFmt = DateTimeFormatter.ofPattern("MM-dd");
        List<String> dates        = new ArrayList<>();
        List<Object> heartRates   = new ArrayList<>();
        List<Object> bloodOxygens = new ArrayList<>();
        List<Object> stepsList    = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate d   = LocalDate.now().minusDays(i);
            String fullKey = d.format(DATE_FMT);
            dates.add(d.format(shortFmt));
            Map<String, Object> row = byDate.get(fullKey);
            if (row != null) {
                heartRates.add(MapValueUtil.getInt(row, "avgHeartRate"));
                bloodOxygens.add(MapValueUtil.getInt(row, "avgBloodOxygen"));
                stepsList.add(MapValueUtil.getInt(row, "avgSteps"));
            } else {
                heartRates.add(null);
                bloodOxygens.add(null);
                stepsList.add(null);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates",       dates);
        result.put("heartRate",   heartRates);
        result.put("bloodOxygen", bloodOxygens);
        result.put("steps",       stepsList);
        return result;
    }

    @Override
    public List<Map<String, Object>> getDeptRanking(String startTime, String endTime) {
        String s = resolve(startTime, monthStart());
        String e = resolve(endTime,   monthEnd());
        List<Map<String, Object>> rows = dashboardMapper.getDeptRanking(s, e);

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("rank",        i + 1);
            item.put("department",  row.get("department"));
            item.put("memberCount", MapValueUtil.getInt(row, "memberCount"));
            item.put("healthScore", MapValueUtil.getInt(row, "healthScore"));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getDailyHealthTrendByRange(String startDate, String endDate) {
        return dashboardMapper.getDailyHealthTrend(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getWarningCountsByDate(String startDate, String endDate) {
        return dashboardMapper.getWarningCountsByDate(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getDayHeartRateRank(String date) {
        return dashboardMapper.getDayHeartRateRank(date);
    }

    @Override
    public List<Map<String, Object>> getDayBloodOxygenRank(String date) {
        return dashboardMapper.getDayBloodOxygenRank(date);
    }

    @Override
    public List<Map<String, Object>> getDayStepsRank(String date) {
        return dashboardMapper.getDayStepsRank(date);
    }

    @Override
    public List<Map<String, Object>> getDayWarnings(String date) {
        return dashboardMapper.getDayWarnings(date);
    }

    // ─── 工具方法 ───────────────────────────────────────────────────

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}
