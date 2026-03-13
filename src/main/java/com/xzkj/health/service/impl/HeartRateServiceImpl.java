package com.xzkj.health.service.impl;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.HeartRateMapper;
import com.xzkj.health.service.HeartRateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 心率监测服务实现 - 增强版
 */
@Slf4j
@Service
public class HeartRateServiceImpl implements HeartRateService {

    @Autowired
    private HeartRateMapper heartRateMapper;

    @Override
    public Map<String, Object> getHeartRateOverview(String startDate, String endDate) {
        try {
            return heartRateMapper.getHeartRateOverview(startDate, endDate);
        } catch (Exception e) {
            log.error("获取心率概览失败", e);
            throw new RuntimeException("获取心率概览失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getTopUsers(int limit, String startDate, String endDate) {
        try {
            return heartRateMapper.getTopUsers(limit, startDate, endDate);
        } catch (Exception e) {
            log.error("获取TOP用户失败", e);
            throw new RuntimeException("获取TOP用户失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getAgeDistribution() {
        try {
            return heartRateMapper.getAgeDistribution();
        } catch (Exception e) {
            log.error("获取年龄段统计失败", e);
            throw new RuntimeException("获取年龄段统计失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getHeartRateDistribution(String startDate, String endDate) {
        try {
            return heartRateMapper.getHeartRateDistributionNew(startDate, endDate);
        } catch (Exception e) {
            log.error("获取心率分布失败", e);
            throw new RuntimeException("获取心率分布失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getHeartRateTrend(int days) {
        try {
            List<Map<String, Object>> rows = heartRateMapper.getHeartRateTrend(days);
            return MapValueUtil.convertTrendData(rows, "avgHeartRate");
        } catch (Exception e) {
            log.error("获取心率趋势失败", e);
            throw new RuntimeException("获取心率趋势失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getRealtimeData(int limit) {
        try {
            return heartRateMapper.getRealtimeData(limit);
        } catch (Exception e) {
            log.error("获取实时数据失败", e);
            throw new RuntimeException("获取实时数据失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getDepartmentStats(String startDate, String endDate) {
        try {
            return heartRateMapper.getDepartmentStats(startDate, endDate);
        } catch (Exception e) {
            log.error("获取部门统计失败", e);
            throw new RuntimeException("获取部门统计失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getAbnormalRecords(int page, int size) {
        try {
            int offset = (page - 1) * size;
            List<Map<String, Object>> list = heartRateMapper.getAbnormalRecords(offset, size);
            int total = heartRateMapper.countAbnormalRecords();
            return MapValueUtil.buildPageResult(list, total, page, size);
        } catch (Exception e) {
            log.error("获取异常心率记录失败", e);
            throw new RuntimeException("获取异常心率记录失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getHourlyStats(String startDate, String endDate) {
        try {
            List<Map<String, Object>> data = heartRateMapper.getHourlyStats(startDate, endDate);
            return data != null ? data : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取心率逐小时数据失败", e);
            throw new RuntimeException("获取心率逐小时数据失败: " + e.getMessage());
        }
    }
}