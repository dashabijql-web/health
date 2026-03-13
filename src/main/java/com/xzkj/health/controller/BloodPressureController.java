package com.xzkj.health.controller;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.Result;
import com.xzkj.health.service.BloodPressureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 血压分析控制器
 */
@RestController
@RequestMapping("/blood-pressure")
public class BloodPressureController {

    @Autowired
    private BloodPressureService bloodPressureService;

    /** 概览统计 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", bloodPressureService.getOverview(d[0], d[1]));
    }

    /** 趋势折线图（收缩压 + 舒张压） */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend(
            @RequestParam(defaultValue = "30") Integer days) {
        days = DateParamUtil.clampDays(days);
        return Result.ok("获取成功", bloodPressureService.getTrend(days));
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
        return Result.ok("获取成功", bloodPressureService.getDepartmentStats(d[0], d[1]));
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
