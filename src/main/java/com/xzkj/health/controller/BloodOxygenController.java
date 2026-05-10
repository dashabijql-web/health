package com.xzkj.health.controller;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.Result;
import com.xzkj.health.dto.bloodoxygen.BloodOxygenAgeStatView;
import com.xzkj.health.dto.bloodoxygen.BloodOxygenDepartmentStatView;
import com.xzkj.health.dto.bloodoxygen.BloodOxygenDistributionItemView;
import com.xzkj.health.dto.bloodoxygen.BloodOxygenHourlyView;
import com.xzkj.health.dto.bloodoxygen.BloodOxygenOverviewView;
import com.xzkj.health.dto.bloodoxygen.BloodOxygenRealtimeView;
import com.xzkj.health.dto.bloodoxygen.BloodOxygenTopUserView;
import com.xzkj.health.dto.bloodoxygen.BloodOxygenTrendView;
import com.xzkj.health.service.BloodOxygenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 血氧监测控制器
 */
@RestController
@RequestMapping("/blood-oxygen")
public class BloodOxygenController {

    @Autowired
    private BloodOxygenService bloodOxygenService;

    /** 获取血氧统计概览 */
    @GetMapping("/overview")
    public Result<BloodOxygenOverviewView> getOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", bloodOxygenService.getBloodOxygenStats(d[0], d[1]));
    }

    /** 获取血氧趋势数据 */
    @GetMapping("/trend")
    public Result<BloodOxygenTrendView> getTrend(
            @RequestParam(defaultValue = "30") Integer days) {
        days = DateParamUtil.clampDays(days);
        return Result.ok("获取成功", bloodOxygenService.getBloodOxygenTrend(days));
    }

    /** 获取血氧分布数据 */
    @GetMapping("/distribution")
    public Result<List<BloodOxygenDistributionItemView>> getDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", bloodOxygenService.getBloodOxygenDistribution(d[0], d[1]));
    }

    /** 获取 TOP N 血氧异常人员 */
    @GetMapping("/top-users")
    public Result<List<BloodOxygenTopUserView>> getTopUsers(
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", bloodOxygenService.getTopUsers(limit, d[0], d[1]));
    }

    /** 获取部门血氧统计 */
    @GetMapping("/department-stats")
    public Result<List<BloodOxygenDepartmentStatView>> getDepartmentStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", bloodOxygenService.getDepartmentStats(d[0], d[1]));
    }

    /** 获取年龄段血氧统计 */
    @GetMapping("/age-stats")
    public Result<List<BloodOxygenAgeStatView>> getAgeStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String[] d = DateParamUtil.range30(startDate, endDate);
        return Result.ok("获取成功", bloodOxygenService.getAgeDistribution(d[0], d[1]));
    }

    /** 获取逐小时平均血氧（支持单日或日期范围） */
    @GetMapping("/hourly")
    public Result<List<BloodOxygenHourlyView>> getHourly(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        startDate = DateParamUtil.today(startDate);
        endDate = DateParamUtil.today(endDate);
        return Result.ok("获取成功", bloodOxygenService.getHourlyStats(startDate, endDate));
    }

    /** 实时血氧列表（近2小时） */
    @GetMapping("/realtime")
    public Result<List<BloodOxygenRealtimeView>> getRealtime(
            @RequestParam(defaultValue = "1000") Integer limit) {
        return Result.ok("获取成功", bloodOxygenService.getRealtime(limit));
    }
}
