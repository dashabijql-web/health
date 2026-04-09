package com.xzkj.health.controller;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.Result;
import com.xzkj.health.service.PressureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 压力指数分析控制器
 */
@RestController
@RequestMapping("/pressure")
public class PressureController {

    @Autowired
    private PressureService pressureService;

    // ── 模块级缓存（overview/department-stats/trend 查询较慢）──
    private final ConcurrentHashMap<String, Object[]> overviewCache  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> deptStatsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> trendCache     = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 10 * 60 * 1000L; // 10 分钟

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
        Map<String, Object> data = pressureService.getOverview(d[0], d[1]);
        overviewCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

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
        Map<String, Object> data = pressureService.getTrend(days);
        trendCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    @GetMapping("/distribution")
    public Result<List<Map<String, Object>>> getDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", pressureService.getDistribution(d[0], d[1]));
    }

    @GetMapping("/top-users")
    public Result<List<Map<String, Object>>> getTopUsers(
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", pressureService.getTopUsers(limit, d[0], d[1]));
    }

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
        List<Map<String, Object>> data = pressureService.getDepartmentStats(d[0], d[1]);
        deptStatsCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    @GetMapping("/realtime")
    public Result<List<Map<String, Object>>> getRealtime(
            @RequestParam(defaultValue = "1000") Integer limit) {
        return Result.ok("获取成功", pressureService.getRealtime(limit));
    }

    @GetMapping("/hourly")
    public Result<List<Map<String, Object>>> getHourly(
            @RequestParam(required = false) String date) {
        return Result.ok("获取成功", pressureService.getHourlyStats(DateParamUtil.today(date)));
    }

    @GetMapping("/abnormal")
    public Result<Map<String, Object>> getAbnormal(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        size = DateParamUtil.clampSize(size);
        return Result.ok("获取成功", pressureService.getAbnormalRecords(page, size));
    }
}
