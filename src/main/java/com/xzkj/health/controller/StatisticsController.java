package com.xzkj.health.controller;

import com.xzkj.health.common.Result;
import com.xzkj.health.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    // ── 模块级缓存（dept-summary ~100ms，monthly-summary ~150ms）──
    // key = 参数字符串，value = [data, expireMs]
    private final ConcurrentHashMap<String, Object[]> deptSummaryCache    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> monthlySummaryCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> dailyCountsCache    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> warningTypesCache   = new ConcurrentHashMap<>();
    private static final long DEPT_SUMMARY_TTL    = 5 * 60 * 1000L; // 5 分钟（部门汇总变化极缓慢）
    private static final long MONTHLY_SUMMARY_TTL = 2 * 60 * 1000L; // 2 分钟
    private static final long DAILY_COUNTS_TTL    = 2 * 60 * 1000L; // 2 分钟
    private static final long WARNING_TYPES_TTL   = 2 * 60 * 1000L; // 2 分钟

    @GetMapping("/dept-summary")
    public Result<List<Map<String, Object>>> getDeptHealthSummary() {
        final String cacheKey = "all";
        Object[] cached = deptSummaryCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
            return Result.ok("获取成功", hit);
        }
        try {
            List<Map<String, Object>> data = statisticsService.getDeptHealthSummary();
            deptSummaryCache.put(cacheKey, new Object[]{ data, System.currentTimeMillis() + DEPT_SUMMARY_TTL });
            return Result.ok("获取成功", data);
        } catch (Exception e) {
            log.error("获取部门健康汇总失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/monthly-summary")
    public Result<List<Map<String, Object>>> getMonthlySummary(@RequestParam String month) {
        Object[] cached = monthlySummaryCache.get(month);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
            return Result.ok("获取成功", hit);
        }
        try {
            List<Map<String, Object>> data = statisticsService.getMonthlySummary(month);
            monthlySummaryCache.put(month, new Object[]{ data, System.currentTimeMillis() + MONTHLY_SUMMARY_TTL });
            return Result.ok("获取成功", data);
        } catch (Exception e) {
            log.error("获取月度汇总失败: month={}", month, e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/daily-counts")
    public Result<List<Map<String, Object>>> getDailyCounts(@RequestParam String month) {
        Object[] cached = dailyCountsCache.get(month);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
            return Result.ok("获取成功", hit);
        }
        try {
            List<Map<String, Object>> data = statisticsService.getDailyRecordCounts(month);
            dailyCountsCache.put(month, new Object[]{ data, System.currentTimeMillis() + DAILY_COUNTS_TTL });
            return Result.ok("获取成功", data);
        } catch (Exception e) {
            log.error("获取每日记录数失败: month={}", month, e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/warning-types")
    public Result<List<Map<String, Object>>> getWarningTypes(@RequestParam String month) {
        Object[] cached = warningTypesCache.get(month);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
            return Result.ok("获取成功", hit);
        }
        try {
            List<Map<String, Object>> data = statisticsService.getWarningTypeCounts(month);
            warningTypesCache.put(month, new Object[]{ data, System.currentTimeMillis() + WARNING_TYPES_TTL });
            return Result.ok("获取成功", data);
        } catch (Exception e) {
            log.error("获取预警类型分布失败: month={}", month, e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}
