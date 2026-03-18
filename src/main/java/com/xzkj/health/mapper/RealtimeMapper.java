package com.xzkj.health.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 实时监控Mapper
 * 使用近7天数据（168小时），避免因手表数据同步延迟导致页面空白
 */
@Mapper
public interface RealtimeMapper {

    /**
     * 获取近7天平均健康数据概览
     */
    @Select("SELECT " +
            "ISNULL(AVG(CAST(heart_rate AS FLOAT)), 0) AS avgHeartRate, " +
            "ISNULL(AVG(CAST(blood_oxygen AS FLOAT)), 0) AS avgBloodOxygen, " +
            "ISNULL(AVG(CAST(steps AS FLOAT)), 0) AS avgSteps, " +
            "ISNULL(AVG(CAST(calories AS FLOAT)), 0) AS avgCalories, " +
            "ISNULL(AVG(CAST(temperature AS FLOAT)) / 10.0, 0) AS avgTemperature, " +
            "ISNULL(AVG(CAST(sleep_minutes AS FLOAT)) / 60.0, 0) AS avgSleep, " +
            "ISNULL(AVG(CAST(blood_pressure_high AS FLOAT)), 0) AS avgBloodPressureHigh, " +
            "ISNULL(AVG(CAST(blood_pressure_low AS FLOAT)), 0) AS avgBloodPressureLow, " +
            "ISNULL(AVG(CAST(pressure AS FLOAT)), 0) AS avgPressure, " +
            "(SELECT COUNT(*) FROM v_warning_record WHERE create_time >= DATEADD(HOUR, -168, GETDATE()) AND is_handled = 0) AS todayWarningCount " +
            "FROM v_health_record " +
            "WHERE record_time >= DATEADD(HOUR, -168, GETDATE())")
    Map<String, Object> getTodayAvgData();

    /**
     * 获取活跃用户列表(近7天有健康记录，取每人最新一条)
     */
    @Select("SELECT " +
            "e.id, " +
            "e.emp_code AS userCode, " +
            "e.emp_name AS userName, " +
            "e.gender, " +
            "CASE WHEN e.birth_date IS NOT NULL THEN FLOOR(DATEDIFF(day, e.birth_date, GETDATE()) / 365.25) ELSE NULL END AS age, " +
            "d.dept_name AS deptName, " +
            "hr.heart_rate AS heartRate, " +
            "hr.blood_oxygen AS bloodOxygen, " +
            "hr.steps, " +
            "hr.calories, " +
            "hr.temperature / 10.0 AS temperature, " +
            "hr.sleep_minutes / 60.0 AS sleepHours, " +
            "hr.blood_pressure_high AS bloodPressureHigh, " +
            "hr.blood_pressure_low AS bloodPressureLow, " +
            "hr.pressure, " +
            "CASE " +
            "    WHEN hr.heart_rate < 60 OR hr.heart_rate > 100 THEN 'warning' " +
            "    WHEN hr.blood_oxygen < 95 THEN 'warning' " +
            "    WHEN hr.temperature < 360 OR hr.temperature > 375 THEN 'warning' " +
            "    WHEN hr.blood_pressure_high > 139 OR hr.blood_pressure_low > 89 THEN 'warning' " +
            "    WHEN hr.pressure > 84 THEN 'warning' " +
            "    ELSE 'normal' " +
            "END AS status, " +
            "hr.record_time AS lastUpdate, " +
            "dv.imei AS imei " +
            "FROM employee e " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "LEFT JOIN device_user du ON du.emp_id = e.id AND du.unbind_time IS NULL " +
            "LEFT JOIN device dv ON dv.id = du.device_id " +
            "INNER JOIN ( " +
            "    SELECT user_code, heart_rate, blood_oxygen, temperature, steps, calories, sleep_minutes, " +
            "           blood_pressure_high, blood_pressure_low, pressure, record_time, " +
            "           ROW_NUMBER() OVER (PARTITION BY user_code ORDER BY record_time DESC) AS rn " +
            "    FROM v_health_record " +
            "    WHERE record_time >= DATEADD(HOUR, -168, GETDATE()) " +
            ") hr ON e.emp_code = hr.user_code AND hr.rn = 1 " +
            "WHERE (e.status IS NULL OR e.status = 0) " +
            "ORDER BY hr.record_time DESC " +
            "OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY")
    List<Map<String, Object>> getOnlineUsers(@Param("offset") int offset, @Param("size") int size);

    /**
     * 获取活跃用户总数（近7天有记录的用户数）
     */
    @Select("SELECT COUNT(DISTINCT user_code) FROM v_health_record WHERE record_time >= DATEADD(HOUR, -168, GETDATE())")
    int countOnlineUsers();

