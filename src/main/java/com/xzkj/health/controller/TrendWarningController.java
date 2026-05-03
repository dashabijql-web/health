package com.xzkj.health.controller;

import com.xzkj.health.common.Result;
import com.xzkj.health.service.TrendWarningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/trend-warning")
public class TrendWarningController {

    @Autowired
    private TrendWarningService trendWarningService;

    // 预测计算较重（全员 14 天数据 + 线性回归），缓存 5 分钟
    private final ConcurrentHashMap<String, Object[]> cache = new ConcurrentHashMap<>();
    private static final long TTL = 5 * 60 * 1000L;

    @GetMapping("/predict")
    public Result<Map<String, Object>> predict() {
        Object[] cached = cache.get("predict");
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hit = (Map<String, Object>) cached[0];
            return Result.ok("获取成功", hit);
        }
        try {
            Map<String, Object> data = trendWarningService.predict();
            cache.put("predict", new Object[]{ data, System.currentTimeMillis() + TTL });
            return Result.ok("获取成功", data);
        } catch (Exception e) {
            log.error("趋势预警预测失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}
