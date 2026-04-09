package com.xzkj.health.controller;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.Result;
import com.xzkj.health.service.SleepService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 睡眠监测控制器
 */
@RestController
@RequestMapping("/sleep")
public class SleepController {

    @Autowired
    private SleepService sleepService;

    // ── 模块级缓存（page-data/trend/quality-distribution 查询较慢）──
    private final ConcurrentHashMap<String, Object[]> pageDataCache          = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> trendCache             = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object[]> qualityDistCache       = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 10 * 60 * 1000L; // 10 分钟

    /** 获取睡眠趋势数据 */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        days = DateParamUtil.clampDays(days);
        String key = "days:" + days;
        Object[] cached = trendCache.get(key);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hit = (Map<String, Object>) cached[0];
            return Result.ok("获取成功", hit);
        }
        Map<String, Object> data = sleepService.getSleepTrend(days);
        trendCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 获取睡眠质量分布 */
    @GetMapping("/quality-distribution")
    public Result<Map<String, Object>> getQualityDistribution() {
        String key = "all";
        Object[] cached = qualityDistCache.get(key);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hit = (Map<String, Object>) cached[0];
            return Result.ok("获取成功", hit);
        }
        Map<String, Object> data = sleepService.getQualityDistribution();
        qualityDistCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }

    /** 获取睡眠页面完整数据 */
    @GetMapping("/page-data")
    public Result<Map<String, Object>> getPageData() {
        String key = "all";
        Object[] cached = pageDataCache.get(key);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hit = (Map<String, Object>) cached[0];
            return Result.ok("获取成功", hit);
        }
        Map<String, Object> data = sleepService.getSleepPageData();
        pageDataCache.put(key, new Object[]{ data, System.currentTimeMillis() + CACHE_TTL });
        return Result.ok("获取成功", data);
    }
}