    /**
     * 获取活跃用户列表 — 直接查分区表，避免扫 UNION ALL 视图
     * 当168h跨月时 tableSource = "(SELECT ... FROM t1 UNION ALL SELECT ... FROM t2) v"
     * 当168h在当月时 tableSource = "health_record_YYYYMM"
     */
    @Select("SELECT " +
            "e.id, " +
            "e.emp_code AS userCode, " +
            "e.emp_name AS userName, " +
            "e.gender, " +
            "CASE WHEN e.birth_date IS NOT NULL THEN FLOOR(DATEDIFF(day, e.birth_date, GETDATE()) / 365.25) ELSE NULL END AS age, " +
            "d.dept_name AS deptName, " +
            "hr.heart_rate AS heartRate, " +
            "hr.blood_oxygen AS bloodOxygen, " +
            "hr.steps, " +
            "hr.calories, " +
            "hr.temperature / 10.0 AS temperature, " +
            "hr.sleep_minutes / 60.0 AS sleepHours, " +
            "hr.blood_pressure_high AS bloodPressureHigh, " +
            "hr.blood_pressure_low AS bloodPressureLow, " +
            "hr.pressure, " +
            "CASE " +
            "    WHEN hr.heart_rate < 60 OR hr.heart_rate > 100 THEN 'warning' " +
            "    WHEN hr.blood_oxygen < 95 THEN 'warning' " +
            "    WHEN hr.temperature < 360 OR hr.temperature > 375 THEN 'warning' " +
            "    WHEN hr.blood_pressure_high > 139 OR hr.blood_pressure_low > 89 THEN 'warning' " +
            "    WHEN hr.pressure > 84 THEN 'warning' " +
            "    ELSE 'normal' " +
            "END AS status, " +
            "hr.record_time AS lastUpdate, " +
            "dv.imei AS imei " +
            "FROM employee e " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "LEFT JOIN device_user du ON du.emp_id = e.id AND du.unbind_time IS NULL " +
            "LEFT JOIN device dv ON dv.id = du.device_id " +
            "INNER JOIN ( " +
            "    SELECT user_code, heart_rate, blood_oxygen, temperature, steps, calories, sleep_minutes, " +
            "           blood_pressure_high, blood_pressure_low, pressure, record_time, " +
            "           ROW_NUMBER() OVER (PARTITION BY user_code ORDER BY record_time DESC) AS rn " +
            "    FROM ${tableSource} " +
            "    WHERE record_time >= DATEADD(HOUR, -168, GETDATE()) " +
            ") hr ON e.emp_code = hr.user_code AND hr.rn = 1 " +
            "WHERE (e.status IS NULL OR e.status = 0) " +
            "ORDER BY hr.record_time DESC " +
            "OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY")
    List<Map<String, Object>> getOnlineUsersDirect(@Param("tableSource") String tableSource,
                                                    @Param("offset") int offset,
                                                    @Param("size") int size);

    /**
     * 活跃用户总数 — 直接查分区表
     */
    @Select("SELECT COUNT(DISTINCT user_code) FROM ${tableSource} WHERE record_time >= DATEADD(HOUR, -168, GETDATE())")
    int countOnlineUsersDirect(@Param("tableSource") String tableSource);

    /**
     * 获取实时统计数据（近7天口径）— 直接查分区表版本
     * 168h 窗口最多跨当月+上月两张表，避免扫 v_health_record UNION ALL 13 张表
     */
    @SelectProvider(type = RealtimeStatsSqlProvider.class, method = "getStatisticsDirect")
    Map<String, Object> getStatisticsDirect();

