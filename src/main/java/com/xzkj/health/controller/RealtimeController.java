package com.xzkj.health.controller;

import com.xzkj.health.common.Result;
import com.xzkj.health.service.RealtimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 实时监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/realtime")
public class RealtimeController {

    @Autowired
    private RealtimeService realtimeService;

    /** 获取当日平均数据概览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        return Result.ok("获取成功", realtimeService.getTodayAvgOverview());
    }

    /** 获取在线用户列表（分页） */
    @GetMapping("/online-users")
    public Result<Map<String, Object>> getOnlineUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10000") Integer size) {
        return Result.ok("获取成功", realtimeService.getOnlineUsers(page, size));
    }

    /** 获取单个用户实时数据 */
    @GetMapping("/user/{userCode}")
    public Result<Map<String, Object>> getUserData(@PathVariable String userCode) {
        return Result.ok("获取成功", realtimeService.getUserRealtimeData(userCode));
    }

    /** 获取实时统计汇总 */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.ok("获取成功", realtimeService.getStatistics());
    }

    /** 获取近期未处理告警列表（安全指挥中心实时告警栏） */
    @GetMapping("/alerts")
    public Result<List<Map<String, Object>>> getAlerts(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.ok("获取成功", realtimeService.getRealtimeAlerts(limit));
    }
}
