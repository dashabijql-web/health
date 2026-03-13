package com.xzkj.health.service;

import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.RealtimeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 实时监控Service - 更新版
 */
@Slf4j
@Service
public class RealtimeService {

    @Autowired
    private RealtimeMapper realtimeMapper;

    /**
     * 获取当日平均数据概览
     */
    public Map<String, Object> getTodayAvgOverview() {
        Map<String, Object> data = realtimeMapper.getTodayAvgData();
        if (data == null) data = new HashMap<>();

        // 格式化数据
        Map<String, Object> result = new HashMap<>();
        result.put("avgHeartRate", Math.round(MapValueUtil.getDouble(data, "avgHeartRate")));
        result.put("avgBloodOxygen", Math.round(MapValueUtil.getDouble(data, "avgBloodOxygen")));
        result.put("avgSteps", Math.round(MapValueUtil.getDouble(data, "avgSteps")));

        // 体温保留1位小数
        double avgTemp = MapValueUtil.getDouble(data, "avgTemperature");
        result.put("avgTemperature", Math.round(avgTemp * 10.0) / 10.0);

        // 睡眠保留1位小数
        double avgSleep = MapValueUtil.getDouble(data, "avgSleep");
        result.put("avgSleep", Math.round(avgSleep * 10.0) / 10.0);

        result.put("todayWarningCount", MapValueUtil.getLong(data, "todayWarningCount"));

        return result;
    }

    /**
     * 获取在线用户列表(无分页版本)
     */
    public Map<String, Object> getOnlineUsers(int page, int size) {
        int offset = (page - 1) * size;

        List<Map<String, Object>> list = realtimeMapper.getOnlineUsers(offset, size);
        if (list == null) list = Collections.emptyList();

        // 格式化数据
        for (Map<String, Object> user : list) {
            // 格式化体温(保留1位小数)
            if (user.containsKey("temperature") && user.get("temperature") != null) {
                double temp = MapValueUtil.getDouble(user, "temperature");
                user.put("temperature", Math.round(temp * 10.0) / 10.0);
            }

            // 格式化睡眠(保留1位小数)
            if (user.containsKey("sleepHours") && user.get("sleepHours") != null) {
                double sleep = MapValueUtil.getDouble(user, "sleepHours");
                user.put("sleepHours", Math.round(sleep * 10.0) / 10.0);
            }
        }

        int total = realtimeMapper.countOnlineUsers();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return result;
    }

    /**
     * 获取用户实时数据
     */
    public Map<String, Object> getUserRealtimeData(String userCode) {
        Map<String, Object> data = realtimeMapper.getUserRealtimeData(userCode);

        if (data == null) {
            data = new HashMap<>();
            data.put("userCode", userCode);
            data.put("status", "offline");
        } else {
            // 格式化体温
            if (data.containsKey("temperature") && data.get("temperature") != null) {
                double temp = MapValueUtil.getDouble(data, "temperature");
                data.put("temperature", Math.round(temp * 10.0) / 10.0);
            }

            // 格式化睡眠
            if (data.containsKey("sleepHours") && data.get("sleepHours") != null) {
                double sleep = MapValueUtil.getDouble(data, "sleepHours");
                data.put("sleepHours", Math.round(sleep * 10.0) / 10.0);
            }
        }

        return data;
    }

    /**
     * 获取实时统计数据
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> data = realtimeMapper.getStatistics();
        if (data == null) data = new HashMap<>();

        // 格式化百分比数据
        Map<String, Object> result = new HashMap<>();
        result.put("onlineUsers", MapValueUtil.getLong(data, "onlineUsers"));
        result.put("totalUsers", MapValueUtil.getLong(data, "totalUsers"));
        result.put("weekRecords", MapValueUtil.getLong(data, "weekRecords"));
        result.put("todayRecords", MapValueUtil.getLong(data, "todayRecords"));
        result.put("onlineRate", Math.round(MapValueUtil.getDouble(data, "onlineRate")));
        result.put("normalRate", Math.round(MapValueUtil.getDouble(data, "normalRate")));

        return result;
    }

    /**
     * 获取近期未处理告警列表（默认取最近20条）
     */
    public List<Map<String, Object>> getRealtimeAlerts(int limit) {
        return realtimeMapper.getRecentAlerts(limit);
    }

}