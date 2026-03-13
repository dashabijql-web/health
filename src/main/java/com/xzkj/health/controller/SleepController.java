package com.xzkj.health.controller;

import com.xzkj.health.common.Result;
import com.xzkj.health.service.SleepService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 睡眠监测控制器
 */
@Slf4j
@RestController
@RequestMapping("/sleep")
public class SleepController {

    @Autowired
    private SleepService sleepService;

    /** 获取睡眠统计概览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        return Result.ok("获取成功", sleepService.getSleepStats());
    }

    /** 获取睡眠趋势数据 */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        return Result.ok("获取成功", sleepService.getSleepTrend(days));
    }

    /** 获取睡眠质量分布 */
    @GetMapping("/quality-distribution")
    public Result<Map<String, Object>> getQualityDistribution() {
        return Result.ok("获取成功", sleepService.getQualityDistribution());
    }

    /** 获取睡眠不足记录（分页） */
    @GetMapping("/insufficient")
    public Result<Map<String, Object>> getInsufficient(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok("获取成功", sleepService.getInsufficientRecords(page, size));
    }

    /** 获取睡眠详细记录（按日期范围） */
    @GetMapping("/records")
    public Result<List<Map<String, Object>>> getRecords(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.ok("获取成功", sleepService.getSleepRecords(startDate, endDate));
    }

    /** 获取睡眠页面完整数据 */
    @GetMapping("/page-data")
    public Result<Map<String, Object>> getPageData() {
        return Result.ok("获取成功", sleepService.getSleepPageData());
    }
}
