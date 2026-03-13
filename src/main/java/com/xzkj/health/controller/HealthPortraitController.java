package com.xzkj.health.controller;

import com.xzkj.health.common.Result;
import com.xzkj.health.mapper.HealthPortraitMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/health-portrait")
public class HealthPortraitController {

    @Autowired
    private HealthPortraitMapper healthPortraitMapper;

    @GetMapping("/{empCode}")
    public Result<Map<String, Object>> getPortrait(@PathVariable String empCode) {
        try {
            Map<String, Object> employee = healthPortraitMapper.getEmployeeDetail(empCode);
            if (employee == null) {
                return Result.error("员工不存在");
            }

            Map<String, Object> vitals = healthPortraitMapper.getLatestVitals(empCode);
            Map<String, Object> exercise = healthPortraitMapper.getTodayExercise(empCode);
            List<Map<String, Object>> trendRows = healthPortraitMapper.get7DayTrend(empCode);
            List<Map<String, Object>> warnings = healthPortraitMapper.get30DayWarnings(empCode);
            String today = java.time.LocalDate.now().toString();
            List<Map<String, Object>> hourlyHrRows = healthPortraitMapper.getHourlyHeartRate(empCode, today);

            // 整理 7 天趋势为前端期望的格式
            if (trendRows == null) trendRows = Collections.emptyList();
            List<String> dates = new ArrayList<>();
            List<Object> heartRates = new ArrayList<>();
            List<Object> bloodOxygens = new ArrayList<>();
            for (Map<String, Object> row : trendRows) {
                Object dateVal = row.get("date");
                dates.add(dateVal != null ? dateVal.toString() : "");
                heartRates.add(row.get("avgHeartRate"));
                bloodOxygens.add(row.get("avgBloodOxygen"));
            }
            Map<String, Object> trend = new LinkedHashMap<>();
            trend.put("dates", dates);
            trend.put("heartRates", heartRates);
            trend.put("bloodOxygens", bloodOxygens);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("empName",     employee.get("empName"));
            data.put("empCode",     employee.get("empCode"));
            data.put("deptName",    employee.get("deptName"));
            data.put("jobTypeName", employee.get("jobTypeName"));
            data.put("gender",      employee.get("gender"));
            data.put("bloodType",   employee.get("bloodType"));
            data.put("height",      employee.get("height"));
            data.put("weight",      employee.get("weight"));
            // Build 24-element hourly HR array (null for missing hours)
            int[] hourlyHr = new int[24];
            java.util.Arrays.fill(hourlyHr, 0);
            for (Map<String, Object> row : hourlyHrRows) {
                Object h = row.get("hour"); Object v = row.get("avgHr");
                if (h != null && v != null) {
                    int hr = ((Number)h).intValue();
                    if (hr >= 0 && hr < 24) hourlyHr[hr] = ((Number)v).intValue();
                }
            }
            data.put("vitals",      vitals);
            data.put("exercise",    exercise);
            data.put("trend",       trend);
            data.put("warnings",    warnings);
            data.put("hourlyHr",    hourlyHr);

            return Result.ok("获取成功", data);
        } catch (Exception e) {
            log.error("获取健康画像失败: empCode={}", empCode, e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}
