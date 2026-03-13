package com.xzkj.health.service;

import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.RiskWarningMapper;
import com.xzkj.health.util.TableNameUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 风险预警Service
 *
 * 分表适配（v2）：
 *   - 读取：Mapper 已改为查 v_warning_record 视图，此层无需改动
 *   - 处理单条/批量预警：
 *     先从视图查出 create_time，计算对应月份表，再更新该月份表
 *   - 插入新预警：insertWarning() 写入当前月份表
 */
@Slf4j
@Service
public class RiskWarningService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private RiskWarningMapper riskWarningMapper;

    // ─── 读取 ─────────────────────────────────────────────────────────

    public Map<String, Object> getWarningStats(String startDate, String endDate) {
        Map<String, Object> stats = riskWarningMapper.getWarningOverview(startDate, endDate);
        if (!stats.containsKey("handledRate")) {
            long total = MapValueUtil.getLong(stats, "totalWarnings");
            long handled = MapValueUtil.getLong(stats, "handledWarnings");
            stats.put("handledRate", total > 0 ? (int) (handled * 100 / total) : 0);
        }
        return stats;
    }

    public Map<String, Object> getWarningList(String level, Boolean handled, String userCode, int page, int size) {
        int offset = (page - 1) * size;
        List<Map<String, Object>> list = riskWarningMapper.getWarningList(level, handled, userCode, offset, size);
        int total = riskWarningMapper.countWarnings(level, handled, userCode);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public Map<String, Object> getWarningTrend(int days) {
        List<Map<String, Object>> trendData = riskWarningMapper.getWarningTrendByType(days);

        List<String> dates = new ArrayList<>();
        List<Integer> heartRateData = new ArrayList<>();
        List<Integer> bloodOxygenData = new ArrayList<>();
        List<Integer> sleepData = new ArrayList<>();
        List<Integer> temperatureData = new ArrayList<>();
        List<Integer> pressureData = new ArrayList<>();

        for (Map<String, Object> item : trendData) {
            String date = (String) item.get("date");
            dates.add(date != null && date.length() >= 5 ? date.substring(5) : "");
            heartRateData.add(MapValueUtil.getInt(item, "heartRate"));
            bloodOxygenData.add(MapValueUtil.getInt(item, "bloodOxygen"));
            sleepData.add(MapValueUtil.getInt(item, "sleep"));
            temperatureData.add(MapValueUtil.getInt(item, "temperature"));
            pressureData.add(MapValueUtil.getInt(item, "pressure"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        Map<String, List<Integer>> series = new HashMap<>();
        series.put("heartRate", heartRateData);
        series.put("bloodOxygen", bloodOxygenData);
        series.put("sleep", sleepData);
        series.put("temperature", temperatureData);
        series.put("pressure", pressureData);
        result.put("series", series);
        return result;
    }

    public List<Map<String, Object>> getDeptWarningStats(String startDate, String endDate) {
        return riskWarningMapper.getDeptWarningStats(startDate, endDate);
    }

    public List<Map<String, Object>> getTypeDistribution() {
        return riskWarningMapper.getTypeDistribution();
    }

    // ─── 写入：新预警记录 ─────────────────────────────────────────────

    /**
     * 检查指定用户在最近 minutes 分钟内是否有同指标的未处理预警。
     * 用于去重，避免同一用户持续异常时每5秒插入一条相同预警。
     * 查询失败时返回 false（fail-open：宁可多插也不丢失告警）。
     */
    public boolean hasRecentWarning(String userCode, String indicatorName, int minutes) {
        try {
            return riskWarningMapper.countRecentWarning(userCode, indicatorName, minutes) > 0;
        } catch (Exception e) {
            log.warn("检查近期预警失败，允许插入: userCode={}, indicator={}, error={}",
                    userCode, indicatorName, e.getMessage());
            return false;
        }
    }

    /**
     * 向当前月份表插入一条新预警记录。
     * 由 DataProcessService 或其他业务逻辑触发时调用。
     */
    public boolean insertWarning(String userCode, String warningType,
                                 String indicatorName, String indicatorValue, String warningLevel) {
        String tableName = TableNameUtil.warningRecordTable();
        try {
            int rows = riskWarningMapper.insertToWarningTable(
                    tableName, userCode, warningType, indicatorName, indicatorValue, warningLevel);
            return rows > 0;
        } catch (Exception e) {
            log.error("[分表] 插入预警到 {} 失败: {}", tableName, e.getMessage(), e);
            return false;
        }
    }

    // ─── 更新：处理预警（需按月份路由） ──────────────────────────────

    /**
     * 处理单条预警。
     *
     * 路由逻辑：
     *   1. 先从 v_warning_record 视图查出该记录的 create_time
     *   2. 用 TableNameUtil 计算对应月份表名
     *   3. 更新该月份表
     */
    public boolean handleWarning(Long id, String handleBy, String handleRemark) {
        String tableName = resolveWarningTable(id);
        if (tableName == null) {
            log.warn("[分表] handleWarning: 未找到 id={} 的预警记录", id);
            return false;
        }
        // 先尝试更新月份表
        int result = riskWarningMapper.handleWarningInTable(tableName, id, handleBy, handleRemark);
        // 月份表没有该行（分表前的历史数据在原始表里），回退更新原始表
        if (result == 0) {
            result = riskWarningMapper.handleWarningOriginal(id, handleBy, handleRemark);
        }
        return result > 0;
    }

    /**
     * 批量处理预警。
     *
     * 路由逻辑：
     *   1. 从视图查出所有 id 对应的 create_time
     *   2. 按月份表分组
     *   3. 对每个月份表执行一次批量 UPDATE
     */
    public boolean handleBatch(List<Long> ids, String handleBy) {
        if (ids == null || ids.isEmpty()) return false;

        List<Map<String, Object>> records = riskWarningMapper.selectCreateTimesByIds(ids);
        if (records.isEmpty()) {
            log.warn("[分表] handleBatch: 未找到任何预警记录，ids={}", ids);
            return false;
        }

        // 按月份表分组
        Map<String, List<Long>> tableToIds = new HashMap<>();
        for (Map<String, Object> rec : records) {
            Long recId = MapValueUtil.toLong(rec.get("id"));
            if (recId == null) continue;
            LocalDateTime createTime = parseDateTime(rec.get("create_time"));
            if (createTime == null) continue;
            String tableName = TableNameUtil.warningRecordTable(createTime);
            tableToIds.computeIfAbsent(tableName, k -> new ArrayList<>()).add(recId);
        }

        int totalUpdated = 0;
        // 收集月份表里找不到的 id，最后在原始表里兜底更新
        List<Long> notFoundIds = new ArrayList<>(ids);

        for (Map.Entry<String, List<Long>> entry : tableToIds.entrySet()) {
            try {
                int rows = riskWarningMapper.handleBatchInTable(entry.getKey(), entry.getValue(), handleBy);
                totalUpdated += rows;
                if (rows == entry.getValue().size()) {
                    notFoundIds.removeAll(entry.getValue());
                }
            } catch (Exception e) {
                log.error("[分表] handleBatch 更新 {} 失败: {}", entry.getKey(), e.getMessage(), e);
            }
        }

        // 回退：在原始 warning_record 表里更新分表前的历史数据
        if (!notFoundIds.isEmpty()) {
            try {
                int rows = riskWarningMapper.handleBatchOriginal(notFoundIds, handleBy);
                totalUpdated += rows;
            } catch (Exception e) {
                log.error("[分表] handleBatch 原始表回退失败: {}", e.getMessage(), e);
            }
        }

        return totalUpdated > 0;
    }

    // ─── 私有工具 ────────────────────────────────────────────────────

    /**
     * 根据 id 从视图查出 create_time，并计算对应月份表名。
     * 找不到时返回 null。
     */
    private String resolveWarningTable(Long id) {
        Map<String, Object> info = riskWarningMapper.selectCreateTimeById(id);
        if (info == null) return null;
        LocalDateTime createTime = parseDateTime(info.get("create_time"));
        if (createTime == null) return null;
        return TableNameUtil.warningRecordTable(createTime);
    }

    /** 将数据库返回的时间对象（可能是 Timestamp/String）解析为 LocalDateTime */
    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        }
        if (value instanceof String) {
            try {
                return LocalDateTime.parse((String) value, DT_FMT);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

}
