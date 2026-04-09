package com.xzkj.health.controller;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.Result;
import com.xzkj.health.service.DashboardService;
import com.xzkj.health.service.RealtimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private RealtimeService realtimeService;

    @Autowired
    private com.xzkj.health.mapper.DashboardMapper dashboardMapper;

    // ── 模块级缓存（body-indicators / overview 查询较慢，约 800ms-1s）──
    // key = "startTime|endTime"，value = [data, expireMs]
    private final ConcurrentHashMap<String, Object[]> bodyIndicatorsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> overviewCache        = new ConcurrentHashMap<>();
    private static final long BODY_INDICATORS_TTL = 10 * 60 * 1000L; // 10 分钟
    private static final long OVERVIEW_TTL        = 10 * 60 * 1000L; // 10 分钟

    // ── 慢查询缓存（daily-trend / warning-counts / dept-stats 约 400-1200ms）──
    private final ConcurrentHashMap<String, Object[]> dailyTrendCache          = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> warningCountsCache       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> deptStatsCache           = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> deptPersonStatsCache     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> calendarCache            = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> deptHealthCompCache      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> warningEventsCache       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> top5Cache               = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> deviceActivationCache   = new ConcurrentHashMap<>();
    private static final long DAILY_TREND_TTL           = 5 * 60 * 1000L; // 5 分钟（日趋势变化极缓慢）
    private static final long WARNING_COUNTS_TTL        = 5 * 60 * 1000L; // 5 分钟
    private static final long WARNING_EVENTS_TTL        = 30 * 1000L;     // 30 秒（预警列表需较实时）
    private static final long DEPT_STATS_TTL            = 10 * 60 * 1000L; // 10 分钟
    private static final long DEPT_PERSON_STATS_TTL     = 10 * 60 * 1000L; // 10 分钟
    private static final long CALENDAR_TTL              = 5 * 60 * 1000L; // 5 分钟（日历数据天级变化）
    private static final long DEPT_HEALTH_COMP_TTL      = 5 * 60 * 1000L; // 5 分钟
    private static final long TOP5_TTL                  = 2 * 60 * 1000L; // 2 分钟
    private static final long DEVICE_ACTIVATION_TTL    = 2 * 60 * 1000L; // 2 分钟

    // ═══════════════════════════════════════════════════════════════
    // 支持时间范围筛选的接口（startTime/endTime 不传则默认当月）
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        String cacheKey = startTime + "|" + endTime;
        Object[] cached = overviewCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hit = (Map<String, Object>) cached[0];
            return Result.ok("获取成功", hit);
        }
        Map<String, Object> data = dashboardService.getCurrentMonthCounts(startTime, endTime);
        overviewCache.put(cacheKey, new Object[]{ data, System.currentTimeMillis() + OVERVIEW_TTL });
        return Result.ok("获取成功", data);
    }

    @GetMapping("/body-indicators")
    public Result<Map<String, Object>> getBodyIndicators(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        String cacheKey = startTime + "|" + endTime;
        Object[] cached = bodyIndicatorsCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hit = (Map<String, Object>) cached[0];
            return Result.ok("获取成功", hit);
        }
        Map<String, Object> data = dashboardService.getCurrentMonthAverage(startTime, endTime);
        bodyIndicatorsCache.put(cacheKey, new Object[]{ data, System.currentTimeMillis() + BODY_INDICATORS_TTL });
        return Result.ok("获取成功", data);
    }

    @GetMapping("/top5")
    public Result<List<Map<String, Object>>> getTop5(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        String cacheKey = startTime + "|" + endTime;
        Object[] cached = top5Cache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
            return Result.ok("获取成功", hit);
        }
        List<Map<String, Object>> data = dashboardService.getDeptTop5(startTime, endTime);
        top5Cache.put(cacheKey, new Object[]{ data, System.currentTimeMillis() + TOP5_TTL });
        return Result.ok("获取成功", data);
    }

    @GetMapping("/device-activation")
    public Result<Map<String, Object>> getDeviceActivation(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        String cacheKey = startTime + "|" + endTime;
        Object[] cached = deviceActivationCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hit = (Map<String, Object>) cached[0];
            return Result.ok("获取成功", hit);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("stats",        dashboardService.getDeviceStats(startTime, endTime));
        Map<String, Object> ratesResult = dashboardService.getWarningRates(startTime, endTime);
        result.put("warningRates", ratesResult != null
                ? ratesResult.getOrDefault("warningRates", Collections.emptyList())
                : Collections.emptyList());
        deviceActivationCache.put(cacheKey, new Object[]{ result, System.currentTimeMillis() + DEVICE_ACTIVATION_TTL });
        return Result.ok("获取成功", result);
    }

    @GetMapping("/warning-events")
    public Result<List<Map<String, Object>>> getWarningEvents(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        String cacheKey = startTime + "|" + endTime;
        Object[] cached = warningEventsCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
            return Result.ok("获取成功", hit);
        }
        // 限制返回最近 200 条预警记录（按时间倒序），避免数据量过大
        List<Map<String, Object>> data = dashboardService.getRecentWarnings(200, startTime, endTime);
        warningEventsCache.put(cacheKey, new Object[]{ data, System.currentTimeMillis() + WARNING_EVENTS_TTL });
        return Result.ok("获取成功", data);
    }

    /** 各指标检测人数（DISTINCT 人数，非条数） */
    @GetMapping("/person-counts")
    public Result<Map<String, Object>> getPersonCounts(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        String[] range = DateParamUtil.range30(startTime, endTime);
        DateTimeFormatter mFmt = DateTimeFormatter.ofPattern("yyyyMM");
        String m1 = LocalDate.parse(range[0]).format(mFmt);
        String m2 = LocalDate.parse(range[1]).format(mFmt);
        String hSrc = m1.equals(m2) ? "health_record_" + m1
            : "(SELECT user_code,record_time,heart_rate,blood_oxygen,blood_pressure_high,blood_pressure_low," +
              "temperature,sleep_minutes,steps,calories,pressure FROM health_record_" + m1 +
              " UNION ALL SELECT user_code,record_time,heart_rate,blood_oxygen,blood_pressure_high,blood_pressure_low," +
              "temperature,sleep_minutes,steps,calories,pressure FROM health_record_" + m2 + ") AS hr_combined";
        return Result.ok("获取成功", dashboardMapper.getPersonCountsByRangeDirect(hSrc, range[0], range[1]));
    }

    /** 各部门检测人数 + 异常人数（部门综合看板图表用） */
    @GetMapping("/dept-person-stats")
    public Result<List<Map<String, Object>>> getDeptPersonStats(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        String cacheKey = startTime + "|" + endTime;
        Object[] cached = deptPersonStatsCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
            return Result.ok("获取成功", hit);
        }
        String[] range = DateParamUtil.range30(startTime, endTime);
        DateTimeFormatter mFmt = DateTimeFormatter.ofPattern("yyyyMM");
        String m1 = LocalDate.parse(range[0]).format(mFmt);
        String m2 = LocalDate.parse(range[1]).format(mFmt);
        String hSrc = m1.equals(m2) ? "health_record_" + m1
            : "(SELECT user_code,record_time FROM health_record_" + m1 + " UNION ALL SELECT user_code,record_time FROM health_record_" + m2 + ")";
        String wSrc = m1.equals(m2) ? "warning_record_" + m1
            : "(SELECT user_code,create_time FROM warning_record_" + m1 + " UNION ALL SELECT user_code,create_time FROM warning_record_" + m2 + ")";
        List<Map<String, Object>> data = dashboardMapper.getDeptPersonStatsDirect(hSrc, wSrc, range[0], range[1]);
        deptPersonStatsCache.put(cacheKey, new Object[]{ data, System.currentTimeMillis() + DEPT_PERSON_STATS_TTL });
        return Result.ok("获取成功", data);
    }

    /** 指标每日检测人数+异常人数（点击指标卡片弹窗用） */
    @GetMapping("/metric-daily-detail")
    public Result<List<Map<String, Object>>> getMetricDailyDetail(
            @RequestParam String metricType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        String[] range = DateParamUtil.range30(startTime, endTime);
        DateTimeFormatter mFmt = DateTimeFormatter.ofPattern("yyyyMM");
        String m1 = LocalDate.parse(range[0]).format(mFmt);
        String m2 = LocalDate.parse(range[1]).format(mFmt);
        // 路由到分区表，避免 v_health_record UNION ALL 全扫描
        String hSrc = m1.equals(m2) ? "health_record_" + m1
            : "(SELECT user_code,record_time,heart_rate,blood_oxygen,steps,temperature,pressure FROM health_record_" + m1 +
              " UNION ALL SELECT user_code,record_time,heart_rate,blood_oxygen,steps,temperature,pressure FROM health_record_" + m2 + ") AS hr_metric";
        return Result.ok("获取成功", dashboardMapper.getMetricDailyDetail(hSrc, metricType, range[0], range[1]));
    }

    /** 单部门每日检测人数+异常人数（点击看板弹窗用） */
    @GetMapping("/dept-daily-detail")
    public Result<List<Map<String, Object>>> getDeptDailyDetail(
            @RequestParam String deptName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        String[] range = DateParamUtil.range30(startTime, endTime);
        DateTimeFormatter mFmt = DateTimeFormatter.ofPattern("yyyyMM");
        String m1 = LocalDate.parse(range[0]).format(mFmt);
        String m2 = LocalDate.parse(range[1]).format(mFmt);
        String hSrc = m1.equals(m2) ? "health_record_" + m1
            : "(SELECT user_code,record_time FROM health_record_" + m1 + " UNION ALL SELECT user_code,record_time FROM health_record_" + m2 + ")";
        String wSrc = m1.equals(m2) ? "warning_record_" + m1
            : "(SELECT user_code,create_time FROM warning_record_" + m1 + " UNION ALL SELECT user_code,create_time FROM warning_record_" + m2 + ")";
        return Result.ok("获取成功", dashboardMapper.getDeptDailyDetail(hSrc, wSrc, deptName, range[0], range[1]));
    }

    /** 各部门每日检测人数（弹窗折线图用） */
    @GetMapping("/dept-daily-persons")
    public Result<List<Map<String, Object>>> getDeptDailyPersons(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        String[] range = DateParamUtil.range30(startTime, endTime);
        return Result.ok("获取成功", dashboardMapper.getDeptDailyPersons(range[0], range[1]));
    }

    @GetMapping("/dept-stats")
    public Result<List<Map<String, Object>>> getDeptStats(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        String cacheKey = startTime + "|" + endTime;
        Object[] cached = deptStatsCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
            return Result.ok("获取成功", hit);
        }
        List<Map<String, Object>> data = dashboardService.getDeptHealthCounts(startTime, endTime);
        deptStatsCache.put(cacheKey, new Object[]{ data, System.currentTimeMillis() + DEPT_STATS_TTL });
        return Result.ok("获取成功", data);
    }

    @GetMapping("/warning-counts")
    public Result<Map<String, Object>> getWarningCounts(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "day") String groupBy) {
        String cacheKey = startTime + "|" + endTime + "|" + groupBy;
        Object[] cached = warningCountsCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hit = (Map<String, Object>) cached[0];
            return Result.ok("获取成功", hit);
        }
        Map<String, Object> data = dashboardService.getWarningDistribution(startTime, endTime, groupBy);
        warningCountsCache.put(cacheKey, new Object[]{ data, System.currentTimeMillis() + WARNING_COUNTS_TTL });
        return Result.ok("获取成功", data);
    }

    @GetMapping("/daily-trend")
    public Result<List<Map<String, Object>>> getDailyTrend(
            @RequestParam(defaultValue = "30") int days) {
        days = DateParamUtil.clampDays(days);
        String cacheKey = String.valueOf(days);
        Object[] cached = dailyTrendCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
            return Result.ok("获取成功", hit);
        }
        List<Map<String, Object>> data = dashboardService.getDailyAnomalyRates(days);
        dailyTrendCache.put(cacheKey, new Object[]{ data, System.currentTimeMillis() + DAILY_TREND_TTL });
        return Result.ok("获取成功", data);
    }

    // ═══════════════════════════════════════════════════════════════
    // 固定接口（不受时间切换影响）
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/realtime-monitor")
    public Result<Map<String, Object>> getRealtimeMonitor() {
        return Result.ok("获取成功", realtimeService.getTodayAvgOverview());
    }

    @GetMapping("/user-distribution")
    public Result<List<Map<String, Object>>> getUserDistribution() {
        return Result.ok("获取成功", dashboardService.getDeptTop5(null, null));
    }

    @GetMapping("/health-alerts")
    public Result<List<Map<String, Object>>> getHealthAlerts() {
        return Result.ok("获取成功", dashboardService.getRecentWarnings(5, null, null));
    }

    @GetMapping("/health-trend")
    public Result<Map<String, Object>> getHealthTrend(
            @RequestParam(defaultValue = "7") int days) {
        days = DateParamUtil.clampDays(days);
        return Result.ok("获取成功", dashboardService.getDailyHealthTrend(days));
    }

    @GetMapping("/department-ranking")
    public Result<List<Map<String, Object>>> getDepartmentRanking(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.ok("获取成功", dashboardService.getDeptRanking(startTime, endTime));
    }

    /**
     * 工作台日历数据：返回指定月份每天的健康均值 + 预警数
     */
    @GetMapping("/calendar")
    public Result<List<Map<String, Object>>> getCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            LocalDate today = LocalDate.now();
            int y = year  != null ? year  : today.getYear();
            int m = month != null ? month : today.getMonthValue();

            String cacheKey = y + "|" + m;
            Object[] cached = calendarCache.get(cacheKey);
            if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
                return Result.ok("获取成功", hit);
            }

            YearMonth ym = YearMonth.of(y, m);
            String startDate = ym.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String endDate   = ym.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE);

            // 每日健康均值
            List<Map<String, Object>> trends = dashboardService.getDailyHealthTrendByRange(startDate, endDate);
            // 每日预警数
            List<Map<String, Object>> warnings = dashboardService.getWarningCountsByDate(startDate, endDate);

            // 合并：以 date 为 key
            Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
            for (Map<String, Object> t : trends) {
                String date = String.valueOf(t.get("date"));
                merged.put(date, new LinkedHashMap<>(t));
            }
            for (Map<String, Object> w : warnings) {
                String date = String.valueOf(w.get("stat_date"));
                merged.computeIfAbsent(date, k -> { Map<String, Object> d = new LinkedHashMap<>(); d.put("date", k); return d; })
                      .put("warningCount", w.get("cnt"));
            }

            // 对没有预警数的日期补 0
            merged.values().forEach(d -> d.putIfAbsent("warningCount", 0));

            List<Map<String, Object>> result = new ArrayList<>(merged.values());
            result.sort(Comparator.comparing(d -> String.valueOf(d.get("date"))));
            calendarCache.put(cacheKey, new Object[]{ result, System.currentTimeMillis() + CALENDAR_TTL });
            return Result.ok("获取成功", result);
        } catch (Exception e) {
            log.error("获取日历数据失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/calendar/day-heart-rate")
    public Result<List<Map<String, Object>>> getDayHeartRateRank(@RequestParam String date) {
        return Result.ok("获取成功", dashboardService.getDayHeartRateRank(date));
    }

    @GetMapping("/calendar/day-blood-oxygen")
    public Result<List<Map<String, Object>>> getDayBloodOxygenRank(@RequestParam String date) {
        return Result.ok("获取成功", dashboardService.getDayBloodOxygenRank(date));
    }

    @GetMapping("/calendar/day-steps")
    public Result<List<Map<String, Object>>> getDayStepsRank(@RequestParam String date) {
        return Result.ok("获取成功", dashboardService.getDayStepsRank(date));
    }

    @GetMapping("/calendar/day-warnings")
    public Result<List<Map<String, Object>>> getDayWarnings(@RequestParam String date) {
        return Result.ok("获取成功", dashboardService.getDayWarnings(date));
    }

    /** 今日班前健康达标率 */
    @GetMapping("/pre-shift-compliance")
    public Result<Map<String, Object>> getPreShiftCompliance() {
        try {
            Map<String, Object> data = dashboardMapper.getTodayPreShiftCompliance();
            if (data == null) data = new HashMap<>();
            int total = data.get("totalToday") != null ? ((Number) data.get("totalToday")).intValue() : 0;
            int qualified = data.get("qualifiedCount") != null ? ((Number) data.get("qualifiedCount")).intValue() : 0;
            int failed = data.get("failedCount") != null ? ((Number) data.get("failedCount")).intValue() : 0;
            int rate = total > 0 ? Math.round(qualified * 100f / total) : 0;
            data.put("totalToday", total);
            data.put("qualifiedCount", qualified);
            data.put("failedCount", failed);
            data.put("preShiftRate", rate);
            return Result.ok("获取成功", data);
        } catch (Exception e) {
            log.error("获取班前达标率失败", e);
            return Result.error("获取失败");
        }
    }

    /** 今日入井准入名单 */
    @GetMapping("/mine-entry-list")
    public Result<List<Map<String, Object>>> getMineEntryList(
            @RequestParam(defaultValue = "1000") int size) {
        try {
            return Result.ok("获取成功", dashboardMapper.getTodayMineEntryList(size));
        } catch (Exception e) {
            log.error("获取入井名单失败", e);
            return Result.error("获取失败");
        }
    }

    /** 部门健康对比（雷达图数据） — 直接查分区表，避免扫 v_health_record UNION ALL 视图 */
    @GetMapping("/dept-health-comparison")
    public Result<List<Map<String, Object>>> getDeptHealthComparison(
            @RequestParam(defaultValue = "7") int days) {
        try {
            String cacheKey = String.valueOf(days);
            Object[] cached = deptHealthCompCache.get(cacheKey);
            if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
                return Result.ok("获取成功", hit);
            }
            DateTimeFormatter mFmt = DateTimeFormatter.ofPattern("yyyyMM");
            String curMonth  = LocalDate.now().format(mFmt);
            String prevMonth = LocalDate.now().minusDays(days).format(mFmt);
            String tblSrc;
            if (curMonth.equals(prevMonth)) {
                tblSrc = "health_record_" + curMonth;
            } else {
                String cols = "user_code,heart_rate,blood_oxygen,blood_pressure_high," +
                              "sleep_minutes,steps,pressure,record_time";
                // 无内嵌别名，mapper SQL 中 "FROM ${tableSource} hr" 会追加 "hr" 作为唯一别名
                tblSrc = "(SELECT " + cols + " FROM health_record_" + prevMonth +
                         " UNION ALL SELECT " + cols + " FROM health_record_" + curMonth + ")";
            }
            List<Map<String, Object>> data = dashboardMapper.getDeptHealthComparisonDirect(tblSrc, days);
            deptHealthCompCache.put(cacheKey, new Object[]{ data, System.currentTimeMillis() + DEPT_HEALTH_COMP_TTL });
            return Result.ok("获取成功", data);
        } catch (Exception e) {
            log.error("获取部门健康对比数据失败", e);
            return Result.error("获取失败");
        }
    }
}
