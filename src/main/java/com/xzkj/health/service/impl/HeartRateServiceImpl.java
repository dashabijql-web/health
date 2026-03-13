package com.xzkj.health.service.impl;

import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.HeartRateMapper;
import com.xzkj.health.service.HeartRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 心率监测服务实现
 * 异常由 GlobalExceptionHandler 统一处理，Service 层无需 try-catch-rethrow
 */
@Service
public class HeartRateServiceImpl implements HeartRateService {

    @Autowired
    private HeartRateMapper heartRateMapper;

    @Override
    public Map<String, Object> getHeartRateOverview(String startDate, String endDate) {
        return heartRateMapper.getHeartRateOverview(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getTopUsers(int limit, String startDate, String endDate) {
        return heartRateMapper.getTopUsers(limit, startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getAgeDistribution() {
        return heartRateMapper.getAgeDistribution();
    }

    @Override
    public List<Map<String, Object>> getHeartRateDistribution(String startDate, String endDate) {
        return heartRateMapper.getHeartRateDistributionNew(startDate, endDate);
    }

    @Override
    public Map<String, Object> getHeartRateTrend(int days) {
        return MapValueUtil.convertTrendData(heartRateMapper.getHeartRateTrend(days), "avgHeartRate");
    }

    @Override
    public List<Map<String, Object>> getDepartmentStats(String startDate, String endDate) {
        return heartRateMapper.getDepartmentStats(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getHourlyStats(String startDate, String endDate) {
        return MapValueUtil.orEmpty(heartRateMapper.getHourlyStats(startDate, endDate));
    }
}
