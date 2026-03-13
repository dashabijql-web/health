package com.xzkj.health.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 心率监测Mapper - 增强版
 * 基于真实表结构: health_record
 * 字段: id, user_code, user_name, dept_name, heart_rate, blood_oxygen,
 *       blood_pressure_high, blood_pressure_low, temperature, sleep_hours,
 *       deep_sleep_hours, light_sleep_hours, steps, record_time, create_time, update_time
 */
@Mapper
public interface HeartRateMapper {

    /**
     * 获取心率概览统计
     * 返回: avgHeartRate, detectionRate, minHeartRate, maxHeartRate, normalCount, abnormalCount, totalCount
     */
    @Select("SELECT " +
            "COALESCE(CAST(AVG(CAST(heart_rate AS FLOAT)) AS INT), 0) AS avgHeartRate, " +
            "COALESCE(MIN(heart_rate), 0) AS minHeartRate, " +
            "COALESCE(MAX(heart_rate), 0) AS maxHeartRate, " +
            "COALESCE(CAST(COUNT(DISTINCT user_code) * 100.0 / " +
            "  NULLIF((SELECT COUNT(DISTINCT user_code) FROM v_health_record " +
            "    WHERE record_time >= CONVERT(DATETIME, #{startDate}) " +
            "    AND record_time < DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate}))), 0) AS INT), 0) AS detectionRate, " +
            "COALESCE(SUM(CASE WHEN heart_rate BETWEEN 55 AND 120 THEN 1 ELSE 0 END), 0) AS normalCount, " +
            "COALESCE(SUM(CASE WHEN heart_rate < 55 OR heart_rate > 120 THEN 1 ELSE 0 END), 0) AS abnormalCount, " +
            "COUNT(*) AS totalCount " +
            "FROM v_health_record " +
            "WHERE heart_rate IS NOT NULL " +
            "AND heart_rate > 0 " +
            "AND record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND record_time < DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate}))")
    Map<String, Object> getHeartRateOverview(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取TOP N心率异常人员统计
     * 返回: userName, count (异常次数)
     */
    @Select("SELECT TOP (#{limit}) " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "COUNT(*) AS count " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "WHERE hr.heart_rate IS NOT NULL " +
            "AND hr.heart_rate > 0 " +
            "AND (hr.heart_rate < 55 OR hr.heart_rate > 120) " +
            "AND hr.record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND hr.record_time < DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "GROUP BY hr.user_code, e.emp_name " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getTopUsers(@Param("limit") int limit, @Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取年龄段心率统计（关联 employee.birth_date 计算真实年龄）
     * 返回: ageRange, avgHeartRate
     */
    @Select("SELECT " +
            "CASE " +
            "  WHEN DATEDIFF(YEAR, e.birth_date, GETDATE()) < 30 THEN '20-30' " +
            "  WHEN DATEDIFF(YEAR, e.birth_date, GETDATE()) < 40 THEN '30-40' " +
            "  WHEN DATEDIFF(YEAR, e.birth_date, GETDATE()) < 50 THEN '40-50' " +
            "  ELSE '50+' " +
            "END AS ageRange, " +
            "CAST(AVG(CAST(hr.heart_rate AS FLOAT)) AS INT) AS avgHeartRate " +
            "FROM v_health_record hr " +
            "INNER JOIN employee e ON hr.user_code = e.emp_code " +
            "WHERE hr.heart_rate IS NOT NULL AND hr.heart_rate > 0 " +
            "AND e.birth_date IS NOT NULL " +
            "AND hr.record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "GROUP BY " +
            "CASE " +
            "  WHEN DATEDIFF(YEAR, e.birth_date, GETDATE()) < 30 THEN '20-30' " +
            "  WHEN DATEDIFF(YEAR, e.birth_date, GETDATE()) < 40 THEN '30-40' " +
            "  WHEN DATEDIFF(YEAR, e.birth_date, GETDATE()) < 50 THEN '40-50' " +
            "  ELSE '50+' " +
            "END " +
            "ORDER BY MIN(DATEDIFF(YEAR, e.birth_date, GETDATE()))")
    List<Map<String, Object>> getAgeDistribution();

    /**
     * 获取心率分布统计(新版 - 北路风格)
     * 分类: 心率偏低(<55)、心率正常(55-120)、心率偏高(>120)
     * 返回: name, value(百分比), color
     */
    @Select("WITH HeartRateStats AS ( " +
            "  SELECT " +
            "    CASE " +
            "      WHEN heart_rate < 55 THEN '心率偏低' " +
            "      WHEN heart_rate BETWEEN 55 AND 120 THEN '心率正常' " +
            "      ELSE '心率偏高' " +
            "    END AS category, " +
            "    CASE " +
            "      WHEN heart_rate < 55 THEN '#4FC3F7' " +
            "      WHEN heart_rate BETWEEN 55 AND 120 THEN '#66BB6A' " +
            "      ELSE '#FFB84D' " +
            "    END AS color, " +
            "    COUNT(*) AS cnt " +
            "  FROM v_health_record " +
            "  WHERE heart_rate IS NOT NULL " +
            "  AND heart_rate > 0 " +
            "  AND record_time >= CONVERT(DATETIME, #{startDate}) " +
            "  AND record_time < DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "  GROUP BY " +
            "    CASE " +
            "      WHEN heart_rate < 55 THEN '心率偏低' " +
            "      WHEN heart_rate BETWEEN 55 AND 120 THEN '心率正常' " +
            "      ELSE '心率偏高' " +
            "    END, " +
            "    CASE " +
            "      WHEN heart_rate < 55 THEN '#4FC3F7' " +
            "      WHEN heart_rate BETWEEN 55 AND 120 THEN '#66BB6A' " +
            "      ELSE '#FFB84D' " +
            "    END " +
            ") " +
            "SELECT " +
            "  category AS name, " +
            "  CAST(cnt * 100.0 / NULLIF(SUM(cnt) OVER(), 0) AS INT) AS value, " +
            "  color " +
            "FROM HeartRateStats " +
            "ORDER BY " +
            "  CASE category " +
            "    WHEN '心率偏低' THEN 1 " +
            "    WHEN '心率正常' THEN 2 " +
            "    WHEN '心率偏高' THEN 3 " +
            "  END")
    List<Map<String, Object>> getHeartRateDistributionNew(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取心率趋势数据（按天分组）
     * 返回: date(YYYY-MM-DD), avgHeartRate
     */
    @Select("SELECT " +
            "CONVERT(VARCHAR(10), record_time, 23) AS date, " +
            "CAST(AVG(CAST(heart_rate AS FLOAT)) AS INT) AS avgHeartRate " +
            "FROM v_health_record " +
            "WHERE heart_rate IS NOT NULL " +
            "AND heart_rate > 0 " +
            "AND record_time >= DATEADD(DAY, -#{days}, GETDATE()) " +
            "GROUP BY CONVERT(VARCHAR(10), record_time, 23) " +
            "ORDER BY date")
    List<Map<String, Object>> getHeartRateTrend(@Param("days") int days);

    /**
     * 获取实时心率数据
     * 返回: userName, heartRate, recordTime
     */
    @Select("SELECT TOP (#{limit}) " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "hr.heart_rate AS heartRate, " +
            "hr.record_time AS recordTime " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "WHERE hr.heart_rate IS NOT NULL " +
            "AND hr.heart_rate > 0 " +
            "ORDER BY hr.record_time DESC")
    List<Map<String, Object>> getRealtimeData(@Param("limit") int limit);

    /**
     * 获取部门心率统计
     * 返回: deptName, avgHeartRate, lowCount, highCount, totalCount
     */
    @Select("SELECT " +
            "d.dept_name AS deptName, " +
            "CAST(AVG(CAST(hr.heart_rate AS FLOAT)) AS INT) AS avgHeartRate, " +
            "SUM(CASE WHEN hr.heart_rate < 55 THEN 1 ELSE 0 END) AS lowCount, " +
            "SUM(CASE WHEN hr.heart_rate > 120 THEN 1 ELSE 0 END) AS highCount, " +
            "SUM(CASE WHEN hr.heart_rate < 55 OR hr.heart_rate > 120 THEN 1 ELSE 0 END) AS abnormalCount, " +
            "COUNT(*) AS totalCount " +
            "FROM v_health_record hr " +
            "INNER JOIN employee e ON hr.user_code = e.emp_code " +
            "INNER JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.heart_rate IS NOT NULL " +
            "AND hr.heart_rate > 0 " +
            "AND hr.record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND hr.record_time < DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "GROUP BY d.dept_name " +
            "ORDER BY avgHeartRate DESC")
    List<Map<String, Object>> getDepartmentStats(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取心率统计数据(保留原有方法,用于兼容)
     */
    @Select("SELECT " +
            "AVG(heart_rate) AS avgHeartRate, " +
            "MAX(heart_rate) AS maxHeartRate, " +
            "MIN(heart_rate) AS minHeartRate, " +
            "SUM(CASE WHEN heart_rate >= 60 AND heart_rate <= 100 THEN 1 ELSE 0 END) AS normalCount, " +
            "SUM(CASE WHEN heart_rate < 60 OR heart_rate > 100 THEN 1 ELSE 0 END) AS abnormalCount, " +
            "COUNT(*) AS totalCount " +
            "FROM v_health_record " +
            "WHERE heart_rate IS NOT NULL " +
            "AND record_time >= DATEADD(DAY, -30, GETDATE())")
    Map<String, Object> getHeartRateStats();

    /**
     * 获取心率分布数据(保留原有方法,用于兼容)
     */
    @Select("SELECT " +
            "'<60' AS range, COUNT(*) AS count FROM v_health_record WHERE heart_rate < 60 AND record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "UNION ALL " +
            "SELECT '60-70', COUNT(*) FROM v_health_record WHERE heart_rate >= 60 AND heart_rate < 70 AND record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "UNION ALL " +
            "SELECT '70-80', COUNT(*) FROM v_health_record WHERE heart_rate >= 70 AND heart_rate < 80 AND record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "UNION ALL " +
            "SELECT '80-90', COUNT(*) FROM v_health_record WHERE heart_rate >= 80 AND heart_rate < 90 AND record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "UNION ALL " +
            "SELECT '90-100', COUNT(*) FROM v_health_record WHERE heart_rate >= 90 AND heart_rate <= 100 AND record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "UNION ALL " +
            "SELECT '>100', COUNT(*) FROM v_health_record WHERE heart_rate > 100 AND record_time >= DATEADD(DAY, -30, GETDATE())")
    List<Map<String, Object>> getHeartRateDistribution();

    /**
     * 获取异常心率记录（分页）
     */
    @Select("SELECT " +
            "hr.user_code AS userCode, " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "hr.heart_rate AS heartRate, " +
            "CASE WHEN hr.heart_rate < 50 OR hr.heart_rate > 120 THEN 'danger' " +
            "     WHEN hr.heart_rate < 60 OR hr.heart_rate > 100 THEN 'warning' " +
            "     ELSE 'normal' END AS level, " +
            "hr.record_time AS recordTime " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE (hr.heart_rate < 60 OR hr.heart_rate > 100) " +
            "AND hr.record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "ORDER BY hr.record_time DESC " +
            "OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY")
    List<Map<String, Object>> getAbnormalRecords(@Param("offset") int offset, @Param("size") int size);

    /**
     * 获取异常记录总数
     */
    @Select("SELECT COUNT(*) " +
            "FROM v_health_record " +
            "WHERE (heart_rate < 60 OR heart_rate > 100) " +
            "AND record_time >= DATEADD(DAY, -30, GETDATE())")
    int countAbnormalRecords();

    /**
     * 获取指定日期每小时平均心率
     */
    @Select("SELECT DATEPART(HOUR, record_time) AS hour, " +
            "CAST(AVG(CAST(heart_rate AS FLOAT)) AS INT) AS avgHeartRate " +
            "FROM v_health_record " +
            "WHERE heart_rate IS NOT NULL AND heart_rate > 0 " +
            "AND record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "GROUP BY DATEPART(HOUR, record_time) " +
            "ORDER BY hour")
    List<Map<String, Object>> getHourlyStats(@Param("startDate") String startDate, @Param("endDate") String endDate);
}