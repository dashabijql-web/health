package com.xzkj.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzkj.health.common.Result;
import com.xzkj.health.model.HealthRecord;
import com.xzkj.health.service.HealthRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health/record")
public class HealthRecordController {

    @Autowired
    private HealthRecordService healthRecordService;

    /**
     * 获取所有健康记录
     */
    @GetMapping("/list")
    public Result<List<HealthRecord>> getAllRecords() {
        List<HealthRecord> list = healthRecordService.list();
        return Result.ok("查询成功", list);
    }

    /**
     * 分页查询（支持 userCode / startTime / endTime / pageSize 过滤）
     * pageSize 是 size 的别名，兼容前端旧参数名
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> getPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String userCode,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        int effectiveSize = (pageSize != null) ? pageSize : size;
        Page<HealthRecord> page = new Page<>(current, effectiveSize);
        IPage<HealthRecord> pageResult = healthRecordService.getPageFiltered(page, userCode, startTime, endTime);

        Map<String, Object> data = new HashMap<>();
        data.put("records", pageResult.getRecords());
        data.put("total",   pageResult.getTotal());
        data.put("current", pageResult.getCurrent());
        data.put("size",    pageResult.getSize());
        return Result.ok("查询成功", data);
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result<HealthRecord> getById(@PathVariable Long id) {
        HealthRecord record = healthRecordService.getById(id);
        if (record != null) {
            return Result.ok("查询成功", record);
        }
        return Result.error(404, "记录不存在");
    }

    /**
     * 根据用户代码查询
     */
    @GetMapping("/user/{userCode}")
    public Result<List<HealthRecord>> getByUserCode(@PathVariable String userCode) {
        List<HealthRecord> records = healthRecordService.getByUserCode(userCode);
        return Result.ok("查询成功", records);
    }

    /**
     * 获取用户最新记录
     */
    @GetMapping("/user/{userCode}/latest")
    public Result<HealthRecord> getLatestByUserCode(@PathVariable String userCode) {
        HealthRecord record = healthRecordService.getLatestByUserCode(userCode);
        if (record != null) {
            return Result.ok("查询成功", record);
        }
        return Result.error(404, "该用户暂无健康记录");
    }

    /**
     * 获取用户健康统计
     */
    @GetMapping("/user/{userCode}/statistics")
    public Result<Map<String, Object>> getStatistics(@PathVariable String userCode) {
        Map<String, Object> statistics = healthRecordService.getHealthStatistics(userCode);
        if (!statistics.isEmpty()) {
            return Result.ok("查询成功", statistics);
        }
        return Result.error(404, "该用户暂无健康记录");
    }

    /**
     * 查询异常心率记录
     */
    @GetMapping("/abnormal/heart")
    public Result<List<HealthRecord>> getAbnormalHeartRate() {
        return Result.ok("查询成功", healthRecordService.getAbnormalHeartRate());
    }

    /**
     * 按时间范围查询
     */
    @GetMapping("/time-range")
    public Result<List<HealthRecord>> getByTimeRange(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return Result.ok("查询成功", healthRecordService.getByTimeRange(startTime, endTime));
    }

    /**
     * 添加健康记录
     */
    @PostMapping("/add")
    public Result<HealthRecord> addRecord(@RequestBody HealthRecord record) {
        boolean success = healthRecordService.save(record);
        if (success) {
            return Result.ok("添加成功", record);
        }
        return Result.error("添加失败");
    }

    /**
     * 批量添加健康记录
     */
    @PostMapping("/batch-add")
    public Result<Map<String, Object>> batchAddRecord(@RequestBody List<HealthRecord> records) {
        boolean success = healthRecordService.batchInsert(records);
        if (success) {
            Map<String, Object> data = new HashMap<>();
            data.put("count", records.size());
            return Result.ok("批量添加成功", data);
        }
        return Result.error("批量添加失败");
    }

    /**
     * 更新健康记录
     */
    @PutMapping("/update")
    public Result<HealthRecord> updateRecord(@RequestBody HealthRecord record) {
        boolean success = healthRecordService.updateById(record);
        if (success) {
            return Result.ok("更新成功", record);
        }
        return Result.error("更新失败");
    }

    /**
     * 删除健康记录
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        boolean success = healthRecordService.removeById(id);
        if (success) {
            return Result.ok("删除成功", null);
        }
        return Result.error("删除失败");
    }

    /**
     * 统计用户记录数量
     */
    @GetMapping("/count-by-user")
    public Result<List<Map<String, Object>>> countByUser() {
        return Result.ok("查询成功", healthRecordService.countByUser());
    }

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> data = new HashMap<>();
        data.put("status",      "UP");
        data.put("recordCount", healthRecordService.count());
        data.put("timestamp",   System.currentTimeMillis());
        return Result.ok("系统运行正常", data);
    }
}
