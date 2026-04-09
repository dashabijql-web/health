package com.xzkj.health.service;

import com.xzkj.health.common.MapValueUtil;
import com.xzkj.health.mapper.SleepMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 睡眠监测Service
 */
@Service
public class SleepService {

    @Autowired
    private SleepMapper sleepMapper;

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
            avgData.add(MapValueUtil.getDouble(item, "avgSleepHours"));
            deepSleep.add(MapValueUtil.getDouble(item, "avgDeepSleep"));
            lightSleep.add(MapValueUtil.getDouble(item, "avgLightSleep"));
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
            int count = MapValueUtil.getInt(item, "count");
            result.put(quality, count);
        }

        return result;
    }

    /**
     * 获取睡眠页面完整数据（并行查询优化：7个DB查询并行执行，冷启动从3.2s降至~0.5s）
     */
    public Map<String, Object> getSleepPageData() {
        // 并行发起全部7个DB查询
        CompletableFuture<Map<String, Object>> uploadRateFuture  = CompletableFuture.supplyAsync(() -> sleepMapper.getYesterdayUploadRate());
        CompletableFuture<Map<String, Object>> greenLineFuture   = CompletableFuture.supplyAsync(() -> sleepMapper.getGreenLineRate());
        CompletableFuture<Map<String, Object>> avgDataFuture     = CompletableFuture.supplyAsync(() -> sleepMapper.getAverageSleepData());
        CompletableFuture<Map<String, Object>> durationFuture    = CompletableFuture.supplyAsync(() -> sleepMapper.getSleepDurationDistribution());
        CompletableFuture<Map<String, Object>> categoryFuture    = CompletableFuture.supplyAsync(() -> sleepMapper.getSleepCategoryDistribution());
        CompletableFuture<List<Map<String, Object>>> deptFuture  = CompletableFuture.supplyAsync(() -> sleepMapper.getDeptUploadStats());
        CompletableFuture<List<Map<String, Object>>> detailFuture = CompletableFuture.supplyAsync(() -> sleepMapper.getLatestSleepDetails());

        // 等待所有查询完成（join 不抛 checked exception）
        Map<String, Object> uploadRateData = uploadRateFuture.join();
        Map<String, Object> greenLineData  = greenLineFuture.join();
        Map<String, Object> avgData        = avgDataFuture.join();
        Map<String, Object> durationData   = durationFuture.join();
        Map<String, Object> categoryData   = categoryFuture.join();
        List<Map<String, Object>> deptStats = deptFuture.join();
        List<Map<String, Object>> details   = detailFuture.join();

        Map<String, Object> result = new HashMap<>();

        // 1. 概览：上传率、达标率、平均睡眠时长、平均得分
        Map<String, Object> overview = new HashMap<>();
        overview.put("uploadRate", Math.round(MapValueUtil.getDouble(uploadRateData, "uploadRate")));
        overview.put("greenLineRate", Math.round(MapValueUtil.getDouble(greenLineData, "greenLineRate")));

        double avgSleepTime = MapValueUtil.getDouble(avgData, "avgSleepTime");
        int hours = (int) avgSleepTime;
        int minutes = (int) Math.round((avgSleepTime - hours) * 60);
        overview.put("avgSleepTime", hours + "小时" + minutes + "分钟");
        overview.put("avgScore", Math.round(MapValueUtil.getDouble(avgData, "avgScore")));
        result.put("overview", overview);

        // 2. 睡眠时长分布
        List<Map<String, Object>> durationLegend = new ArrayList<>();
        long total = MapValueUtil.getLong(durationData, "total");
        if (total > 0) {
            durationLegend.add(createLegendItem("<4小时",
                    Math.round(MapValueUtil.getLong(durationData, "less4") * 100.0 / total), "#FF6B6B"));
            durationLegend.add(createLegendItem("4-6小时",
                    Math.round(MapValueUtil.getLong(durationData, "range4to6") * 100.0 / total), "#FFA726"));
            durationLegend.add(createLegendItem("6-8小时",
                    Math.round(MapValueUtil.getLong(durationData, "range6to8") * 100.0 / total), "#4FC3F7"));
            durationLegend.add(createLegendItem(">8小时",
                    Math.round(MapValueUtil.getLong(durationData, "more8") * 100.0 / total), "#66BB6A"));
        }
        result.put("durationLegend", durationLegend);
        result.put("durationTotal", (int) total);

        // 3. 睡眠分类统计
        List<Map<String, Object>> categoryLegend = new ArrayList<>();
        long deepSleepMin  = MapValueUtil.getLong(categoryData, "deepSleep");
        long lightSleepMin = MapValueUtil.getLong(categoryData, "lightSleep");
        long dreamMin      = MapValueUtil.getLong(categoryData, "dream");
        long awakeMin      = MapValueUtil.getLong(categoryData, "awake");
        long timeTotal     = deepSleepMin + lightSleepMin + dreamMin + awakeMin;
        if (timeTotal > 0) {
            categoryLegend.add(createLegendItem("深睡",  Math.round(deepSleepMin  * 100.0 / timeTotal), "#4FC3F7"));
            categoryLegend.add(createLegendItem("浅睡",  Math.round(lightSleepMin * 100.0 / timeTotal), "#66BB6A"));
            categoryLegend.add(createLegendItem("梦境",  Math.round(dreamMin      * 100.0 / timeTotal), "#FFA726"));
            categoryLegend.add(createLegendItem("清醒",  Math.round(awakeMin      * 100.0 / timeTotal), "#FF6B6B"));
        }
        result.put("categoryLegend", categoryLegend);

        // 4. 部门上传统计
        result.put("deptUpload", deptStats);

        // 5. 最新数据明细（格式化睡眠时长和评级）
        for (Map<String, Object> detail : details) {
            double sleepHours = MapValueUtil.getDouble(detail, "sleepHours");
            int h = (int) sleepHours;
            int m = (int) Math.round((sleepHours - h) * 60);
            detail.put("sleepHours", h + "小时" + m + "分钟");
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

}