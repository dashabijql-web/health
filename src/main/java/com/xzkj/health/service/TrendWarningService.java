package com.xzkj.health.service;

import com.xzkj.health.mapper.TrendWarningMapper;
import com.xzkj.health.util.TableNameUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 健康趋势预警 Service
 *
 * 算法说明：
 *   1. 拉取最近 14 天所有员工的每日健康指标均值
 *   2. 对每位员工每个指标做简单线性回归，计算趋势斜率（slope）
 *   3. 外推 7 天后的预测值，与阈值比较
 *   4. 按风险程度（高/中/低）分类返回
 *
 * 风险等级：
 *   HIGH   (3) — 当前均值已接近阈值（距离 < 10%）或预测 ≤3 天内突破阈值
 *   MEDIUM (2) — 预测 4~7 天内突破阈值
 *   LOW    (1) — 趋势向危险方向倾斜，但 7 天内不会突破
 */
@Slf4j
@Service
public class TrendWarningService {

    @Autowired
    private TrendWarningMapper trendWarningMapper;

    // ─── 指标阈值（与 DataProcessService 保持一致）─────────────────────
    // heart_rate:          高 >100  低 <55   (bpm)
    // blood_oxygen:        低 <95   (%)
    // temperature:         高 >373  (实际 *10，即 37.3°C)
    // avg_bp_high:         高 >140  (mmHg)
    // avg_pressure:        高 >75   (压力指数)

    private static final Map<String, double[]> THRESHOLDS = new LinkedHashMap<>();
    private static final Map<String, String>   METRIC_NAMES = new LinkedHashMap<>();
    private static final Map<String, String>   METRIC_UNITS = new LinkedHashMap<>();

    static {
        // [lowThreshold, highThreshold]  — NaN 表示该方向无阈值
        THRESHOLDS.put("avg_heart_rate",  new double[]{ 55,  100 });
        THRESHOLDS.put("avg_blood_oxygen",new double[]{ 95,  Double.NaN });
        THRESHOLDS.put("avg_temperature", new double[]{ Double.NaN, 373 });
        THRESHOLDS.put("avg_bp_high",     new double[]{ Double.NaN, 140 });
        THRESHOLDS.put("avg_pressure",    new double[]{ Double.NaN, 75  });

        METRIC_NAMES.put("avg_heart_rate",  "心率");
        METRIC_NAMES.put("avg_blood_oxygen","血氧");
        METRIC_NAMES.put("avg_temperature", "体温");
        METRIC_NAMES.put("avg_bp_high",     "收缩压");
        METRIC_NAMES.put("avg_pressure",    "压力指数");

        METRIC_UNITS.put("avg_heart_rate",  "bpm");
        METRIC_UNITS.put("avg_blood_oxygen","%");
        METRIC_UNITS.put("avg_temperature", "°C");
        METRIC_UNITS.put("avg_bp_high",     "mmHg");
        METRIC_UNITS.put("avg_pressure",    "");
    }

