package com.xzkj.health.mapper;

import com.xzkj.health.dto.realtime.RealtimeAlertRow;
import com.xzkj.health.dto.realtime.RealtimeOverviewRow;
import com.xzkj.health.dto.realtime.RealtimeStatisticsRow;
import com.xzkj.health.dto.realtime.RealtimeUserDetailRow;
import com.xzkj.health.dto.realtime.RealtimeUserRow;
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
 * 概览与历史统计保留近7天口径；实时人员列表使用可配置的短时上报窗口。
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
    RealtimeOverviewRow getTodayAvgData();

    /**
     * 获取短时活跃人员快照。每个指标取在线窗口内最新的非空值，避免最新心跳包把其余体征覆盖成空。
     */
    @SelectProvider(type = RealtimeOnlineSqlProvider.class, method = "getActiveUsers")
    List<RealtimeUserRow> getActiveUsersDirect(@Param("tableSource") String tableSource,
                                               @Param("onlineWindowMinutes") int onlineWindowMinutes);

    class RealtimeOnlineSqlProvider {
        public String getActiveUsers(Map<String, Object> params) {
            String source = String.valueOf(params.get("tableSource"));
            if (!source.matches("health_record_\\d{6}|\\(SELECT [a-zA-Z0-9_, ]+ FROM health_record_\\d{6} UNION ALL SELECT [a-zA-Z0-9_, ]+ FROM health_record_\\d{6}\\)")) {
                throw new IllegalArgumentException("Invalid realtime table source");
            }

            String recent = "SELECT t.*, " +
                    "ROW_NUMBER() OVER (PARTITION BY t.user_code, CASE WHEN t.heart_rate IS NOT NULL AND t.heart_rate > 0 THEN 1 ELSE 0 END ORDER BY t.record_time DESC, t.id DESC) AS rn_hr, " +
                    "ROW_NUMBER() OVER (PARTITION BY t.user_code, CASE WHEN t.blood_oxygen IS NOT NULL AND t.blood_oxygen > 0 THEN 1 ELSE 0 END ORDER BY t.record_time DESC, t.id DESC) AS rn_bo, " +
                    "ROW_NUMBER() OVER (PARTITION BY t.user_code, CASE WHEN t.temperature IS NOT NULL AND t.temperature > 0 THEN 1 ELSE 0 END ORDER BY t.record_time DESC, t.id DESC) AS rn_tp, " +
                    "ROW_NUMBER() OVER (PARTITION BY t.user_code, CASE WHEN t.blood_pressure_high IS NOT NULL AND t.blood_pressure_high > 0 THEN 1 ELSE 0 END ORDER BY t.record_time DESC, t.id DESC) AS rn_bph, " +
                    "ROW_NUMBER() OVER (PARTITION BY t.user_code, CASE WHEN t.blood_pressure_low IS NOT NULL AND t.blood_pressure_low > 0 THEN 1 ELSE 0 END ORDER BY t.record_time DESC, t.id DESC) AS rn_bpl, " +
                    "ROW_NUMBER() OVER (PARTITION BY t.user_code, CASE WHEN t.pressure IS NOT NULL THEN 1 ELSE 0 END ORDER BY t.record_time DESC, t.id DESC) AS rn_pr, " +
                    "ROW_NUMBER() OVER (PARTITION BY t.user_code, CASE WHEN t.steps IS NOT NULL THEN 1 ELSE 0 END ORDER BY t.record_time DESC, t.id DESC) AS rn_st, " +
                    "ROW_NUMBER() OVER (PARTITION BY t.user_code, CASE WHEN t.calories IS NOT NULL THEN 1 ELSE 0 END ORDER BY t.record_time DESC, t.id DESC) AS rn_cal, " +
                    "ROW_NUMBER() OVER (PARTITION BY t.user_code, CASE WHEN t.sleep_minutes IS NOT NULL THEN 1 ELSE 0 END ORDER BY t.record_time DESC, t.id DESC) AS rn_sl " +
                    "FROM " + source + " AS t " +
                    "WHERE t.record_time >= DATEADD(MINUTE, -#{onlineWindowMinutes}, GETDATE())";

            String snapshot = "SELECT x.user_code, " +
                    "MAX(CASE WHEN x.rn_hr = 1 AND x.heart_rate IS NOT NULL AND x.heart_rate > 0 THEN x.heart_rate END) AS heart_rate, " +
                    "MAX(CASE WHEN x.rn_bo = 1 AND x.blood_oxygen IS NOT NULL AND x.blood_oxygen > 0 THEN x.blood_oxygen END) AS blood_oxygen, " +
                    "MAX(CASE WHEN x.rn_tp = 1 AND x.temperature IS NOT NULL AND x.temperature > 0 THEN x.temperature END) AS temperature, " +
                    "MAX(CASE WHEN x.rn_bph = 1 AND x.blood_pressure_high IS NOT NULL AND x.blood_pressure_high > 0 THEN x.blood_pressure_high END) AS blood_pressure_high, " +
                    "MAX(CASE WHEN x.rn_bpl = 1 AND x.blood_pressure_low IS NOT NULL AND x.blood_pressure_low > 0 THEN x.blood_pressure_low END) AS blood_pressure_low, " +
                    "MAX(CASE WHEN x.rn_pr = 1 AND x.pressure IS NOT NULL THEN x.pressure END) AS pressure, " +
                    "MAX(CASE WHEN x.rn_st = 1 AND x.steps IS NOT NULL THEN x.steps END) AS steps, " +
                    "MAX(CASE WHEN x.rn_cal = 1 AND x.calories IS NOT NULL THEN x.calories END) AS calories, " +
                    "MAX(CASE WHEN x.rn_sl = 1 AND x.sleep_minutes IS NOT NULL THEN x.sleep_minutes END) AS sleep_minutes, " +
                    "MAX(x.record_time) AS report_time, " +
                    "MAX(CASE WHEN x.heart_rate IS NOT NULL OR x.blood_oxygen IS NOT NULL OR x.temperature IS NOT NULL " +
                    "OR x.blood_pressure_high IS NOT NULL OR x.blood_pressure_low IS NOT NULL OR x.pressure IS NOT NULL " +
                    "THEN x.record_time END) AS vital_record_time " +
                    "FROM (" + recent + ") AS x GROUP BY x.user_code";

            return "SELECT e.id, e.emp_code AS userCode, e.emp_name AS userName, e.gender, " +
                    "CASE WHEN e.birth_date IS NOT NULL THEN FLOOR(DATEDIFF(day, e.birth_date, GETDATE()) / 365.25) ELSE NULL END AS age, " +
                    "d.dept_name AS deptName, j.risk_level AS riskLevel, " +
                    "hr.heart_rate AS heartRate, hr.blood_oxygen AS bloodOxygen, hr.steps, hr.calories, " +
                    "hr.temperature / 10.0 AS temperature, hr.sleep_minutes / 60.0 AS sleepHours, " +
                    "hr.blood_pressure_high AS bloodPressureHigh, hr.blood_pressure_low AS bloodPressureLow, hr.pressure, " +
                    "CONVERT(varchar(23), hr.vital_record_time, 121) AS lastUpdate, " +
                    "DATEDIFF(SECOND, hr.vital_record_time, GETDATE()) AS dataAgeSeconds, dv.imei AS imei " +
                    "FROM employee e " +
                    "LEFT JOIN department d ON e.dept_id = d.id " +
                    "LEFT JOIN job_type j ON e.job_type_id = j.id " +
                    "LEFT JOIN device_user du ON du.emp_id = e.id AND du.unbind_time IS NULL " +
                    "LEFT JOIN device dv ON dv.id = du.device_id " +
                    "INNER JOIN (" + snapshot + ") AS hr ON e.emp_code = hr.user_code " +
                    "WHERE (e.status IS NULL OR e.status = 0) ORDER BY hr.report_time DESC";
        }
    }

    /**
     * 获取实时统计数据（近7天口径）— 直接查分区表版本
     * 168h 窗口最多跨当月+上月两张表，避免扫 v_health_record UNION ALL 13 张表
     */
    @SelectProvider(type = RealtimeStatsSqlProvider.class, method = "getStatisticsDirect")
    RealtimeStatisticsRow getStatisticsDirect();

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
            "CONVERT(varchar(19), w.create_time, 120) AS createTime " +
            "FROM v_warning_record w " +
            "LEFT JOIN employee e ON w.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE w.is_handled = 0 " +
            "AND w.create_time >= DATEADD(DAY, -7, GETDATE()) " +
            "ORDER BY w.create_time DESC")
    List<RealtimeAlertRow> getRecentAlerts(@Param("limit") int limit);

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
            "CONVERT(varchar(19), hr.record_time, 120) AS lastUpdate, " +
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
    RealtimeUserDetailRow getUserRealtimeData(@Param("userCode") String userCode);
}
