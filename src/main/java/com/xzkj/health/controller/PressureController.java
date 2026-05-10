package com.xzkj.health.controller;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.Result;
import com.xzkj.health.dto.pressure.PressureAbnormalPageView;
import com.xzkj.health.dto.pressure.PressureDepartmentStatView;
import com.xzkj.health.dto.pressure.PressureDistributionItemView;
import com.xzkj.health.dto.pressure.PressureHourlyView;
import com.xzkj.health.dto.pressure.PressureOverviewView;
import com.xzkj.health.dto.pressure.PressureRealtimeView;
import com.xzkj.health.dto.pressure.PressureTopUserView;
import com.xzkj.health.dto.pressure.PressureTrendView;
import com.xzkj.health.service.PressureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 压力指数分析控制器
 */
@RestController
@RequestMapping("/pressure")
public class PressureController {

    @Autowired
    private PressureService pressureService;

    @GetMapping("/overview")
    public Result<PressureOverviewView> getOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", pressureService.getPressureOverview(d[0], d[1]));
    }

    @GetMapping("/trend")
    public Result<PressureTrendView> getTrend(
            @RequestParam(defaultValue = "30") Integer days) {
        days = DateParamUtil.clampDays(days);
        return Result.ok("获取成功", pressureService.getPressureTrend(days));
    }

    @GetMapping("/distribution")
    public Result<List<PressureDistributionItemView>> getDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", pressureService.getDistribution(d[0], d[1]));
    }

    @GetMapping("/top-users")
    public Result<List<PressureTopUserView>> getTopUsers(
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", pressureService.getTopUsers(limit, d[0], d[1]));
    }

    @GetMapping("/department-stats")
    public Result<List<PressureDepartmentStatView>> getDepartmentStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", pressureService.getDepartmentStats(d[0], d[1]));
    }

    @GetMapping("/realtime")
    public Result<List<PressureRealtimeView>> getRealtime(
            @RequestParam(defaultValue = "1000") Integer limit) {
        return Result.ok("获取成功", pressureService.getRealtime(limit));
    }

    @GetMapping("/hourly")
    public Result<List<PressureHourlyView>> getHourly(
            @RequestParam(required = false) String date) {
        return Result.ok("获取成功", pressureService.getHourlyStats(DateParamUtil.today(date)));
    }

    @GetMapping("/abnormal")
    public Result<PressureAbnormalPageView> getAbnormal(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        size = DateParamUtil.clampSize(size);
        return Result.ok("获取成功", pressureService.getAbnormalRecords(page, size));
    }
}
