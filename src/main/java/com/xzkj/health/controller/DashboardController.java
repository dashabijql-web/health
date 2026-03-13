package com.xzkj.health.controller;

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
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private RealtimeService realtimeService;

    // ═══════════════════════════════════════════════════════════════
    // 支持时间范围筛选的接口（startTime/endTime 不传则默认当月）
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.ok("获取成功", dashboardService.getCurrentMonthCounts(startTime, endTime));
    }

    @GetMapping("/body-indicators")
    public Result<Map<String, Object>> getBodyIndicators(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.ok("获取成功", dashboardService.getCurrentMonthAverage(startTime, endTime));
    }

    @GetMapping("/top5")
    public Result<List<Map<String, Object>>> getTop5(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.ok("获取成功", dashboardService.getDeptTop5(startTime, endTime));
    }

    @GetMapping("/device-activation")
    public Result<Map<String, Object>> getDeviceActivation(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Map<String, Object> result = new HashMap<>();
        result.put("stats",        dashboardService.getDeviceStats(startTime, endTime));
        Map<String, Object> ratesResult = dashboardService.getWarningRates(startTime, endTime);
        result.put("warningRates", ratesResult != null
                ? ratesResult.getOrDefault("warningRates", Collections.emptyList())
                : Collections.emptyList());
        return Result.ok("获取成功", result);
    }

    @GetMapping("/warning-events")
    public Result<List<Map<String, Object>>> getWarningEvents(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        // 限制返回最近 200 条预警记录（按时间倒序），避免数据量过大
        return Result.ok("获取成功", dashboardService.getRecentWarnings(200, startTime, endTime));
    }

    @GetMapping("/dept-stats")
    public Result<List<Map<String, Object>>> getDeptStats(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.ok("获取成功", dashboardService.getDeptHealthCounts(startTime, endTime));
    }

    @GetMapping("/warning-counts")
    public Result<Map<String, Object>> getWarningCounts(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "day") String groupBy) {
        return Result.ok("获取成功", dashboardService.getWarningDistribution(startTime, endTime, groupBy));
    }

    @GetMapping("/daily-trend")
    public Result<List<Map<String, Object>>> getDailyTrend(
            @RequestParam(defaultValue = "30") int days) {
        days = Math.max(1, Math.min(days, 365));
        return Result.ok("获取成功", dashboardService.getDailyAnomalyRates(days));
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
        days = Math.max(1, Math.min(days, 365));
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
}
