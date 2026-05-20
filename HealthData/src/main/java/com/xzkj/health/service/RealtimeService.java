package com.xzkj.health.service;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.exception.BusinessException;
import com.xzkj.health.config.datasource.HealthCacheKeys;
import com.xzkj.health.dto.realtime.RealtimeAlertRow;
import com.xzkj.health.dto.realtime.RealtimeAlertView;
import com.xzkj.health.dto.realtime.RealtimeOverviewRow;
import com.xzkj.health.dto.realtime.RealtimeOverviewView;
import com.xzkj.health.dto.realtime.RealtimeStatisticsRow;
import com.xzkj.health.dto.realtime.RealtimeStatisticsView;
import com.xzkj.health.dto.realtime.RealtimeUserDetailRow;
import com.xzkj.health.dto.realtime.RealtimeUserDetailView;
import com.xzkj.health.dto.realtime.RealtimeUserPageView;
import com.xzkj.health.dto.realtime.RealtimeUserRow;
import com.xzkj.health.dto.realtime.RealtimeUserView;
import com.xzkj.health.mapper.RealtimeMapper;
import com.xzkj.health.util.LocalTtlCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时监控Service - 更新版
 */
@Slf4j
@Service
public class RealtimeService {

    private static final long STATISTICS_TTL = 30_000L;
    private static final long ONLINE_USERS_TTL = 10_000L;

    @Autowired
    private RealtimeMapper realtimeMapper;

    private final LocalTtlCache<RealtimeStatisticsView> statisticsCache = new LocalTtlCache<>();
    private final ConcurrentHashMap<String, CacheEntry<RealtimeUserPageView>> onlineUsersCache = new ConcurrentHashMap<>();

    /**
     * 获取当日平均数据概览
     */
    public RealtimeOverviewView getTodayAvgOverview() {
        RealtimeOverviewRow data = realtimeMapper.getTodayAvgData();
        if (data == null) data = new RealtimeOverviewRow();

        double avgTemp = doubleValue(data.getAvgTemperature());
        double avgSleep = doubleValue(data.getAvgSleep());
        return new RealtimeOverviewView(
                Math.round(doubleValue(data.getAvgHeartRate())),
                Math.round(doubleValue(data.getAvgBloodOxygen())),
                Math.round(doubleValue(data.getAvgSteps())),
                Math.round(avgTemp * 10.0) / 10.0,
                Math.round(avgSleep * 10.0) / 10.0,
                longValue(data.getTodayWarningCount())
        );
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
        String cols = "id,user_code,heart_rate,blood_oxygen,temperature,steps,calories," +
                      "sleep_minutes,blood_pressure_high,blood_pressure_low,pressure,record_time";
        return "(SELECT " + cols + " FROM health_record_" + prevMonth +
               " UNION ALL SELECT " + cols + " FROM health_record_" + curMonth + ")";
    }

    /**
     * 获取在线用户列表(无分页版本)
     */
    @Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
    public RealtimeUserPageView getOnlineUsers(Integer page, Integer size) {
        int safePage = Math.max(1, page == null ? 1 : page);
        int safeSize = Math.max(1, DateParamUtil.clampSize(size == null ? 10000 : size, 10000));

        if (safePage == 1 && safeSize >= 200) {
            String cacheKey = HealthCacheKeys.key("online-users", safePage, safeSize);
            long now = System.currentTimeMillis();
            CacheEntry<RealtimeUserPageView> cached = onlineUsersCache.get(cacheKey);
            if (cached != null && now < cached.expireAt()) {
                return cached.value();
            }
            try {
                RealtimeUserPageView data = loadOnlineUsersPage(safePage, safeSize, false);
                onlineUsersCache.put(cacheKey, new CacheEntry<>(data, System.currentTimeMillis() + ONLINE_USERS_TTL));
                return data;
            } catch (Exception ex) {
                log.warn("实时监控在线用户查询失败，尝试返回过期缓存: {}", ex.getMessage());
                if (cached != null) {
                    RealtimeUserPageView stale = cached.value();
                    return new RealtimeUserPageView(
                            stale.list(),
                            stale.total(),
                            stale.page(),
                            stale.size(),
                            true
                    );
                }
                throw new BusinessException(503, "实时监控数据加载失败，请稍后重试");
            }
        }

        return loadOnlineUsersPage(safePage, safeSize, false);
    }

    private RealtimeUserPageView loadOnlineUsersPage(int page, int size, boolean stale) {
        int offset = (page - 1) * size;
        String tblSrc = onlineUsersTableSource();

        List<RealtimeUserRow> rows = realtimeMapper.getOnlineUsersDirect(tblSrc, offset, size);
        List<RealtimeUserView> list = toRealtimeUserViews(rows);

        // 短路计数优化：若本页返回的行数 < 请求的 size，说明已到最后一页，
        // total = offset + 实际返回数，无需再发一条 COUNT 查询（节省一次 DB 往返）。
        // 只有当本页恰好满页（可能还有下一页）时才查 COUNT。
        int total;
        if (list.size() < size) {
            total = offset + list.size();
        } else {
            total = realtimeMapper.countOnlineUsersDirect(tblSrc);
        }

        return new RealtimeUserPageView(list, total, page, size, stale);
    }

