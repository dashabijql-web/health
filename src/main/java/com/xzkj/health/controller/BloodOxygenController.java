package com.xzkj.health.controller;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.Result;
import com.xzkj.health.service.BloodOxygenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 血氧监测控制器
 */
@RestController
@RequestMapping("/blood-oxygen")
public class BloodOxygenController {

    @Autowired
    private BloodOxygenService bloodOxygenService;

    // ── 模块级缓存（overview/department-stats/age-stats/trend 查询较慢）──
    private final ConcurrentHashMap<String, Object[]> overviewCache  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> deptStatsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> ageStatsCache  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> trendCache     = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 10 * 60 * 1000L; // 10 分钟

    /** 获取血氧统计概览 */
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
        Map<String, Object> data = bloodOxygenService.getBloodOxygenStats(d[0], d[1]);
        overviewCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 获取血氧趋势数据 */
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
        Map<String, Object> data = bloodOxygenService.getBloodOxygenTrend(days);
        trendCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 获取血氧分布数据 */
    @GetMapping("/distribution")
    public Result<List<Map<String, Object>>> getDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", bloodOxygenService.getBloodOxygenDistribution(d[0], d[1]));
    }

    /** 获取 TOP N 血氧异常人员 */
    @GetMapping("/top-users")
    public Result<List<Map<String, Object>>> getTopUsers(
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", bloodOxygenService.getTopUsers(limit, d[0], d[1]));
    }

    /** 获取部门血氧统计 */
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
        List<Map<String, Object>> data = bloodOxygenService.getDepartmentStats(d[0], d[1]);
        deptStatsCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 获取年龄段血氧统计 */
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
        List<Map<String, Object>> data = bloodOxygenService.getAgeDistribution(d[0], d[1]);
        ageStatsCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 获取逐小时平均血氧（支持单日或日期范围） */
    @GetMapping("/hourly")
    public Result<List<Map<String, Object>>> getHourly(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        startDate = DateParamUtil.today(startDate);
        endDate = DateParamUtil.today(endDate);
        return Result.ok("获取成功", bloodOxygenService.getHourlyStats(startDate, endDate));
    }

    /** 实时血氧列表（近2小时） */
    @GetMapping("/realtime")
    public Result<List<Map<String, Object>>> getRealtime(
            @RequestParam(defaultValue = "1000") Integer limit) {
        return Result.ok("获取成功", bloodOxygenService.getRealtime(limit));
    }
}
