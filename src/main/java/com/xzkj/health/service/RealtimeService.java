package com.xzkj.health.service;

import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.RealtimeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    /** 计算近168h查询所需的分区表表达式（避免扫全部UNION ALL视图）
     *
     * 注意：跨月情况返回的是不含 AS 别名的裸 UNION 子查询，
     * 供 Mapper 中的 ${tableSource} 使用时由 Mapper SQL 自行提供别名（t / mx 等）。
     * 对于 getStatisticsDirect 等 RealtimeStatsSqlProvider 内嵌 AS 别名的情况，
     * 那些 SQL 是独立构建的，不依赖此方法。
     */
    private static String onlineUsersTableSource() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMM");
        String curMonth = LocalDate.now().format(fmt);
        String prevMonth = LocalDate.now().minusDays(7).format(fmt);
        if (curMonth.equals(prevMonth)) {
            return "health_record_" + curMonth;
        }
        // 168h跨月：只需当月+上月两张表
        // 不加 AS 别名 — Mapper 中每处 ${tableSource} 出现时自行指定别名（AS t / AS mx）
        String cols = "user_code,heart_rate,blood_oxygen,temperature,steps,calories," +
                      "sleep_minutes,blood_pressure_high,blood_pressure_low,pressure,record_time";
        return "(SELECT " + cols + " FROM health_record_" + prevMonth +
               " UNION ALL SELECT " + cols + " FROM health_record_" + curMonth + ")";
    }

    /**
     * 获取在线用户列表(无分页版本)
     */
    @Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
    public Map<String, Object> getOnlineUsers(int page, int size) {
        int offset = (page - 1) * size;
        String tblSrc = onlineUsersTableSource();

        List<Map<String, Object>> list = realtimeMapper.getOnlineUsersDirect(tblSrc, offset, size);
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

        // 短路计数优化：若本页返回的行数 < 请求的 size，说明已到最后一页，
        // total = offset + 实际返回数，无需再发一条 COUNT 查询（节省一次 DB 往返）。
        // 只有当本页恰好满页（可能还有下一页）时才查 COUNT。
        int total;
        if (list.size() < size) {
            total = offset + list.size();
        } else {
            total = realtimeMapper.countOnlineUsersDirect(tblSrc);
        }

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
     * 获取实时统计数据 — 使用直接查分区表版本（2月表 vs 13张全扫描，性能提升约5-10x）
     */
    @Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
    public Map<String, Object> getStatistics() {
        Map<String, Object> data = realtimeMapper.getStatisticsDirect();
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