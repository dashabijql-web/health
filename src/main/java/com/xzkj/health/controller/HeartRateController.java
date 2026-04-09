package com.xzkj.health.controller;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.Result;
import com.xzkj.health.service.HeartRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 心率监测控制器
 */
@RestController
@RequestMapping("/heart-rate")
public class HeartRateController {

    @Autowired
    private HeartRateService heartRateService;

    // ── 模块级缓存（overview/department-stats/age-stats/trend 查询较慢）──
    private final ConcurrentHashMap<String, Object[]> overviewCache    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> deptStatsCache   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> ageStatsCache    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> trendCache       = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 10 * 60 * 1000L; // 10 分钟

    /** 获取心率统计概览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        String key = d[0] + "|" + d[1];
        Object[] cached = overviewCache.get(key);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hit = (Map<String, Object>) cached[0];
            return Result.ok("获取成功", hit);
        }
        Map<String, Object> data = heartRateService.getHeartRateOverview(d[0], d[1]);
        overviewCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 获取 TOP N 心率异常人员 */
    @GetMapping("/top-users")
    public Result<List<Map<String, Object>>> getTopUsers(
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", heartRateService.getTopUsers(limit, d[0], d[1]));
    }

    /** 获取年龄段心率统计 */
    @GetMapping("/age-stats")
    public Result<List<Map<String, Object>>> getAgeStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        String key = d[0] + "|" + d[1];
        Object[] cached = ageStatsCache.get(key);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
            return Result.ok("获取成功", hit);
        }
        List<Map<String, Object>> data = heartRateService.getAgeDistribution(d[0], d[1]);
        ageStatsCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 获取心率分布统计（偏低/正常/偏高） */
    @GetMapping("/distribution")
    public Result<List<Map<String, Object>>> getDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", heartRateService.getHeartRateDistribution(d[0], d[1]));
    }

    /** 获取心率趋势数据 */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend(
            @RequestParam(defaultValue = "30") Integer days) {
        days = DateParamUtil.clampDays(days);
        String key = "days:" + days;
        Object[] cached = trendCache.get(key);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hit = (Map<String, Object>) cached[0];
            return Result.ok("获取成功", hit);
        }
        Map<String, Object> data = heartRateService.getHeartRateTrend(days);
        trendCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 获取部门心率统计 */
    @GetMapping("/department-stats")
    public Result<List<Map<String, Object>>> getDepartmentStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        String key = d[0] + "|" + d[1];
        Object[] cached = deptStatsCache.get(key);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hit = (List<Map<String, Object>>) cached[0];
            return Result.ok("获取成功", hit);
        }
        List<Map<String, Object>> data = heartRateService.getDepartmentStats(d[0], d[1]);
        deptStatsCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 获取逐小时平均心率（支持单日或日期范围） */
    @GetMapping("/hourly")
    public Result<List<Map<String, Object>>> getHourly(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        startDate = DateParamUtil.today(startDate);
        endDate = DateParamUtil.today(endDate);
        return Result.ok("获取成功", heartRateService.getHourlyStats(startDate, endDate));
    }

    /** 实时心率列表（近2小时） */
    @GetMapping("/realtime")
    public Result<List<Map<String, Object>>> getRealtime(
            @RequestParam(defaultValue = "1000") Integer limit) {
        return Result.ok("获取成功", heartRateService.getRealtime(limit));
    }

    /** 按日统计心率异常人次 */
    @GetMapping("/daily-anomaly")
    public Result<List<Map<String, Object>>> getDailyAnomaly(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", heartRateService.getDailyAnomalyCount(d[0], d[1]));
    }
}
