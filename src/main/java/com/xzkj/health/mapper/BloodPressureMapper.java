package com.xzkj.health.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 血压分析 Mapper
 * 字段：blood_pressure_high（收缩压 mmHg）、blood_pressure_low（舒张压 mmHg）
 * 正常范围：收缩压 90-139，舒张压 60-89
 */
@Mapper
public interface BloodPressureMapper {

    /** 概览统计：平均收缩压/舒张压、正常率、检测人数、最大/最小值 */
    @Select("SELECT " +
            "COALESCE(CAST(AVG(CAST(blood_pressure_high AS FLOAT)) AS INT), 0) AS avgSystolic, " +
            "COALESCE(CAST(AVG(CAST(blood_pressure_low  AS FLOAT)) AS INT), 0) AS avgDiastolic, " +
            "COALESCE(MIN(blood_pressure_high), 0) AS minSystolic, " +
            "COALESCE(MAX(blood_pressure_high), 0) AS maxSystolic, " +
            "COALESCE(MIN(blood_pressure_low),  0) AS minDiastolic, " +
            "COALESCE(MAX(blood_pressure_low),  0) AS maxDiastolic, " +
            "COUNT(DISTINCT user_code) AS detectionCount, " +
            "COUNT(*) AS totalCount, " +
            "COALESCE(CAST(SUM(CASE WHEN blood_pressure_high BETWEEN 90 AND 139 " +
            "  AND blood_pressure_low BETWEEN 60 AND 89 THEN 1 ELSE 0 END) * 100.0 / " +
            "  NULLIF(COUNT(*), 0) AS INT), 0) AS normalRate, " +
            "COALESCE(SUM(CASE WHEN blood_pressure_high > 139 OR blood_pressure_low > 89 THEN 1 ELSE 0 END), 0) AS abnormalCount, " +
            "COALESCE(CAST(SUM(CASE WHEN (blood_pressure_high BETWEEN 120 AND 139) " +
            "  OR (blood_pressure_low BETWEEN 80 AND 89) THEN 1 ELSE 0 END) * 100.0 / " +
            "  NULLIF(COUNT(*), 0) AS INT), 0) AS elevatedRate, " +
            "COALESCE(CAST(SUM(CASE WHEN blood_pressure_high >= 140 " +
            "  OR blood_pressure_low >= 90 THEN 1 ELSE 0 END) * 100.0 / " +
            "  NULLIF(COUNT(*), 0) AS INT), 0) AS hypertensionRate " +
            "FROM v_health_record " +
            "WHERE blood_pressure_high IS NOT NULL AND blood_pressure_high > 0 " +
            "AND record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate}))")
    Map<String, Object> getOverview(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** 每日趋势：收缩压 + 舒张压均值 */
    @Select("SELECT " +
            "CONVERT(VARCHAR(10), record_time, 23) AS date, " +
            "CAST(AVG(CAST(blood_pressure_high AS FLOAT)) AS INT) AS avgSystolic, " +
            "CAST(AVG(CAST(blood_pressure_low  AS FLOAT)) AS INT) AS avgDiastolic " +
            "FROM v_health_record " +
            "WHERE blood_pressure_high IS NOT NULL AND blood_pressure_high > 0 " +
            "AND record_time >= DATEADD(DAY, -#{days}, GETDATE()) " +
            "GROUP BY CONVERT(VARCHAR(10), record_time, 23) " +
            "ORDER BY date")
    List<Map<String, Object>> getTrend(@Param("days") int days);

    /** 分布统计：正常 / 偏高 / 高血压 */
    @Select("WITH BP AS ( " +
            "  SELECT " +
            "    CASE " +
            "      WHEN blood_pressure_high < 90  OR blood_pressure_low < 60  THEN '偏低' " +
            "      WHEN blood_pressure_high <= 139 AND blood_pressure_low <= 89 THEN '正常' " +
            "      WHEN blood_pressure_high <= 159 OR blood_pressure_low <= 99  THEN '偏高' " +
            "      ELSE '高血压' " +
            "    END AS category, " +
            "    CASE " +
            "      WHEN blood_pressure_high < 90  OR blood_pressure_low < 60  THEN '#4FC3F7' " +
            "      WHEN blood_pressure_high <= 139 AND blood_pressure_low <= 89 THEN '#66BB6A' " +
            "      WHEN blood_pressure_high <= 159 OR blood_pressure_low <= 99  THEN '#FFB84D' " +
            "      ELSE '#F06292' " +
            "    END AS color, " +
            "    COUNT(*) AS cnt " +
            "  FROM v_health_record " +
            "  WHERE blood_pressure_high IS NOT NULL AND blood_pressure_high > 0 " +
            "  AND record_time >= CONVERT(DATETIME, #{startDate}) " +
            "  AND record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "  GROUP BY " +
            "    CASE " +
            "      WHEN blood_pressure_high < 90  OR blood_pressure_low < 60  THEN '偏低' " +
            "      WHEN blood_pressure_high <= 139 AND blood_pressure_low <= 89 THEN '正常' " +
            "      WHEN blood_pressure_high <= 159 OR blood_pressure_low <= 99  THEN '偏高' " +
            "      ELSE '高血压' " +
            "    END, " +
            "    CASE " +
            "      WHEN blood_pressure_high < 90  OR blood_pressure_low < 60  THEN '#4FC3F7' " +
            "      WHEN blood_pressure_high <= 139 AND blood_pressure_low <= 89 THEN '#66BB6A' " +
            "      WHEN blood_pressure_high <= 159 OR blood_pressure_low <= 99  THEN '#FFB84D' " +
            "      ELSE '#F06292' " +
            "    END " +
            ") " +
            "SELECT category AS name, " +
            "  CAST(cnt * 100.0 / NULLIF(SUM(cnt) OVER(), 0) AS INT) AS value, " +
            "  color " +
            "FROM BP " +
            "ORDER BY CASE category WHEN '偏低' THEN 1 WHEN '正常' THEN 2 WHEN '偏高' THEN 3 ELSE 4 END")
    List<Map<String, Object>> getDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** TOP N 高收缩压人员 */
    @Select("SELECT TOP (#{limit}) " +
            "hr.user_code AS userCode, " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "CAST(AVG(CAST(hr.blood_pressure_high AS FLOAT)) AS INT) AS avgSystolic, " +
            "CAST(AVG(CAST(hr.blood_pressure_low  AS FLOAT)) AS INT) AS avgDiastolic, " +
            "COUNT(*) AS count " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.blood_pressure_high IS NOT NULL AND hr.blood_pressure_high > 0 " +
            "AND (hr.blood_pressure_high > 139 OR hr.blood_pressure_low > 89) " +
            "AND hr.record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND hr.record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "GROUP BY hr.user_code, e.emp_name, d.dept_name " +
            "ORDER BY avgSystolic DESC")
    List<Map<String, Object>> getTopUsers(@Param("limit") int limit,
                                          @Param("startDate") String startDate,
                                          @Param("endDate") String endDate);

    /** 部门血压统计 */
    @Select("SELECT " +
            "d.dept_name AS deptName, " +
            "CAST(AVG(CAST(hr.blood_pressure_high AS FLOAT)) AS INT) AS avgSystolic, " +
            "CAST(AVG(CAST(hr.blood_pressure_low  AS FLOAT)) AS INT) AS avgDiastolic, " +
            "SUM(CASE WHEN hr.blood_pressure_high > 139 OR hr.blood_pressure_low > 89 THEN 1 ELSE 0 END) AS abnormalCount, " +
            "COUNT(*) AS totalCount " +
            "FROM v_health_record hr " +
            "INNER JOIN employee e ON hr.user_code = e.emp_code " +
            "INNER JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.blood_pressure_high IS NOT NULL AND hr.blood_pressure_high > 0 " +
            "AND hr.record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND hr.record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "GROUP BY d.dept_name " +
            "ORDER BY avgSystolic DESC")
    List<Map<String, Object>> getDepartmentStats(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** 异常血压记录（分页） */
    @Select("SELECT " +
            "hr.user_code AS userCode, " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "hr.blood_pressure_high AS systolic, " +
            "hr.blood_pressure_low  AS diastolic, " +
            "CASE WHEN hr.blood_pressure_high >= 160 OR hr.blood_pressure_low >= 100 THEN 'danger' " +
            "     WHEN hr.blood_pressure_high >  139 OR hr.blood_pressure_low >   89 THEN 'warning' " +
            "     ELSE 'normal' END AS level, " +
            "hr.record_time AS recordTime " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE (hr.blood_pressure_high > 139 OR hr.blood_pressure_low > 89) " +
            "AND hr.blood_pressure_high IS NOT NULL AND hr.blood_pressure_high > 0 " +
            "AND hr.record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "ORDER BY hr.record_time DESC " +
            "OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY")
    List<Map<String, Object>> getAbnormalRecords(@Param("offset") int offset, @Param("size") int size);

    /** 异常记录总数 */
    @Select("SELECT COUNT(*) FROM v_health_record " +
            "WHERE (blood_pressure_high > 139 OR blood_pressure_low > 89) " +
            "AND blood_pressure_high IS NOT NULL AND blood_pressure_high > 0 " +
            "AND record_time >= DATEADD(DAY, -30, GETDATE())")
    int countAbnormalRecords();

    /** 实时血压列表（近2小时最新记录，按时间倒序） */
    @Select("SELECT TOP (#{limit}) " +
            "hr.user_code AS userCode, " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "hr.blood_pressure_high AS systolic, " +
            "hr.blood_pressure_low  AS diastolic, " +
            "hr.record_time AS recordTime " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.blood_pressure_high IS NOT NULL AND hr.blood_pressure_high > 0 " +
            "AND hr.record_time >= DATEADD(HOUR, -2, GETDATE()) " +
            "ORDER BY hr.record_time DESC")
    List<Map<String, Object>> getRealtime(@Param("limit") int limit);

    /** 指定日期的每小时均值（收缩压 + 舒张压） */
    @Select("SELECT DATEPART(HOUR, record_time) AS hour, " +
            "CAST(AVG(CAST(blood_pressure_high AS FLOAT)) AS INT) AS avgSystolic, " +
            "CAST(AVG(CAST(blood_pressure_low  AS FLOAT)) AS INT) AS avgDiastolic " +
            "FROM v_health_record " +
            "WHERE blood_pressure_high IS NOT NULL AND blood_pressure_high > 0 " +
            "AND record_time >= CONVERT(DATETIME, #{date}) " +
            "AND record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{date})) " +
            "GROUP BY DATEPART(HOUR, record_time) " +
            "ORDER BY hour")
    List<Map<String, Object>> getHourlyStats(@Param("date") String date);
}
