package com.xzkj.health.service.impl;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.mapper.PressureMapper;
import com.xzkj.health.service.PressureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class PressureServiceImpl implements PressureService {

    @Autowired
    private PressureMapper pressureMapper;

    @Override
    public Map<String, Object> getOverview(String startDate, String endDate) {
        try {
            return pressureMapper.getOverview(startDate, endDate);
        } catch (Exception e) {
            log.error("获取压力概览失败", e);
            throw new RuntimeException("获取压力概览失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getTrend(int days) {
        try {
            List<Map<String, Object>> rows = pressureMapper.getTrend(days);
            List<String> dates = new ArrayList<>();
            List<Integer> values = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                String date = (String) row.get("date");
                dates.add(DateParamUtil.shortDate(date));
                Number v = (Number) row.get("avgPressure");
                values.add(v != null ? v.intValue() : 0);
            }
            Map<String, Object> result = new HashMap<>();
            result.put("dates", dates);
            result.put("values", values);
            return result;
        } catch (Exception e) {
            log.error("获取压力趋势失败", e);
            throw new RuntimeException("获取压力趋势失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getDistribution(String startDate, String endDate) {
        try {
            return pressureMapper.getDistribution(startDate, endDate);
        } catch (Exception e) {
            log.error("获取压力分布失败", e);
            throw new RuntimeException("获取压力分布失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getTopUsers(int limit, String startDate, String endDate) {
        try {
            return pressureMapper.getTopUsers(limit, startDate, endDate);
        } catch (Exception e) {
            log.error("获取TOP压力用户失败", e);
            throw new RuntimeException("获取TOP压力用户失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getDepartmentStats(String startDate, String endDate) {
        try {
            return pressureMapper.getDepartmentStats(startDate, endDate);
        } catch (Exception e) {
            log.error("获取部门压力统计失败", e);
            throw new RuntimeException("获取部门压力统计失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getRealtime(int limit) {
        try { return pressureMapper.getRealtime(limit); }
        catch (Exception e) { log.error("获取实时压力失败", e); return Collections.emptyList(); }
    }

    @Override
    public List<Map<String, Object>> getHourlyStats(String date) {
        try { return pressureMapper.getHourlyStats(date); }
        catch (Exception e) { log.error("获取压力小时统计失败", e); return Collections.emptyList(); }
    }

    @Override
    public Map<String, Object> getAbnormalRecords(int page, int size) {
        try {
            int offset = (page - 1) * size;
            List<Map<String, Object>> list = pressureMapper.getAbnormalRecords(offset, size);
            int total = pressureMapper.countAbnormalRecords();
            Map<String, Object> result = new HashMap<>();
            result.put("list", list);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            return result;
        } catch (Exception e) {
            log.error("获取异常压力记录失败", e);
            throw new RuntimeException("获取异常压力记录失败: " + e.getMessage());
        }
    }
}
