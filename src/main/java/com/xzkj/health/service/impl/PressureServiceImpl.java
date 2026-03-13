package com.xzkj.health.service.impl;

import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.PressureMapper;
import com.xzkj.health.service.PressureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 压力监测服务实现
 * 异常由 GlobalExceptionHandler 统一处理
 */
@Service
public class PressureServiceImpl implements PressureService {

    @Autowired
    private PressureMapper pressureMapper;

    @Override
    public Map<String, Object> getOverview(String startDate, String endDate) {
        return pressureMapper.getOverview(startDate, endDate);
    }

    @Override
    public Map<String, Object> getTrend(int days) {
        return MapValueUtil.convertTrendData(pressureMapper.getTrend(days), "avgPressure");
    }

    @Override
    public List<Map<String, Object>> getDistribution(String startDate, String endDate) {
        return pressureMapper.getDistribution(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getTopUsers(int limit, String startDate, String endDate) {
        return pressureMapper.getTopUsers(limit, startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getDepartmentStats(String startDate, String endDate) {
        return pressureMapper.getDepartmentStats(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getRealtime(int limit) {
        return MapValueUtil.orEmpty(pressureMapper.getRealtime(limit));
    }

    @Override
    public List<Map<String, Object>> getHourlyStats(String date) {
        return MapValueUtil.orEmpty(pressureMapper.getHourlyStats(date));
    }

    @Override
    public Map<String, Object> getAbnormalRecords(int page, int size) {
        int offset = (page - 1) * size;
        return MapValueUtil.buildPageResult(
                pressureMapper.getAbnormalRecords(offset, size),
                pressureMapper.countAbnormalRecords(), page, size);
    }
}
