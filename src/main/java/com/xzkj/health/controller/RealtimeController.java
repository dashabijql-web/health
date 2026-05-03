package com.xzkj.health.controller;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.Result;
import com.xzkj.health.service.RealtimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 实时监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/realtime")
public class RealtimeController {

    @Autowired
    private RealtimeService realtimeService;

    // 30 秒内存缓存：/statistics 查询约 400ms，被 dashboard 每 30s 轮询一次，直接缓存整个结果即可
    private static final long STATISTICS_TTL = 30_000L;
    private final AtomicReference<Map<String, Object>> statisticsCache = new AtomicReference<>();
    private final AtomicLong statisticsCacheExpire = new AtomicLong(0L);

    // 10 秒内存缓存：/online-users(page=1,size≥200) 每次查询约 170ms + 235KB 序列化，
    // 前端固定 15s 刷新一次，用 10s 缓存可避免多标签页/Dashboard 重复打 DB。
    // 只缓存 page=1 的全量拉取；分页翻页请求（page>1）不缓存。
    private static final long ONLINE_USERS_TTL = 10_000L;
    private final AtomicReference<Map<String, Object>> onlineUsersCache = new AtomicReference<>();
    private final AtomicLong onlineUsersCacheExpire = new AtomicLong(0L);
    private volatile int onlineUsersCachedSize = -1;

    /** 获取当日平均数据概览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        return Result.ok("获取成功", realtimeService.getTodayAvgOverview());
    }

    /** 获取在线用户列表（分页）；page=1 且 size≥200 走 10s 内存缓存 */
    @GetMapping("/online-users")
    public Result<Map<String, Object>> getOnlineUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10000") Integer size) {
        page = Math.max(1, page == null ? 1 : page);
        size = Math.max(1, DateParamUtil.clampSize(size == null ? 10000 : size, 10000));
        // 只对 page=1 的全量请求做缓存（size≥200 视为全量拉取）
        if (page == 1 && size >= 200) {
            long now = System.currentTimeMillis();
            Map<String, Object> cached = onlineUsersCache.get();
            if (cached != null && now < onlineUsersCacheExpire.get() && onlineUsersCachedSize == size) {
                return Result.ok("获取成功", cached);
            }
            try {
                Map<String, Object> data = realtimeService.getOnlineUsers(page, size);
                onlineUsersCache.set(data);
                onlineUsersCacheExpire.set(System.currentTimeMillis() + ONLINE_USERS_TTL);
                onlineUsersCachedSize = size;
                return Result.ok("获取成功", data);
            } catch (Exception e) {
                log.warn("实时监控在线用户查询失败，尝试返回过期缓存: {}", e.getMessage());
                if (cached != null) {
                    Map<String, Object> stale = new HashMap<>(cached);
                    stale.put("stale", true);
                    return Result.ok("获取成功（缓存）", stale);
                }
                return Result.error("实时监控数据加载失败，请稍后重试");
            }
        }
        return Result.ok("获取成功", realtimeService.getOnlineUsers(page, size));
    }

    /** 获取单个用户实时数据 */
    @GetMapping("/user/{userCode}")
    public Result<Map<String, Object>> getUserData(@PathVariable String userCode) {
        return Result.ok("获取成功", realtimeService.getUserRealtimeData(userCode));
    }

    /** 获取实时统计汇总（30s 内存缓存，避免 400ms DB 查询被高频轮询） */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        long now = System.currentTimeMillis();
        if (now < statisticsCacheExpire.get() && statisticsCache.get() != null) {
            return Result.ok("获取成功", statisticsCache.get());
        }
        Map<String, Object> data = realtimeService.getStatistics();
        statisticsCache.set(data);
        statisticsCacheExpire.set(now + STATISTICS_TTL);
        return Result.ok("获取成功", data);
    }

    /** 获取近期未处理告警列表（安全指挥中心实时告警栏） */
    @GetMapping("/alerts")
    public Result<List<Map<String, Object>>> getAlerts(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.ok("获取成功", realtimeService.getRealtimeAlerts(limit));
    }
}
