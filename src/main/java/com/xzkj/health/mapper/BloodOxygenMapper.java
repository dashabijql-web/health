package com.xzkj.health.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 血氧数据访问接口
 *
 * 修复说明（v3）：
 * 消除所有 DECLARE @var...; SELECT... 多语句写法，避免 Druid 1.2.8 WallFilter 拦截。
 * 将 @s / @e 时间范围计算全部内联为单条 SELECT，效果与变量写法完全等价。
 */
@Mapper
public interface BloodOxygenMapper {

    /**
     * 获取当月血氧统计概览
     * 优化：用 CTE 单次扫描 v_health_record，消除 detectionRate 的相关子查询（原来扫描2次）
     * Base 含全部记录（用于 totalCount 分母），再用 CASE WHEN 区分有无血氧数据
     */
    @Select("WITH Base AS ( " +
            "  SELECT user_code, blood_oxygen " +
            "  FROM v_health_record " +
            "  WHERE record_time >= CONVERT(DATETIME, #{startDate}) " +
            "  AND   record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            ") " +
            "SELECT " +
            "CAST(AVG(CAST(CASE WHEN blood_oxygen IS NOT NULL THEN blood_oxygen END AS FLOAT)) AS DECIMAL(5,2)) AS avgBloodOxygen, " +
            "COALESCE(MAX(CASE WHEN blood_oxygen IS NOT NULL THEN blood_oxygen END), 0) AS maxBloodOxygen, " +
            "COALESCE(MIN(CASE WHEN blood_oxygen IS NOT NULL THEN blood_oxygen END), 0) AS minBloodOxygen, " +
            "COUNT(DISTINCT CASE WHEN blood_oxygen >= 95 THEN user_code END) AS normalCount, " +
            "COUNT(DISTINCT CASE WHEN blood_oxygen IS NOT NULL AND blood_oxygen < 95 THEN user_code END) AS abnormalCount, " +
            "COUNT(DISTINCT CASE WHEN blood_oxygen IS NOT NULL THEN user_code END) AS totalCount, " +
            "ISNULL(COUNT(DISTINCT CASE WHEN blood_oxygen IS NOT NULL THEN user_code END) * 100 / " +
            "  NULLIF(COUNT(DISTINCT user_code), 0), 0) AS detectionRate " +
            "FROM Base")
    Map<String, Object> getBloodOxygenStats(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取血氧趋势数据（无 DECLARE，原本就是单语句，保持不变）
     */
    @Select("SELECT " +
            "CONVERT(VARCHAR(10), record_time, 23) AS date, " +
            "AVG(CAST(blood_oxygen AS FLOAT)) AS avgBloodOxygen, " +
            "MAX(blood_oxygen) AS maxBloodOxygen, " +
            "MIN(blood_oxygen) AS minBloodOxygen " +
            "FROM v_health_record " +
            "WHERE blood_oxygen IS NOT NULL " +
            "AND record_time >= DATEADD(DAY, -#{days}, GETDATE()) " +
            "GROUP BY CONVERT(VARCHAR(10), record_time, 23) " +
            "ORDER BY date")
    List<Map<String, Object>> getBloodOxygenTrend(@Param("days") Integer days);

    /**
     * 获取血氧分布统计
     * 优化：原来6次 UNION ALL 各自独立扫描 v_health_record（6次全扫）
     *      改为单 CTE 扫描一次，再用 CASE WHEN 分组聚合（1次扫描）
     */
    @Select("WITH Base AS ( " +
            "  SELECT blood_oxygen " +
            "  FROM v_health_record " +
            "  WHERE blood_oxygen IS NOT NULL " +
            "  AND record_time >= CONVERT(DATETIME, #{startDate}) " +
            "  AND record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "), " +
            "Grouped AS ( " +
            "  SELECT " +
            "    CASE " +
            "      WHEN blood_oxygen <  90 THEN '<90' " +
            "      WHEN blood_oxygen <  93 THEN '90-93' " +
            "      WHEN blood_oxygen <  95 THEN '93-95' " +
            "      WHEN blood_oxygen <  97 THEN '95-97' " +
            "      WHEN blood_oxygen <  99 THEN '97-99' " +
            "      ELSE '≥99' " +
            "    END AS range, " +
            "    CASE " +
            "      WHEN blood_oxygen <  90 THEN 1 " +
            "      WHEN blood_oxygen <  93 THEN 2 " +
            "      WHEN blood_oxygen <  95 THEN 3 " +
            "      WHEN blood_oxygen <  97 THEN 4 " +
            "      WHEN blood_oxygen <  99 THEN 5 " +
            "      ELSE 6 " +
            "    END AS sort_order " +
            "  FROM Base " +
            ") " +
            "SELECT range, COUNT(*) AS count " +
            "FROM Grouped " +
            "GROUP BY range, sort_order " +
            "ORDER BY sort_order")
    List<Map<String, Object>> getBloodOxygenDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取异常血氧记录（分页）
     * 修复（v2已修）：原来用 id NOT IN (SELECT TOP offset ...) 做分页越翻越慢
     * 改为标准 OFFSET...FETCH，已是单语句，无需再动
     */
    @Select("SELECT " +
            "hr.id, " +
            "hr.user_code AS userCode, " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "hr.blood_oxygen AS bloodOxygen, " +
            "hr.record_time AS recordTime, " +
            "CASE " +
            "  WHEN hr.blood_oxygen < 90 THEN 'danger' " +
            "  WHEN hr.blood_oxygen < 95 THEN 'warning' " +
            "  ELSE 'normal' " +
            "END AS level " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.blood_oxygen < 95 " +
            "AND hr.record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "ORDER BY hr.record_time DESC " +
            "OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY")
    List<Map<String, Object>> getAbnormalRecords(@Param("offset") Integer offset,
                                                 @Param("size") Integer size);

    /**
     * 获取异常血氧记录总数
     */
    @Select("SELECT COUNT(*) FROM v_health_record " +
            "WHERE blood_oxygen < 95 " +
            "AND record_time >= DATEADD(DAY, -30, GETDATE())")
    Integer getAbnormalCount();

    /**
     * 获取TOP异常人员统计
     */
    @Select("SELECT TOP (#{limit}) " +
            "hr.user_code AS userCode, " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "COUNT(*) AS count " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "WHERE hr.blood_oxygen < 90 " +
            "AND hr.blood_oxygen IS NOT NULL " +
            "AND hr.record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND hr.record_time < DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "GROUP BY hr.user_code, e.emp_name " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getTopUsers(@Param("limit") int limit, @Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取部门血氧统计
     */
    @Select("SELECT " +
            "d.dept_name AS deptName, " +
            "CAST(AVG(CAST(hr.blood_oxygen AS FLOAT)) AS INT) AS avgBloodOxygen, " +
            "SUM(CASE WHEN hr.blood_oxygen < 90 THEN 1 ELSE 0 END) AS lowCount, " +
            "SUM(CASE WHEN hr.blood_oxygen >= 99 THEN 1 ELSE 0 END) AS highCount, " +
            "COUNT(*) AS totalCount " +
            "FROM v_health_record hr " +
            "INNER JOIN employee e ON hr.user_code = e.emp_code " +
            "INNER JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.blood_oxygen IS NOT NULL " +
            "AND hr.blood_oxygen > 0 " +
            "AND hr.record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND hr.record_time < DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "GROUP BY d.dept_name " +
            "ORDER BY avgBloodOxygen DESC")
    List<Map<String, Object>> getDepartmentStats(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取年龄段血氧分布（关联 employee.birth_date 计算真实年龄）
     * 返回: ageRange, avgBloodOxygen
     */
    @Select("SELECT " +
            "CASE " +
            "  WHEN DATEDIFF(YEAR, e.birth_date, GETDATE()) < 30 THEN '20-30' " +
            "  WHEN DATEDIFF(YEAR, e.birth_date, GETDATE()) < 40 THEN '30-40' " +
            "  WHEN DATEDIFF(YEAR, e.birth_date, GETDATE()) < 50 THEN '40-50' " +
            "  ELSE '50+' " +
            "END AS ageRange, " +
            "CAST(AVG(CAST(hr.blood_oxygen AS FLOAT)) AS INT) AS avgBloodOxygen " +
            "FROM v_health_record hr " +
            "INNER JOIN employee e ON hr.user_code = e.emp_code " +
            "WHERE hr.blood_oxygen IS NOT NULL AND hr.blood_oxygen > 0 " +
            "AND e.birth_date IS NOT NULL " +
            "AND hr.record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND hr.record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "GROUP BY " +
            "CASE " +
            "  WHEN DATEDIFF(YEAR, e.birth_date, GETDATE()) < 30 THEN '20-30' " +
            "  WHEN DATEDIFF(YEAR, e.birth_date, GETDATE()) < 40 THEN '30-40' " +
            "  WHEN DATEDIFF(YEAR, e.birth_date, GETDATE()) < 50 THEN '40-50' " +
            "  ELSE '50+' " +
            "END " +
            "ORDER BY MIN(DATEDIFF(YEAR, e.birth_date, GETDATE()))")
    List<Map<String, Object>> getAgeDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取实时血氧数据
     */
    @Select("SELECT TOP (#{limit}) " +
            "hr.user_code AS userCode, " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "hr.blood_oxygen AS bloodOxygen, " +
            "hr.record_time AS recordTime " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.blood_oxygen IS NOT NULL " +
            "AND hr.blood_oxygen > 0 " +
            "ORDER BY hr.record_time DESC")
    List<Map<String, Object>> getRealtimeData(@Param("limit") int limit);

    /**
     * 获取指定日期每小时平均血氧
     */
    @Select("SELECT DATEPART(HOUR, record_time) AS hour, " +
            "CAST(AVG(CAST(blood_oxygen AS FLOAT)) AS DECIMAL(5,1)) AS avgBloodOxygen " +
            "FROM v_health_record " +
            "WHERE blood_oxygen IS NOT NULL AND blood_oxygen > 0 " +
            "AND record_time >= CONVERT(DATETIME, #{startDate}) " +
            "AND record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{endDate})) " +
            "GROUP BY DATEPART(HOUR, record_time) " +
            "ORDER BY hour")
    List<Map<String, Object>> getHourlyStats(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** 实时血氧列表（近2小时最新记录，按时间倒序） */
    @Select("SELECT TOP (#{limit}) " +
            "hr.user_code AS userCode, " +
            "ISNULL(e.emp_name, hr.user_code) AS userName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "hr.blood_oxygen AS bloodOxygen, " +
            "hr.record_time AS recordTime " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.blood_oxygen IS NOT NULL AND hr.blood_oxygen > 0 " +
            "AND hr.record_time >= DATEADD(HOUR, -2, GETDATE()) " +
            "ORDER BY hr.record_time DESC")
    List<Map<String, Object>> getRealtime(@Param("limit") int limit);
}