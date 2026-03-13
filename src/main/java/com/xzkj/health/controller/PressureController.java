package com.xzkj.health.controller;

import com.xzkj.health.common.Result;
import com.xzkj.health.service.PressureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 压力指数分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/pressure")
public class PressureController {

    @Autowired
    private PressureService pressureService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(29).toString();
        if (endDate == null) endDate = LocalDate.now().toString();
        return Result.ok("获取成功", pressureService.getOverview(startDate, endDate));
    }

    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend(
            @RequestParam(defaultValue = "30") Integer days) {
        days = Math.max(1, Math.min(days, 365));
        return Result.ok("获取成功", pressureService.getTrend(days));
    }

    @GetMapping("/distribution")
    public Result<List<Map<String, Object>>> getDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(29).toString();
        if (endDate == null) endDate = LocalDate.now().toString();
        return Result.ok("获取成功", pressureService.getDistribution(startDate, endDate));
    }

    @GetMapping("/top-users")
    public Result<List<Map<String, Object>>> getTopUsers(
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(29).toString();
        if (endDate == null) endDate = LocalDate.now().toString();
        return Result.ok("获取成功", pressureService.getTopUsers(limit, startDate, endDate));
    }

    @GetMapping("/department-stats")
    public Result<List<Map<String, Object>>> getDepartmentStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(29).toString();
        if (endDate == null) endDate = LocalDate.now().toString();
        return Result.ok("获取成功", pressureService.getDepartmentStats(startDate, endDate));
    }

    @GetMapping("/realtime")
    public Result<List<Map<String, Object>>> getRealtime(
            @RequestParam(defaultValue = "1000") Integer limit) {
        return Result.ok("获取成功", pressureService.getRealtime(limit));
    }

    @GetMapping("/hourly")
    public Result<List<Map<String, Object>>> getHourly(
            @RequestParam(required = false) String date) {
        if (date == null) date = LocalDate.now().toString();
        return Result.ok("获取成功", pressureService.getHourlyStats(date));
    }

    @GetMapping("/abnormal")
    public Result<Map<String, Object>> getAbnormal(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        size = Math.min(size, 200);
        return Result.ok("获取成功", pressureService.getAbnormalRecords(page, size));
    }
}
