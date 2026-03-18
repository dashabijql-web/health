package com.xzkj.health.service;

import com.xzkj.health.mapper.StatisticsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StatisticsService {

    @Autowired
    private StatisticsMapper statisticsMapper;

    public List<Map<String, Object>> getDeptHealthSummary() {
        return statisticsMapper.getDeptHealthSummary();
    }

    public List<Map<String, Object>> getMonthlySummary(String month) {
        String[] range = monthToRange(month);
        String tableName = "health_record_" + month.replace("-", "");
        String warningTableName = "warning_record_" + month.replace("-", "");
        try {
            return statisticsMapper.getMonthlySummary(tableName, warningTableName, range[0], range[1]);
        } catch (Exception e) {
            log.warn("月度汇总查询失败(表 {} 可能不存在): {}", tableName, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getDailyRecordCounts(String month) {
        String[] range = monthToRange(month);
        String tableName = "health_record_" + month.replace("-", "");
        try {
            return statisticsMapper.getDailyRecordCounts(tableName, range[0], range[1]);
        } catch (Exception e) {
            log.warn("每日记录数查询失败(表 {} 可能不存在): {}", tableName, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getWarningTypeCounts(String month) {
        String[] range = monthToRange(month);
        String warningTableName = "warning_record_" + month.replace("-", "");
        try {
            return statisticsMapper.getWarningTypeCountsDirect(warningTableName, range[0], range[1]);
        } catch (Exception e) {
            log.warn("预警类型统计查询失败(表 {} 可能不存在), fallback 到视图: {}", warningTableName, e.getMessage());
            return statisticsMapper.getWarningTypeCounts(range[0], range[1]);
        }
    }

    /** 将 "YYYY-MM" 转为 [startDate, endDate)，如 ["2026-03-01", "2026-04-01"] */
    private String[] monthToRange(String month) {
        YearMonth ym = YearMonth.parse(month);
        return new String[]{ ym.atDay(1).toString(), ym.plusMonths(1).atDay(1).toString() };
    }
}
