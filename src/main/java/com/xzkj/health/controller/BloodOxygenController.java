package com.xzkj.health.controller;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.Result;
import com.xzkj.health.service.BloodOxygenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 血氧监测控制器
 */
@Slf4j
@RestController
@RequestMapping("/blood-oxygen")
public class BloodOxygenController {

    @Autowired
    private BloodOxygenService bloodOxygenService;

    /** 获取血氧统计概览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", bloodOxygenService.getBloodOxygenStats(d[0], d[1]));
    }

    /** 获取血氧趋势数据 */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend(
            @RequestParam(defaultValue = "30") Integer days) {
        days = DateParamUtil.clampDays(days);
        return Result.ok("获取成功", bloodOxygenService.getBloodOxygenTrend(days));
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
        return Result.ok("获取成功", bloodOxygenService.getDepartmentStats(d[0], d[1]));
    }

    /** 获取年龄段血氧统计 */
    @GetMapping("/age-stats")
    public Result<List<Map<String, Object>>> getAgeStats() {
        return Result.ok("获取成功", bloodOxygenService.getAgeDistribution());
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
}
