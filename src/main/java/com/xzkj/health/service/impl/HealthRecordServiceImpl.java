package com.xzkj.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzkj.health.mapper.HealthRecordMapper;
import com.xzkj.health.model.HealthRecord;
import com.xzkj.health.service.HealthRecordService;
import com.xzkj.health.util.TableNameUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 健康记录 Service 实现
 *
 * 分表路由策略：
 *   - save()        → 写入当前月份表 health_record_YYYYMM（由 TableNameUtil 计算）
 *   - batchInsert() → 写入当前月份表（按记录时间路由，若记录无时间则写当前月）
 *   - 所有读操作    → 通过视图 v_health_record（SQL 已在 HealthRecordMapper 中改为查视图）
 */
@Service
@Slf4j
public class HealthRecordServiceImpl extends ServiceImpl<HealthRecordMapper, HealthRecord>
        implements HealthRecordService {

    /**
     * 覆盖 IService.save()：将记录路由到对应月份表。
     * DataProcessService 的所有 save(record) 调用都会走这里。
     */
    @Override
    public boolean save(HealthRecord record) {
        String tableName = TableNameUtil.healthRecordTable();
        try {
            int rows = baseMapper.insertToTable(tableName, record);
            return rows > 0;
        } catch (Exception e) {
            log.error("[分表] 写入 {} 失败: {}", tableName, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<HealthRecord> getByUserCode(String userCode) {
        return baseMapper.selectByUserCode(userCode);
    }

    @Override
    public HealthRecord getLatestByUserCode(String userCode) {
        return baseMapper.selectLatestByUserCode(userCode);
    }

    @Override
    public List<HealthRecord> getAbnormalHeartRate() {
        return baseMapper.selectAbnormalHeartRate();
    }

    @Override
    public List<HealthRecord> getByTimeRange(String startTime, String endTime) {
        return baseMapper.selectByTimeRange(startTime, endTime);
    }

    @Override
    public Page<HealthRecord> getPage(Page<HealthRecord> page) {
        QueryWrapper<HealthRecord> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("record_time");
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<HealthRecord> getPageFiltered(Page<HealthRecord> page,
                                               String userCode,
                                               String startTime,
                                               String endTime) {
        QueryWrapper<HealthRecord> wrapper = new QueryWrapper<>();
        if (userCode != null && !userCode.isBlank()) {
            wrapper.eq("user_code", userCode);
        }
        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge("record_time", startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            // 加上时分秒，覆盖当天全天
            wrapper.le("record_time", endTime.length() == 10 ? endTime + " 23:59:59" : endTime);
        }
        wrapper.orderByDesc("record_time");
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public List<Map<String, Object>> countByUser() {
        return baseMapper.countByUser();
    }

    @Override
    public Map<String, Object> getHealthStatistics(String userCode) {
        List<HealthRecord> records = getByUserCode(userCode);

        if (records.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("userCode", userCode);
        stats.put("totalRecords", records.size());

        double avgHeartRate = records.stream()
                .filter(r -> r.getHeartRate() != null)
                .mapToInt(HealthRecord::getHeartRate)
                .average()
                .orElse(0.0);

        double avgBloodOxygen = records.stream()
                .filter(r -> r.getBloodOxygen() != null)
                .mapToInt(HealthRecord::getBloodOxygen)
                .average()
                .orElse(0.0);

        int totalSteps = records.stream()
                .filter(r -> r.getSteps() != null)
                .mapToInt(HealthRecord::getSteps)
                .sum();

        stats.put("avgHeartRate", String.format("%.2f", avgHeartRate));
        stats.put("avgBloodOxygen", String.format("%.2f", avgBloodOxygen));
        stats.put("totalSteps", totalSteps);
        stats.put("latestRecord", records.get(0));

        return stats;
    }

    /**
     * 批量插入：按每条记录的 time 字段路由到对应月份表。
     * 若 time 为空，则使用当前月份表。
     */
    @Override
    public boolean batchInsert(List<HealthRecord> records) {
        if (records == null || records.isEmpty()) {
            return false;
        }
        int successCount = 0;
        for (HealthRecord record : records) {
            String tableName = TableNameUtil.healthRecordTable();
            try {
                int rows = baseMapper.insertToTable(tableName, record);
                if (rows > 0) successCount++;
            } catch (Exception e) {
                log.error("[分表] 批量插入 {} 失败: {}", tableName, e.getMessage(), e);
            }
        }
        return successCount > 0;
    }
}
