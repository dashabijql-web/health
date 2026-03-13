package com.xzkj.health.controller;

import com.xzkj.health.common.Result;
import com.xzkj.health.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/dept-summary")
    public Result<List<Map<String, Object>>> getDeptHealthSummary() {
        try {
            List<Map<String, Object>> data = statisticsService.getDeptHealthSummary();
            return Result.ok("获取成功", data);
        } catch (Exception e) {
            log.error("获取部门健康汇总失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/monthly-summary")
    public Result<List<Map<String, Object>>> getMonthlySummary(@RequestParam String month) {
        try {
            List<Map<String, Object>> data = statisticsService.getMonthlySummary(month);
            return Result.ok("获取成功", data);
        } catch (Exception e) {
            log.error("获取月度汇总失败: month={}", month, e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/daily-counts")
    public Result<List<Map<String, Object>>> getDailyCounts(@RequestParam String month) {
        try {
            List<Map<String, Object>> data = statisticsService.getDailyRecordCounts(month);
            return Result.ok("获取成功", data);
        } catch (Exception e) {
            log.error("获取每日记录数失败: month={}", month, e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/warning-types")
    public Result<List<Map<String, Object>>> getWarningTypes(@RequestParam String month) {
        try {
            List<Map<String, Object>> data = statisticsService.getWarningTypeCounts(month);
            return Result.ok("获取成功", data);
        } catch (Exception e) {
            log.error("获取预警类型分布失败: month={}", month, e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}
