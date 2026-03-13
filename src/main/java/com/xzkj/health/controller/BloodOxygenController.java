package com.xzkj.health.controller;

import com.xzkj.health.common.Result;
import com.xzkj.health.service.BloodOxygenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
        if (startDate == null) startDate = LocalDate.now().minusDays(29).toString();
        if (endDate == null) endDate = LocalDate.now().toString();
        return Result.ok("获取成功", bloodOxygenService.getBloodOxygenStats(startDate, endDate));
    }

    /** 获取血氧趋势数据 */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend(
            @RequestParam(defaultValue = "30") Integer days) {
        return Result.ok("获取成功", bloodOxygenService.getBloodOxygenTrend(days));
    }

    /** 获取血氧分布数据 */
    @GetMapping("/distribution")
    public Result<List<Map<String, Object>>> getDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(29).toString();
        if (endDate == null) endDate = LocalDate.now().toString();
        return Result.ok("获取成功", bloodOxygenService.getBloodOxygenDistribution(startDate, endDate));
    }

    /** 获取异常血氧记录（分页） */
    @GetMapping("/abnormal")
    public Result<Map<String, Object>> getAbnormal(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok("获取成功", bloodOxygenService.getAbnormalRecords(page, size));
    }

    /** 获取 TOP N 血氧异常人员 */
    @GetMapping("/top-users")
    public Result<List<Map<String, Object>>> getTopUsers(
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(29).toString();
        if (endDate == null) endDate = LocalDate.now().toString();
        return Result.ok("获取成功", bloodOxygenService.getTopUsers(limit, startDate, endDate));
    }

    /** 获取部门血氧统计 */
    @GetMapping("/department-stats")
    public Result<List<Map<String, Object>>> getDepartmentStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(29).toString();
        if (endDate == null) endDate = LocalDate.now().toString();
        return Result.ok("获取成功", bloodOxygenService.getDepartmentStats(startDate, endDate));
    }

    /** 获取年龄段血氧统计 */
    @GetMapping("/age-stats")
    public Result<List<Map<String, Object>>> getAgeStats() {
        return Result.ok("获取成功", bloodOxygenService.getAgeDistribution());
    }

    /** 获取实时血氧数据 */
    @GetMapping("/realtime")
    public Result<List<Map<String, Object>>> getRealtime(
            @RequestParam(defaultValue = "20") Integer limit) {
        return Result.ok("获取成功", bloodOxygenService.getRealtimeData(limit));
    }

    /** 获取逐小时平均血氧（支持单日或日期范围） */
    @GetMapping("/hourly")
    public Result<List<Map<String, Object>>> getHourly(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (startDate == null) startDate = LocalDate.now().toString();
        if (endDate == null) endDate = LocalDate.now().toString();
        return Result.ok("获取成功", bloodOxygenService.getHourlyStats(startDate, endDate));
    }
}
