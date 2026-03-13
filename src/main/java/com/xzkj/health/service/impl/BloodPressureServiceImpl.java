package com.xzkj.health.service.impl;

import com.xzkj.health.common.DateParamUtil;
import com.xzkj.health.mapper.BloodPressureMapper;
import com.xzkj.health.service.BloodPressureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class BloodPressureServiceImpl implements BloodPressureService {

    @Autowired
    private BloodPressureMapper bloodPressureMapper;

    @Override
    public Map<String, Object> getOverview(String startDate, String endDate) {
        try {
            return bloodPressureMapper.getOverview(startDate, endDate);
        } catch (Exception e) {
            log.error("获取血压概览失败", e);
            throw new RuntimeException("获取血压概览失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getTrend(int days) {
        try {
            List<Map<String, Object>> rows = bloodPressureMapper.getTrend(days);

            List<String> dates = new ArrayList<>();
            List<Integer> systolicValues = new ArrayList<>();
            List<Integer> diastolicValues = new ArrayList<>();

            for (Map<String, Object> row : rows) {
                String date = (String) row.get("date");
                dates.add(DateParamUtil.shortDate(date));
                Number sys = (Number) row.get("avgSystolic");
                Number dia = (Number) row.get("avgDiastolic");
                systolicValues.add(sys != null ? sys.intValue() : 0);
                diastolicValues.add(dia != null ? dia.intValue() : 0);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("dates", dates);
            result.put("systolicValues", systolicValues);
            result.put("diastolicValues", diastolicValues);
            return result;
        } catch (Exception e) {
            log.error("获取血压趋势失败", e);
            throw new RuntimeException("获取血压趋势失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getDistribution(String startDate, String endDate) {
        try {
            return bloodPressureMapper.getDistribution(startDate, endDate);
        } catch (Exception e) {
            log.error("获取血压分布失败", e);
            throw new RuntimeException("获取血压分布失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getTopUsers(int limit, String startDate, String endDate) {
        try {
            return bloodPressureMapper.getTopUsers(limit, startDate, endDate);
        } catch (Exception e) {
            log.error("获取TOP血压用户失败", e);
            throw new RuntimeException("获取TOP血压用户失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getDepartmentStats(String startDate, String endDate) {
        try {
            return bloodPressureMapper.getDepartmentStats(startDate, endDate);
        } catch (Exception e) {
            log.error("获取部门血压统计失败", e);
            throw new RuntimeException("获取部门血压统计失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getRealtime(int limit) {
        try { return bloodPressureMapper.getRealtime(limit); }
        catch (Exception e) { log.error("获取实时血压失败", e); return Collections.emptyList(); }
    }

    @Override
    public List<Map<String, Object>> getHourlyStats(String date) {
        try { return bloodPressureMapper.getHourlyStats(date); }
        catch (Exception e) { log.error("获取血压小时统计失败", e); return Collections.emptyList(); }
    }

    @Override
    public Map<String, Object> getAbnormalRecords(int page, int size) {
        try {
            int offset = (page - 1) * size;
            List<Map<String, Object>> list = bloodPressureMapper.getAbnormalRecords(offset, size);
            int total = bloodPressureMapper.countAbnormalRecords();

            Map<String, Object> result = new HashMap<>();
            result.put("list", list);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            return result;
        } catch (Exception e) {
            log.error("获取异常血压记录失败", e);
            throw new RuntimeException("获取异常血压记录失败: " + e.getMessage());
        }
    }
}
