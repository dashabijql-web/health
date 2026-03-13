package com.xzkj.health.service;

import java.util.List;
import java.util.Map;

/**
 * Dashboard服务接口
 */
public interface DashboardService {

    /**
     * 获取指定时间段内的检测人数统计
     * @param startTime 开始日期（YYYY-MM-DD），null 则默认当月月初
     * @param endTime   结束日期（YYYY-MM-DD），null 则默认当月月末
     */
    Map<String, Object> getCurrentMonthCounts(String startTime, String endTime);

    /**
     * 获取指定时间段内的平均身体指标
     */
    Map<String, Object> getCurrentMonthAverage(String startTime, String endTime);

    /**
     * 获取指定时间段内预警次数最多的员工 TOP15
     */
    List<Map<String, Object>> getDeptTop5(String startTime, String endTime);

    /**
     * 获取设备激活统计（activeRate 按时间段，usageRate/warningRate 取当日）
     */
    Map<String, Object> getDeviceStats(String startTime, String endTime);

    /**
     * 获取指定时间段内的各指标预警率
     */
    Map<String, Object> getWarningRates(String startTime, String endTime);

    /**
     * 获取指定时间段内的最近预警事件
     * @param limit     返回数量
     */
    List<Map<String, Object>> getRecentWarnings(int limit, String startTime, String endTime);

    /**
     * 按部门统计指定时间段内的健康数据量
     */
    List<Map<String, Object>> getDeptHealthCounts(String startTime, String endTime);

    /**
     * 获取过去 N 天每日各指标异常率
     */
    List<Map<String, Object>> getDailyAnomalyRates(int days);

    /**
     * 按日/时统计预警数量（柱状图专用，不受 TOP 10000 限制）
     * groupBy: "day" 或 "hour"（hour 仅对当日有效）
     */
    Map<String, Object> getWarningDistribution(String startTime, String endTime, String groupBy);

    /**
     * 获取过去 N 天每日体征均值（心率/血氧/步数），用于健康趋势图
     */
    Map<String, Object> getDailyHealthTrend(int days);

    /**
     * 获取部门健康排名（基于真实预警率），默认当月
     */
    List<Map<String, Object>> getDeptRanking(String startTime, String endTime);

    /**
     * 获取指定日期范围内每日体征均值（心率/血氧/步数），用于日历展示
     */
    List<Map<String, Object>> getDailyHealthTrendByRange(String startDate, String endDate);

    /**
     * 获取指定日期范围内每日预警数量，用于日历展示
     */
    List<Map<String, Object>> getWarningCountsByDate(String startDate, String endDate);

    /** 指定日期心率排行（偏差最大排前） */
    List<Map<String, Object>> getDayHeartRateRank(String date);

    /** 指定日期血氧排行（最低排前） */
    List<Map<String, Object>> getDayBloodOxygenRank(String date);

    /** 指定日期步数排行（最少排前） */
    List<Map<String, Object>> getDayStepsRank(String date);

    /** 指定日期预警列表（最严重排前） */
    List<Map<String, Object>> getDayWarnings(String date);
}
