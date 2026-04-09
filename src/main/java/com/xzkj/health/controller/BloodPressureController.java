package com.xzkj.health.controller;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.Result;
import com.xzkj.health.service.BloodPressureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 血压分析控制器
 */
@RestController
@RequestMapping("/blood-pressure")
public class BloodPressureController {

    @Autowired
    private BloodPressureService bloodPressureService;

    // ── 模块级缓存（overview/department-stats/trend 查询较慢）──
    private final ConcurrentHashMap<String, Object[]> overviewCache  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> deptStatsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> trendCache     = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 10 * 60 * 1000L; // 10 分钟

    /** 概览统计 */
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
        Map<String, Object> data = bloodPressureService.getOverview(d[0], d[1]);
        overviewCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 趋势折线图（收缩压 + 舒张压） */
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
        Map<String, Object> data = bloodPressureService.getTrend(days);
        trendCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 分布饼图 */
    @GetMapping("/distribution")
    public Result<List<Map<String, Object>>> getDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", bloodPressureService.getDistribution(d[0], d[1]));
    }

    /** TOP N 高血压人员 */
    @GetMapping("/top-users")
    public Result<List<Map<String, Object>>> getTopUsers(
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", bloodPressureService.getTopUsers(limit, d[0], d[1]));
    }

    /** 部门血压统计 */
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
        List<Map<String, Object>> data = bloodPressureService.getDepartmentStats(d[0], d[1]);
        deptStatsCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 实时血压列表 */
    @GetMapping("/realtime")
    public Result<List<Map<String, Object>>> getRealtime(
            @RequestParam(defaultValue = "1000") Integer limit) {
        return Result.ok("获取成功", bloodPressureService.getRealtime(limit));
    }

    /** 指定日期逐小时均值 */
    @GetMapping("/hourly")
    public Result<List<Map<String, Object>>> getHourly(
            @RequestParam(required = false) String date) {
        return Result.ok("获取成功", bloodPressureService.getHourlyStats(DateParamUtil.today(date)));
    }

    /** 异常记录分页 */
    @GetMapping("/abnormal")
    public Result<Map<String, Object>> getAbnormal(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        size = DateParamUtil.clampSize(size);
        return Result.ok("获取成功", bloodPressureService.getAbnormalRecords(page, size));
    }
}
