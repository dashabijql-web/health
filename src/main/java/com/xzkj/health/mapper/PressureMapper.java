package com.xzkj.health.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 压力指数分析 Mapper
 * 字段：pressure（0-100，正常 < 70，偏高 70-84，高压 >= 85）
 */
@Mapper
public interface PressureMapper {

    /** 概览统计 */
    @Select("SELECT " +
            "COALESCE(CAST(AVG(CAST(pressure AS FLOAT)) AS INT), 0) AS avgPressure, " +
            "COALESCE(MIN(pressure), 0) AS minPressure, " +
            "COALESCE(MAX(pressure), 0) AS maxPressure, " +
            "COUNT(DISTINCT user_code) AS detectionCount, " +
            "COUNT(*) AS totalCount, " +
            "COALESCE(CAST(SUM(CASE WHEN pressure < 70 THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0) AS INT), 0) AS normalRate, " +
            "COALESCE(SUM(CASE WHEN pressure >= 70 THEN 1 ELSE 0 END), 0) AS abnormalCount, " +
            "COALESCE(SUM(CASE WHEN pressure >= 85 THEN 1 ELSE 0 END), 0) AS highCount " +
            "FROM v_health_record " +
            "WHERE pressure IS NOT NULL AND pressure > 0 " +
            "AND record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate}))")
    Map<String, Object> getOverview(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** 每日趋势 */
    @Select("SELECT " +
            "CONVERT(VARCHAR(10), record_time, 23) AS date, " +
            "CAST(AVG(CAST(pressure AS FLOAT)) AS INT) AS avgPressure " +
            "FROM v_health_record " +
            "WHERE pressure IS NOT NULL AND pressure > 0 " +
            "AND record_time >= DATEADD(DAY, -#{days}, GETDATE()) " +
            "GROUP BY CONVERT(VARCHAR(10), record_time, 23) " +
            "ORDER BY date")
    List<Map<String, Object>> getTrend(@Param("days") int days);

    /** 分布统计：正常 / 偏高 / 高压 */
    @Select("SELECT category AS name, CAST(cnt * 100.0 / NULLIF(SUM(cnt) OVER(), 0) AS INT) AS value, color " +
            "FROM ( " +
            "  SELECT " +
            "    CASE WHEN pressure < 70 THEN '正常' WHEN pressure < 85 THEN '偏高' ELSE '高压' END AS category, " +
            "    CASE WHEN pressure < 70 THEN '#66BB6A' WHEN pressure < 85 THEN '#FFB84D' ELSE '#F06292' END AS color, " +
            "    COUNT(*) AS cnt " +
            "  FROM v_health_record " +
            "  WHERE pressure IS NOT NULL AND pressure > 0 " +
            "  AND record_time >= CONVERT(DATETIME, #{startDate}) " +
            "  AND record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "  GROUP BY " +
            "    CASE WHEN pressure < 70 THEN '正常' WHEN pressure < 85 THEN '偏高' ELSE '高压' END, " +
            "    CASE WHEN pressure < 70 THEN '#66BB6A' WHEN pressure < 85 THEN '#FFB84D' ELSE '#F06292' END " +
            ") AS ps " +
            "ORDER BY CASE category WHEN '正常' THEN 1 WHEN '偏高' THEN 2 ELSE 3 END")
    List<Map<String, Object>> getDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** TOP N 高压力人员 */
    @Select("SELECT TOP (#{limit}) " +
            "hr.user_code AS userCode, " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "CAST(AVG(CAST(hr.pressure AS FLOAT)) AS INT) AS avgPressure, " +
            "MAX(hr.pressure) AS maxPressure, " +
            "COUNT(*) AS count " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.pressure IS NOT NULL AND hr.pressure >= 70 " +
            "AND hr.record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND hr.record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "GROUP BY hr.user_code, e.emp_name, d.dept_name " +
            "ORDER BY avgPressure DESC")
    List<Map<String, Object>> getTopUsers(@Param("limit") int limit,
                                          @Param("startDate") String startDate,
                                          @Param("endDate") String endDate);

    /** 部门压力统计 */
    @Select("SELECT " +
            "d.dept_name AS deptName, " +
            "CAST(AVG(CAST(hr.pressure AS FLOAT)) AS INT) AS avgPressure, " +
            "SUM(CASE WHEN hr.pressure >= 85 THEN 1 ELSE 0 END) AS highCount, " +
            "SUM(CASE WHEN hr.pressure >= 70 THEN 1 ELSE 0 END) AS abnormalCount, " +
            "COUNT(*) AS totalCount " +
            "FROM v_health_record hr " +
            "INNER JOIN employee e ON hr.user_code = e.emp_code " +
            "INNER JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.pressure IS NOT NULL AND hr.pressure > 0 " +
            "AND hr.record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND hr.record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "GROUP BY d.dept_name " +
            "ORDER BY avgPressure DESC")
    List<Map<String, Object>> getDepartmentStats(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** 异常压力记录（分页） */
    @Select("SELECT " +
            "hr.user_code AS userCode, " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "hr.pressure, " +
            "CASE WHEN hr.pressure >= 85 THEN 'danger' ELSE 'warning' END AS level, " +
            "hr.record_time AS recordTime " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.pressure >= 70 AND hr.pressure IS NOT NULL " +
            "AND hr.record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "ORDER BY hr.record_time DESC " +
            "OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY")
    List<Map<String, Object>> getAbnormalRecords(@Param("offset") int offset, @Param("size") int size);

    /** 异常记录总数 */
    @Select("SELECT COUNT(*) FROM v_health_record " +
            "WHERE pressure >= 70 AND pressure IS NOT NULL " +
            "AND record_time >= DATEADD(DAY, -30, GETDATE())")
    int countAbnormalRecords();

    /** 实时压力列表（近2小时最新记录） */
    @Select("SELECT TOP (#{limit}) " +
            "hr.user_code AS userCode, " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "hr.pressure, " +
            "hr.record_time AS recordTime " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.pressure IS NOT NULL AND hr.pressure > 0 " +
            "AND hr.record_time >= DATEADD(HOUR, -2, GETDATE()) " +
            "ORDER BY hr.record_time DESC")
    List<Map<String, Object>> getRealtime(@Param("limit") int limit);

    /** 指定日期的每小时均值 */
    @Select("SELECT DATEPART(HOUR, record_time) AS hour, " +
            "CAST(AVG(CAST(pressure AS FLOAT)) AS INT) AS avgPressure " +
            "FROM v_health_record " +
            "WHERE pressure IS NOT NULL AND pressure > 0 " +
            "AND record_time >= CONVERT(DATETIME, #{date}) " +
            "AND record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{date})) " +
            "GROUP BY DATEPART(HOUR, record_time) " +
            "ORDER BY hour")
    List<Map<String, Object>> getHourlyStats(@Param("date") String date);
}