    /**
     * 动态 SQL 提供器：只 UNION 当月+上月两张分区表，而非 v_health_record 全部13张
     */
    class RealtimeStatsSqlProvider {
        public String getStatisticsDirect() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMM");
            String cur  = "health_record_" + LocalDate.now().format(fmt);
            String prev = "health_record_" + LocalDate.now().minusMonths(1).format(fmt);
            // 必须用 AS 关键字，否则 Druid SQL 防火墙（SQL Server 模式）拒绝子查询别名
            // 不使用 CTE，改为纯子查询，避免 Druid wall filter 拒绝 WITH xxx AS
            String src = "(SELECT user_code, heart_rate, blood_oxygen, record_time FROM " + cur +
                         " UNION ALL SELECT user_code, heart_rate, blood_oxygen, record_time FROM " + prev + ") AS hr168";
            return "SELECT " +
                   "  (SELECT COUNT(DISTINCT user_code) FROM " + src + " WHERE record_time >= DATEADD(HOUR, -168, GETDATE())) AS onlineUsers, " +
                   "  (SELECT COUNT(*) FROM employee WHERE status IS NULL OR status = 0) AS totalUsers, " +
                   "  (SELECT COUNT(*) FROM " + src + " WHERE record_time >= DATEADD(HOUR, -168, GETDATE())) AS weekRecords, " +
                   "  (SELECT COUNT(*) FROM " + src + " WHERE record_time >= DATEADD(HOUR, -168, GETDATE())) AS todayRecords, " +
                   "  ISNULL((SELECT COUNT(DISTINCT user_code) FROM " + src + " WHERE record_time >= DATEADD(HOUR, -168, GETDATE())) * 100 " +
                   "    / NULLIF((SELECT COUNT(*) FROM employee WHERE status IS NULL OR status = 0), 0), 0) AS onlineRate, " +
                   "  ISNULL((SELECT SUM(CASE WHEN heart_rate >= 60 AND heart_rate <= 100 AND blood_oxygen >= 95 THEN 1 ELSE 0 END) FROM " + src + " WHERE record_time >= DATEADD(HOUR, -168, GETDATE())) * 100 " +
                   "    / NULLIF((SELECT COUNT(*) FROM " + src + " WHERE record_time >= DATEADD(HOUR, -168, GETDATE())), 0), 0) AS normalRate";
        }
    }

    /**
     * 获取实时统计数据（近7天口径）
     * 优化：用 CTE 将 v_health_record 的 168h 窗口扫描从 5 次缩减为 1 次
     */
    @Select("WITH Stats7d AS ( " +
            "  SELECT " +
            "    COUNT(*) AS totalRecords, " +
            "    COUNT(DISTINCT user_code) AS distinctUsers, " +
            "    SUM(CASE WHEN heart_rate >= 60 AND heart_rate <= 100 AND blood_oxygen >= 95 THEN 1 ELSE 0 END) AS healthyRecords " +
            "  FROM v_health_record " +
            "  WHERE record_time >= DATEADD(HOUR, -168, GETDATE()) " +
            "), " +
            "TotalEmp AS ( " +
            "  SELECT COUNT(*) AS cnt FROM employee WHERE status IS NULL OR status = 0 " +
            ") " +
            "SELECT " +
            "  (SELECT distinctUsers  FROM Stats7d) AS onlineUsers, " +
            "  (SELECT cnt            FROM TotalEmp) AS totalUsers, " +
            "  (SELECT totalRecords   FROM Stats7d) AS weekRecords, " +
            "  (SELECT totalRecords   FROM Stats7d) AS todayRecords, " +
            "  CASE WHEN (SELECT cnt FROM TotalEmp) > 0 " +
            "       THEN (SELECT distinctUsers FROM Stats7d) * 100 / (SELECT cnt FROM TotalEmp) " +
            "       ELSE 0 END AS onlineRate, " +
            "  CASE WHEN (SELECT totalRecords FROM Stats7d) > 0 " +
            "       THEN (SELECT healthyRecords FROM Stats7d) * 100 / (SELECT totalRecords FROM Stats7d) " +
            "       ELSE 0 END AS normalRate")
    Map<String, Object> getStatistics();

    /**
     * 获取近期未处理告警列表（安全指挥中心实时告警栏）
     */
    @Select("SELECT TOP (#{limit}) " +
            "w.id, " +
            "w.user_code AS userCode, " +
            "ISNULL(e.emp_name, w.user_code) AS userName, " +
            "d.dept_name AS deptName, " +
            "w.warning_type AS warningType, " +
            "w.indicator_name AS indicatorName, " +
            "w.indicator_value AS indicatorValue, " +
            "w.warning_level AS warningLevel, " +
            "w.is_handled AS handled, " +
            "w.create_time AS createTime " +
            "FROM v_warning_record w " +
            "LEFT JOIN employee e ON w.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE w.is_handled = 0 " +
            "AND w.create_time >= DATEADD(DAY, -7, GETDATE()) " +
            "ORDER BY w.create_time DESC")
    List<Map<String, Object>> getRecentAlerts(@Param("limit") int limit);

    /**
     * 获取用户实时数据
     */
    @Select("SELECT TOP 1 " +
            "hr.user_code AS userCode, " +
            "e.emp_name AS userName, " +
            "d.dept_name AS deptName, " +
            "hr.heart_rate AS heartRate, " +
            "hr.blood_oxygen AS bloodOxygen, " +
            "hr.temperature / 10.0 AS temperature, " +
            "hr.blood_pressure_high AS bloodPressureHigh, " +
            "hr.blood_pressure_low AS bloodPressureLow, " +
            "hr.pressure, " +
            "hr.steps, " +
            "hr.calories, " +
            "hr.sleep_minutes / 60.0 AS sleepHours, " +
            "hr.record_time AS lastUpdate, " +
            "CASE " +
            "    WHEN hr.heart_rate < 60 OR hr.heart_rate > 100 THEN 'warning' " +
            "    WHEN hr.blood_oxygen < 95 THEN 'warning' " +
            "    WHEN hr.temperature < 360 OR hr.temperature > 375 THEN 'warning' " +
            "    WHEN hr.blood_pressure_high > 139 OR hr.blood_pressure_low > 89 THEN 'warning' " +
            "    WHEN hr.pressure > 84 THEN 'warning' " +
            "    ELSE 'normal' " +
            "END AS status " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON e.emp_code = hr.user_code " +
            "LEFT JOIN department d ON d.id = e.dept_id " +
            "WHERE hr.user_code = #{userCode} " +
            "ORDER BY hr.record_time DESC")
    Map<String, Object> getUserRealtimeData(@Param("userCode") String userCode);
}