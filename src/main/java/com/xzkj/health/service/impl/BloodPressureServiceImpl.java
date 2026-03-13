package com.xzkj.health.service.impl;

import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.BloodPressureMapper;
import com.xzkj.health.service.BloodPressureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 血压监测服务实现
 * 异常由 GlobalExceptionHandler 统一处理
 */
@Service
public class BloodPressureServiceImpl implements BloodPressureService {

    @Autowired
    private BloodPressureMapper bloodPressureMapper;

    @Override
    public Map<String, Object> getOverview(String startDate, String endDate) {
        return bloodPressureMapper.getOverview(startDate, endDate);
    }

    @Override
    public Map<String, Object> getTrend(int days) {
        return MapValueUtil.convertDualTrendData(bloodPressureMapper.getTrend(days),
                "avgSystolic", "systolicValues", "avgDiastolic", "diastolicValues");
    }

    @Override
    public List<Map<String, Object>> getDistribution(String startDate, String endDate) {
        return bloodPressureMapper.getDistribution(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getTopUsers(int limit, String startDate, String endDate) {
        return bloodPressureMapper.getTopUsers(limit, startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getDepartmentStats(String startDate, String endDate) {
        return bloodPressureMapper.getDepartmentStats(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getRealtime(int limit) {
        return MapValueUtil.orEmpty(bloodPressureMapper.getRealtime(limit));
    }

    @Override
    public List<Map<String, Object>> getHourlyStats(String date) {
        return MapValueUtil.orEmpty(bloodPressureMapper.getHourlyStats(date));
    }

    @Override
    public Map<String, Object> getAbnormalRecords(int page, int size) {
        int offset = (page - 1) * size;
        return MapValueUtil.buildPageResult(
                bloodPressureMapper.getAbnormalRecords(offset, size),
                bloodPressureMapper.countAbnormalRecords(), page, size);
    }
}