    /**
     * 执行预测，返回结果
     * @return Map containing "list" (at-risk employees) and "summary" (stats)
     */
    public Map<String, Object> predict() {
        int days = 14;
        List<Map<String, Object>> rows = trendWarningMapper.getDailyAverages(healthRecordSource(days), days);

        // 按 emp_code 分组
        Map<String, List<Map<String, Object>>> byEmp = rows.stream()
                .collect(Collectors.groupingBy(r -> (String) r.get("emp_code"),
                         LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> riskList = new ArrayList<>();
        int totalEmployees = byEmp.size();

        for (Map.Entry<String, List<Map<String, Object>>> entry : byEmp.entrySet()) {
            String empCode = entry.getKey();
            List<Map<String, Object>> empRows = entry.getValue();
            if (empRows.isEmpty()) continue;

            String empName  = (String) empRows.get(0).get("emp_name");
            String deptName = (String) empRows.get(0).get("dept_name");

            // 收集日期列表和各指标序列
            List<String> dates = empRows.stream()
                    .map(r -> String.valueOf(r.get("record_date")))
                    .collect(Collectors.toList());

            List<Map<String, Object>> riskMetrics = new ArrayList<>();
            int maxRiskLevel = 0;

            for (String metric : THRESHOLDS.keySet()) {
                List<Double> values = extractValues(empRows, metric);
                // 至少 2 个数据点才做预测
                long validCount = values.stream().filter(v -> !Double.isNaN(v)).count();
                if (validCount < 2) continue;

                // 用最后 min(7, size) 个有效数据点做线性回归
                List<double[]> points = buildPoints(values);
                if (points.size() < 2) continue;

                double slope = linearRegressionSlope(points);
                double lastValue = points.get(points.size() - 1)[1];
                double[] thresh = THRESHOLDS.get(metric);
                double lowT  = thresh[0];
                double highT = thresh[1];

                // 检查是否有风险（趋势向危险方向）
                int riskLevel = 0;
                String riskDir = null;
                double targetThresh = Double.NaN;

                // 检查高阈值方向
                if (!Double.isNaN(highT) && slope > 0) {
                    double gap = highT - lastValue;
                    if (gap <= 0) {
                        riskLevel = 3; riskDir = "high"; targetThresh = highT;
                    } else {
                        double daysToBreak = gap / slope;
                        if (daysToBreak <= 3)       { riskLevel = 3; riskDir = "high"; targetThresh = highT; }
                        else if (daysToBreak <= 7)  { riskLevel = 2; riskDir = "high"; targetThresh = highT; }
                        else if (gap / highT < 0.1) { riskLevel = 1; riskDir = "high"; targetThresh = highT; }
                    }
                }
                // 检查低阈值方向
                if (!Double.isNaN(lowT) && slope < 0) {
                    double gap = lastValue - lowT;
                    if (gap <= 0) {
                        riskLevel = Math.max(riskLevel, 3); riskDir = "low"; targetThresh = lowT;
                    } else {
                        double daysToBreak = gap / (-slope);
                        if      (daysToBreak <= 3)           { riskLevel = Math.max(riskLevel, 3); riskDir = "low"; targetThresh = lowT; }
                        else if (daysToBreak <= 7)           { riskLevel = Math.max(riskLevel, 2); riskDir = "low"; targetThresh = lowT; }
                        else if (gap / lowT < 0.1)           { riskLevel = Math.max(riskLevel, 1); riskDir = "low"; targetThresh = lowT; }
                    }
                }

                if (riskLevel == 0) continue;

                double projectedValue = lastValue + slope * 7;
                // 体温特殊处理：展示时除以10
                double displayCurrent   = "avg_temperature".equals(metric) ? lastValue / 10.0 : lastValue;
                double displayProjected = "avg_temperature".equals(metric) ? projectedValue / 10.0 : projectedValue;
                double displayThreshold = "avg_temperature".equals(metric) ? targetThresh / 10.0 : targetThresh;

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("metric",         metric);
                m.put("metricName",     METRIC_NAMES.get(metric));
                m.put("unit",           METRIC_UNITS.get(metric));
                m.put("currentValue",   round2(displayCurrent));
                m.put("projectedValue", round2(displayProjected));
                m.put("threshold",      round2(displayThreshold));
                m.put("slope",          round2("avg_temperature".equals(metric) ? slope / 10.0 : slope));
                m.put("riskLevel",      riskLevel);
                m.put("riskDir",        riskDir);
                // 历史数据（用于迷你图）
                m.put("history",        buildDisplayHistory(values, "avg_temperature".equals(metric)));

                riskMetrics.add(m);
                maxRiskLevel = Math.max(maxRiskLevel, riskLevel);
            }

            if (riskMetrics.isEmpty()) continue;
            // 按风险等级降序排列指标
            riskMetrics.sort((a, b) -> Integer.compare((int)b.get("riskLevel"), (int)a.get("riskLevel")));

            Map<String, Object> emp = new LinkedHashMap<>();
            emp.put("empCode",    empCode);
            emp.put("empName",    empName);
            emp.put("deptName",   deptName);
            emp.put("riskLevel",  maxRiskLevel);
            emp.put("riskMetrics",riskMetrics);
            emp.put("dates",      dates);
            riskList.add(emp);
        }

        // 按风险等级降序排序
        riskList.sort((a, b) -> Integer.compare((int)b.get("riskLevel"), (int)a.get("riskLevel")));

        // 统计
        long high   = riskList.stream().filter(e -> (int)e.get("riskLevel") == 3).count();
        long medium = riskList.stream().filter(e -> (int)e.get("riskLevel") == 2).count();
        long low    = riskList.stream().filter(e -> (int)e.get("riskLevel") == 1).count();
        long normal = totalEmployees - high - medium - low;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total",        totalEmployees);
        summary.put("highRisk",     high);
        summary.put("mediumRisk",   medium);
        summary.put("lowRisk",      low);
        summary.put("normal",       Math.max(normal, 0));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list",    riskList);
        result.put("summary", summary);
        return result;
    }

    // ─── 私有辅助方法 ────────────────────────────────────────────────────

    private String healthRecordSource(int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);
        List<String> tables = TableNameUtil.healthRecordTables(start, end);
        if (tables.size() == 1) return tables.get(0);

        String cols = "user_code,heart_rate,blood_oxygen,temperature,blood_pressure_high,pressure,record_time";
        return tables.stream()
                .map(t -> "SELECT " + cols + " FROM " + t)
                .collect(Collectors.joining(" UNION ALL ", "(", ")"));
    }

    private List<Double> extractValues(List<Map<String, Object>> rows, String key) {
        return rows.stream().map(r -> {
            Object v = r.get(key);
            if (v == null) return Double.NaN;
            return ((Number) v).doubleValue();
        }).collect(Collectors.toList());
    }

    /** 取最后 7 个有效点，构造 (x, y) 对 */
    private List<double[]> buildPoints(List<Double> values) {
        List<double[]> pts = new ArrayList<>();
        int x = 0;
        for (double v : values) {
            if (!Double.isNaN(v)) pts.add(new double[]{ x, v });
            x++;
        }
        if (pts.size() > 7) pts = pts.subList(pts.size() - 7, pts.size());
        return pts;
    }

    /** 最小二乘法线性回归斜率 */
    private double linearRegressionSlope(List<double[]> pts) {
        int n = pts.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (double[] p : pts) { sumX += p[0]; sumY += p[1]; sumXY += p[0]*p[1]; sumX2 += p[0]*p[0]; }
        double denom = n * sumX2 - sumX * sumX;
        if (Math.abs(denom) < 1e-9) return 0;
        return (n * sumXY - sumX * sumY) / denom;
    }

    /** 将 14 天数据转为前端可用的 List<Double>，NaN 转 null */
    private List<Object> buildDisplayHistory(List<Double> values, boolean divBy10) {
        List<Object> list = new ArrayList<>();
        for (double v : values) {
            if (Double.isNaN(v)) list.add(null);
            else list.add(round2(divBy10 ? v / 10.0 : v));
        }
        return list;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
