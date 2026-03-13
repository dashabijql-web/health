package com.xzkj.health.service;

import com.xzkj.health.mapper.SleepMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 睡眠监测Service
 */
@Slf4j
@Service
public class SleepService {

    @Autowired
    private SleepMapper sleepMapper;

    /**
     * 获取睡眠统计概览
     */
    public Map<String, Object> getSleepStats() {
        Map<String, Object> stats = sleepMapper.getSleepStats();
        if (stats == null) stats = new HashMap<>();

        // 计算良好睡眠率
        long totalCount = getLongValue(stats, "totalCount");
        long goodSleepCount = getLongValue(stats, "goodSleepCount");

        int goodSleepRate = totalCount > 0 ? (int) (goodSleepCount * 100 / totalCount) : 0;
        stats.put("goodSleepRate", goodSleepRate);

        return stats;
    }

    /**
     * 获取睡眠趋势数据
     */
    public Map<String, Object> getSleepTrend(int days) {
        List<Map<String, Object>> trendData = sleepMapper.getSleepTrend(days);

        List<String> dates = new ArrayList<>();
        List<Double> avgData = new ArrayList<>();
        List<Double> deepSleep = new ArrayList<>();
        List<Double> lightSleep = new ArrayList<>();

        for (Map<String, Object> item : trendData) {
            String date = (String) item.get("date");
            dates.add(date.substring(5)); // 只保留月-日
            avgData.add(getDoubleValue(item, "avgSleepHours"));
            deepSleep.add(getDoubleValue(item, "avgDeepSleep"));
            lightSleep.add(getDoubleValue(item, "avgLightSleep"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("avgData", avgData);
        result.put("deepSleep", deepSleep);
        result.put("lightSleep", lightSleep);

        return result;
    }

    /**
     * 获取睡眠质量分布
     */
    public Map<String, Object> getQualityDistribution() {
        List<Map<String, Object>> distribution = sleepMapper.getSleepQualityDistribution();

        Map<String, Object> result = new HashMap<>();
        for (Map<String, Object> item : distribution) {
            String quality = (String) item.get("quality");
            int count = getIntValue(item, "count");
            result.put(quality, count);
        }

        return result;
    }

    /**
     * 获取睡眠不足记录
     */
    public Map<String, Object> getInsufficientRecords(int page, int size) {
        int offset = (page - 1) * size;

        List<Map<String, Object>> list = sleepMapper.getInsufficientRecords(offset, size);
        int total = sleepMapper.countInsufficientRecords();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return result;
    }

    /**
     * 获取睡眠详细记录
     */
    public List<Map<String, Object>> getSleepRecords(String startDate, String endDate) {
        return sleepMapper.getSleepRecords(startDate, endDate);
    }

    /**
     * 获取睡眠页面完整数据
     */
    public Map<String, Object> getSleepPageData() {
        Map<String, Object> result = new HashMap<>();

        // 1. 获取上传率和达标率
        Map<String, Object> uploadRateData = sleepMapper.getYesterdayUploadRate();
        Map<String, Object> greenLineData = sleepMapper.getGreenLineRate();
        Map<String, Object> avgData = sleepMapper.getAverageSleepData();

        Map<String, Object> overview = new HashMap<>();
        overview.put("uploadRate", Math.round(getDoubleValue(uploadRateData, "uploadRate")));
        overview.put("greenLineRate", Math.round(getDoubleValue(greenLineData, "greenLineRate")));

        // 2. 处理平均睡眠时长
        double avgSleepTime = getDoubleValue(avgData, "avgSleepTime");
        int hours = (int) avgSleepTime;
        int minutes = (int) Math.round((avgSleepTime - hours) * 60);
        overview.put("avgSleepTime", hours + "小时" + minutes + "分钟");

        // 3. 平均得分
        overview.put("avgScore", Math.round(getDoubleValue(avgData, "avgScore")));

        result.put("overview", overview);

        // 4. 睡眠时长分布
        Map<String, Object> durationData = sleepMapper.getSleepDurationDistribution();
        List<Map<String, Object>> durationLegend = new ArrayList<>();

        long total = getLongValue(durationData, "total");
        if (total > 0) {
            durationLegend.add(createLegendItem("<4小时",
                    Math.round(getLongValue(durationData, "less4") * 100.0 / total), "#FF6B6B"));
            durationLegend.add(createLegendItem("4-6小时",
                    Math.round(getLongValue(durationData, "range4to6") * 100.0 / total), "#FFA726"));
            durationLegend.add(createLegendItem("6-8小时",
                    Math.round(getLongValue(durationData, "range6to8") * 100.0 / total), "#4FC3F7"));
            durationLegend.add(createLegendItem(">8小时",
                    Math.round(getLongValue(durationData, "more8") * 100.0 / total), "#66BB6A"));
        }
        result.put("durationLegend", durationLegend);
        result.put("durationTotal", (int)total); // 添加总人数

        // 5. 睡眠分类统计
        Map<String, Object> categoryData = sleepMapper.getSleepCategoryDistribution();
        List<Map<String, Object>> categoryLegend = new ArrayList<>();

        long categoryTotal = getLongValue(categoryData, "total");
        if (categoryTotal > 0) {
            categoryLegend.add(createLegendItem("深睡",
                    Math.round(getLongValue(categoryData, "deepSleep") * 100.0 / categoryTotal), "#4FC3F7"));
            categoryLegend.add(createLegendItem("浅睡",
                    Math.round(getLongValue(categoryData, "lightSleep") * 100.0 / categoryTotal), "#66BB6A"));
            categoryLegend.add(createLegendItem("梦境",
                    Math.round(getLongValue(categoryData, "dream") * 100.0 / categoryTotal), "#FFA726"));
            categoryLegend.add(createLegendItem("清醒",
                    Math.round(getLongValue(categoryData, "awake") * 100.0 / categoryTotal), "#FF6B6B"));
            categoryLegend.add(createLegendItem("睡眠小憩、午睡",
                    Math.round(getLongValue(categoryData, "nap") * 100.0 / categoryTotal), "#FFB84D"));
        }
        result.put("categoryLegend", categoryLegend);

        // 6. 部门上传统计
        List<Map<String, Object>> deptStats = sleepMapper.getDeptUploadStats();
        result.put("deptUpload", deptStats);

        // 7. 最新数据明细
        List<Map<String, Object>> details = sleepMapper.getLatestSleepDetails();
        // 格式化睡眠时长
        for (Map<String, Object> detail : details) {
            double sleepHours = getDoubleValue(detail, "sleepHours");
            int h = (int) sleepHours;
            int m = (int) Math.round((sleepHours - h) * 60);
            detail.put("sleepHours", h + "小时" + m + "分钟");

            // 添加评级文本
            String level = (String) detail.get("level");
            String levelText = "良";
            if ("excellent".equals(level)) levelText = "优";
            else if ("fair".equals(level)) levelText = "中";
            else if ("poor".equals(level)) levelText = "差";
            detail.put("levelText", levelText);
        }
        result.put("detailList", details);

        return result;
    }

    private Map<String, Object> createLegendItem(String name, long value, String color) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value);
        item.put("color", color);
        return item;
    }

    // 辅助方法
    private long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        return 0L;
    }

    private int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        return 0;
    }

    //    private double getDoubleValue(Map<String, Object> map, String key) {
//        Object value = map.get(key);
//        if (value == null) return 0.0;
//        if (value instanceof Double) return (Double) value;
//        if (value instanceof Float) return ((Float) value).doubleValue();
//        if (value instanceof Integer) return ((Integer) value).doubleValue();
//        if (value instanceof Long) return ((Long) value).doubleValue();
//        return 0.0;
//    }
    private double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Float) return ((Float) value).doubleValue();
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        // ⭐ 添加这一行
        if (value instanceof java.math.BigDecimal) return ((java.math.BigDecimal) value).doubleValue();
        return 0.0;
    }
}