    /**
     * 获取用户实时数据
     */
    public RealtimeUserDetailView getUserRealtimeData(String userCode) {
        RealtimeUserDetailRow data = realtimeMapper.getUserRealtimeData(userCode);

        if (data == null) {
            return new RealtimeUserDetailView(
                    userCode,
                    "",
                    "",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "offline"
            );
        }

        return new RealtimeUserDetailView(
                stringValue(data.getUserCode()),
                stringValue(data.getUserName()),
                stringValue(data.getDeptName()),
                intValue(data.getHeartRate()),
                intValue(data.getBloodOxygen()),
                roundNullableDouble(data.getTemperature()),
                intValue(data.getBloodPressureHigh()),
                intValue(data.getBloodPressureLow()),
                intValue(data.getPressure()),
                intValue(data.getSteps()),
                intValue(data.getCalories()),
                roundNullableDouble(data.getSleepHours()),
                stringValue(data.getLastUpdate()),
                stringValue(data.getStatus())
        );
    }

    /**
     * 获取实时统计数据 — 使用直接查分区表版本（2月表 vs 13张全扫描，性能提升约5-10x）
     */
    @Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
    public RealtimeStatisticsView getStatistics() {
        String cacheKey = HealthCacheKeys.key("statistics");
        RealtimeStatisticsView hit = statisticsCache.getIfFresh(cacheKey);
        if (hit != null) {
            return hit;
        }
        RealtimeStatisticsRow data = realtimeMapper.getStatisticsDirect();
        if (data == null) data = new RealtimeStatisticsRow();

        RealtimeStatisticsView result = new RealtimeStatisticsView(
                longValue(data.getOnlineUsers()),
                longValue(data.getTotalUsers()),
                longValue(data.getWeekRecords()),
                longValue(data.getTodayRecords()),
                Math.round(doubleValue(data.getOnlineRate())),
                Math.round(doubleValue(data.getNormalRate()))
        );
        statisticsCache.put(cacheKey, result, STATISTICS_TTL);
        return result;
    }

    /**
     * 获取近期未处理告警列表（默认取最近20条）
     */
    public List<RealtimeAlertView> getRealtimeAlerts(int limit) {
        List<RealtimeAlertRow> rows = realtimeMapper.getRecentAlerts(limit);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<RealtimeAlertView> result = new ArrayList<>();
        for (RealtimeAlertRow row : rows) {
            if (row == null) {
                continue;
            }
            Integer handled = intValue(row.getHandled());
            result.add(new RealtimeAlertView(
                    longValue(row.getId()),
                    stringValue(row.getUserCode()),
                    stringValue(row.getUserName()),
                    stringValue(row.getDeptName()),
                    stringValue(row.getWarningType()),
                    stringValue(row.getIndicatorName()),
                    stringValue(row.getIndicatorValue()),
                    stringValue(row.getWarningLevel()),
                    handled != null && handled > 0,
                    stringValue(row.getCreateTime())
            ));
        }
        return result;
    }

    private List<RealtimeUserView> toRealtimeUserViews(List<RealtimeUserRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<RealtimeUserView> result = new ArrayList<>();
        for (RealtimeUserRow user : rows) {
            if (user == null) {
                continue;
            }
            result.add(new RealtimeUserView(
                    longValue(user.getId()),
                    stringValue(user.getUserCode()),
                    stringValue(user.getUserName()),
                    intValue(user.getGender()),
                    intValue(user.getAge()),
                    stringValue(user.getDeptName()),
                    intValue(user.getHeartRate()),
                    intValue(user.getBloodOxygen()),
                    intValue(user.getSteps()),
                    intValue(user.getCalories()),
                    roundNullableDouble(user.getTemperature()),
                    roundNullableDouble(user.getSleepHours()),
                    intValue(user.getBloodPressureHigh()),
                    intValue(user.getBloodPressureLow()),
                    intValue(user.getPressure()),
                    stringValue(user.getStatus()),
                    stringValue(user.getLastUpdate()),
                    stringValue(user.getImei())
            ));
        }
        return result;
    }

    private Integer intValue(Number value) {
        return value == null ? null : value.intValue();
    }

    private long longValue(Number value) {
        return value == null ? 0L : value.longValue();
    }

    private double doubleValue(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private Double roundNullableDouble(Number value) {
        return value == null ? null : Math.round(value.doubleValue() * 10.0) / 10.0;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record CacheEntry<T>(T value, long expireAt) {}
}
