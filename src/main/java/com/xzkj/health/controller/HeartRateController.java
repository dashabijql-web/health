package com.xzkj.health.controller;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.Result;
import com.xzkj.health.service.HeartRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 心率监测控制器
 */
@RestController
@RequestMapping("/heart-rate")
public class HeartRateController {

    @Autowired
    private HeartRateService heartRateService;

    /** 获取心率统计概览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", heartRateService.getHeartRateOverview(d[0], d[1]));
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
    public Result<List<Map<String, Object>>> getAgeStats() {
        return Result.ok("获取成功", heartRateService.getAgeDistribution());
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
        return Result.ok("获取成功", heartRateService.getHeartRateTrend(days));
    }

    /** 获取部门心率统计 */
    @GetMapping("/department-stats")
    public Result<List<Map<String, Object>>> getDepartmentStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", heartRateService.getDepartmentStats(d[0], d[1]));
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
}
