package com.xzkj.health.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;
import java.util.Map;

/**
 * Dashboard数据访问接口
 *
 * 所有带 startTime/endTime 的方法均支持日期范围筛选。
 * Service 层负责在 null 时填入当月默认值，Mapper 始终收到具体日期字符串。
 */
@Mapper
public interface DashboardMapper {

    /**
     * 获取指定日期范围内各指标有记录的人次
     */
    @Select("SELECT " +
            "COUNT(CASE WHEN heart_rate   IS NOT NULL THEN 1 END) AS heartRate, " +
            "COUNT(CASE WHEN blood_oxygen IS NOT NULL THEN 1 END) AS bloodOxygen, " +
            "COUNT(CASE WHEN sleep_minutes IS NOT NULL THEN 1 END) AS sleep, " +
            "COUNT(CASE WHEN steps        IS NOT NULL THEN 1 END) AS steps, " +
            "COUNT(CASE WHEN temperature  IS NOT NULL THEN 1 END) AS temperature, " +
            "COUNT(CASE WHEN pressure     IS NOT NULL THEN 1 END) AS pressure " +
            "FROM v_health_record " +
            "WHERE record_time >= CONVERT(date, #{startTime}) " +
            "AND   record_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime}))")
    Map<String, Object> getCountsByRange(@Param("startTime") String startTime,
                                         @Param("endTime")   String endTime);

    /**
     * 获取指定日期范围内各指标平均值
     */
    @Select("SELECT " +
            "AVG(CAST(sleep_minutes AS FLOAT)) / 60.0 AS avgSleep, " +
            "AVG(CAST(pressure     AS FLOAT)) AS avgPressure, " +
            "AVG(CAST(blood_oxygen AS FLOAT)) AS avgBloodOxygen, " +
            "AVG(CAST(heart_rate   AS FLOAT)) AS avgHeartRate, " +
            "AVG(CAST(steps        AS FLOAT)) AS avgSteps, " +
            "AVG(CAST(temperature  AS FLOAT)) / 10.0 AS avgTemperature, " +
            "AVG(CAST(blood_pressure_high AS FLOAT)) AS avgBloodPressureHigh, " +
            "AVG(CAST(blood_pressure_low AS FLOAT)) AS avgBloodPressureLow, " +
            "AVG(CAST(calories AS FLOAT)) AS avgCalories " +
            "FROM v_health_record " +
            "WHERE record_time >= CONVERT(date, #{startTime}) " +
            "AND   record_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime}))")
    Map<String, Object> getAverageByRange(@Param("startTime") String startTime,
                                          @Param("endTime")   String endTime);

    /**
     * 优化版：直接查分区表，避免 v_health_record UNION ALL 视图全扫描
     */
    @Select("SELECT " +
            "AVG(CAST(sleep_minutes AS FLOAT)) / 60.0 AS avgSleep, " +
            "AVG(CAST(pressure     AS FLOAT)) AS avgPressure, " +
            "AVG(CAST(blood_oxygen AS FLOAT)) AS avgBloodOxygen, " +
            "AVG(CAST(heart_rate   AS FLOAT)) AS avgHeartRate, " +
            "AVG(CAST(steps        AS FLOAT)) AS avgSteps, " +
            "AVG(CAST(temperature  AS FLOAT)) / 10.0 AS avgTemperature, " +
            "AVG(CAST(blood_pressure_high AS FLOAT)) AS avgBloodPressureHigh, " +
            "AVG(CAST(blood_pressure_low AS FLOAT)) AS avgBloodPressureLow, " +
            "AVG(CAST(calories AS FLOAT)) AS avgCalories " +
            "FROM ${healthSource} " +
            "WHERE record_time >= CONVERT(date, #{startTime}) " +
            "AND   record_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime}))")
    Map<String, Object> getAverageByRangeDirect(@Param("healthSource") String healthSource,
                                                @Param("startTime")    String startTime,
                                                @Param("endTime")      String endTime);

    /**
     * 按预警次数排行，取 TOP 15 员工（用于"异常人员排行"）
     */
    @Select("SELECT TOP 15 " +
            "ISNULL(e.emp_name, w.user_code) AS userName, " +
            "w.user_code AS userCode, " +
            "COUNT(*) AS count " +
            "FROM v_warning_record w " +
            "LEFT JOIN employee e ON w.user_code = e.emp_code " +
            "WHERE w.create_time >= CONVERT(date, #{startTime}) " +
            "AND   w.create_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "GROUP BY w.user_code, e.emp_name " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getTop5ByRange(@Param("startTime") String startTime,
                                              @Param("endTime")   String endTime);

    /**
     * 优化版：直接查分区表，避免 v_warning_record UNION ALL 全扫描
     */
    @Select("SELECT TOP 15 " +
            "ISNULL(e.emp_name, w.user_code) AS userName, " +
            "w.user_code AS userCode, " +
            "COUNT(*) AS count " +
            "FROM ${warningSource} w " +
            "LEFT JOIN employee e ON w.user_code = e.emp_code " +
            "WHERE w.create_time >= CONVERT(date, #{startTime}) " +
            "AND   w.create_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "GROUP BY w.user_code, e.emp_name " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getTop5ByRangeDirect(@Param("warningSource") String warningSource,
                                                   @Param("startTime")     String startTime,
                                                   @Param("endTime")       String endTime);

    /**
     * 设备统计：boundDevices=当前绑定设备数，activeRate/usageRate/warningRate 均按时间段计算
     * 优化：用 CTE 将 v_health_record 的昂贵扫描从 2 次缩减为 1 次；activeRate 与 usageRate 逻辑相同合并计算
     */
    @Select("WITH TotalEmp AS ( " +
            "  SELECT COUNT(*) AS cnt FROM employee WHERE status IS NULL OR status = 0 " +
            "), " +
            "ActiveEmp AS ( " +
            "  SELECT COUNT(DISTINCT e.emp_code) AS cnt " +
            "  FROM employee e " +
            "  INNER JOIN v_health_record hr ON e.emp_code = hr.user_code " +
            "  WHERE (e.status IS NULL OR e.status = 0) " +
            "  AND hr.record_time >= CONVERT(date, #{startTime}) " +
            "  AND hr.record_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "), " +
            "WarnEmp AS ( " +
            "  SELECT COUNT(DISTINCT e.emp_code) AS cnt " +
            "  FROM employee e " +
            "  INNER JOIN v_warning_record wr ON e.emp_code = wr.user_code " +
            "  WHERE (e.status IS NULL OR e.status = 0) " +
            "  AND wr.create_time >= CONVERT(date, #{startTime}) " +
            "  AND wr.create_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "  AND wr.is_handled = 0 " +
            ") " +
            "SELECT " +
            "(SELECT cnt FROM TotalEmp) AS total, " +
            "(SELECT COUNT(*) FROM device_user WHERE is_current = 1) AS boundDevices, " +
            "ISNULL((SELECT cnt FROM ActiveEmp) * 100 / NULLIF((SELECT cnt FROM TotalEmp), 0), 0) AS activeRate, " +
            "ISNULL((SELECT cnt FROM ActiveEmp) * 100 / NULLIF((SELECT cnt FROM TotalEmp), 0), 0) AS usageRate, " +
            "ISNULL((SELECT cnt FROM WarnEmp)   * 100 / NULLIF((SELECT cnt FROM TotalEmp), 0), 0) AS warningRate, " +
            "(SELECT COUNT(*) FROM device WHERE battery_level IS NOT NULL AND battery_level > 0 AND battery_level < 20) AS lowBattery")
    Map<String, Object> getDeviceStatsByRange(@Param("startTime") String startTime,
                                               @Param("endTime")   String endTime);

    /**
     * 获取指定日期范围内各指标预警率
     * 含义：触发该类型预警的 DISTINCT 员工数 / 全部在职员工数
     * 数据来自 v_warning_record（经过冷却去重的业务事件），与异常率（v_health_record 阈值统计）真正区分
     * 优化：用 CTE 将 v_warning_record 的时间范围扫描从 4 次缩减为 1 次，再用 CASE WHEN 分组聚合
     */
    @Select("WITH WarnData AS ( " +
            "  SELECT user_code, warning_type " +
            "  FROM v_warning_record " +
            "  WHERE create_time >= CONVERT(date, #{startTime}) " +
            "  AND   create_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "), " +
            "TotalEmp AS ( " +
            "  SELECT COUNT(*) AS cnt FROM employee WHERE status IS NULL OR status = 0 " +
            ") " +
            "SELECT name, " +
            "ISNULL(cnt * 100 / NULLIF((SELECT cnt FROM TotalEmp), 0), 0) AS rate, " +
            "icon " +
            "FROM ( " +
            "  SELECT '心率预警率' AS name, " +
            "  COUNT(DISTINCT CASE WHEN warning_type LIKE '%心率%' THEN user_code END) AS cnt, " +
            "  'el-icon-heart' AS icon " +
            "  FROM WarnData " +
            "  UNION ALL " +
            "  SELECT '血氧预警率', " +
            "  COUNT(DISTINCT CASE WHEN warning_type LIKE '%血氧%' THEN user_code END), " +
            "  'el-icon-data-analysis' " +
            "  FROM WarnData " +
            "  UNION ALL " +
            "  SELECT '体温预警率', " +
            "  COUNT(DISTINCT CASE WHEN warning_type LIKE '%体温%' THEN user_code END), " +
            "  'el-icon-thermometer' " +
            "  FROM WarnData " +
            "  UNION ALL " +
            "  SELECT '压力预警率', " +
            "  COUNT(DISTINCT CASE WHEN warning_type LIKE '%压力%' THEN user_code END), " +
            "  'el-icon-warning' " +
            "  FROM WarnData " +
            ") t")
    List<Map<String, Object>> getWarningRatesByRange(@Param("startTime") String startTime,
                                                     @Param("endTime")   String endTime);

    /**
     * 按部门统计指定日期范围内的健康数据量
     */
    @Select("SELECT d.dept_name AS name, COUNT(*) AS count " +
            "FROM v_health_record hr " +
            "INNER JOIN employee e  ON hr.user_code = e.emp_code " +
            "INNER JOIN department d ON e.dept_id  = d.id " +
            "WHERE hr.record_time >= CONVERT(date, #{startTime}) " +
            "AND   hr.record_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "GROUP BY d.dept_name " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getDeptCountsByRange(@Param("startTime") String startTime,
                                                   @Param("endTime")   String endTime);

    /**
     * 获取部门预警数，同时返回上一周期数据用于环比
     */
    @Select("SELECT d.dept_name AS name, " +
            "COUNT(DISTINCT CASE WHEN w.create_time >= CONVERT(date, #{startTime}) " +
            "  AND w.create_time < DATEADD(DAY,1,CONVERT(date,#{endTime})) THEN w.id END) AS count, " +
            "COUNT(DISTINCT CASE WHEN w.create_time >= DATEADD(DAY,-#{days},CONVERT(date,#{startTime})) " +
            "  AND w.create_time < CONVERT(date,#{startTime}) THEN w.id END) AS prevCount " +
            "FROM department d " +
            "LEFT JOIN employee e ON d.id = e.dept_id AND (e.status IS NULL OR e.status = 0) " +
            "LEFT JOIN v_warning_record w ON e.emp_code = w.user_code " +
            "GROUP BY d.dept_name " +
            "HAVING COUNT(DISTINCT CASE WHEN w.create_time >= CONVERT(date, #{startTime}) " +
            "  AND w.create_time < DATEADD(DAY,1,CONVERT(date,#{endTime})) THEN w.id END) > 0 " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getDeptWarningWithTrend(@Param("startTime") String startTime,
                                                      @Param("endTime")   String endTime,
                                                      @Param("days")      int    days);

    /**
     * 优化版：直接查 warning 分区表，避免 v_warning_record UNION ALL 全扫描（~1s → ~150ms）
     */
    @Select("SELECT d.dept_name AS name, " +
            "COUNT(DISTINCT CASE WHEN w.create_time >= CONVERT(date, #{startTime}) " +
            "  AND w.create_time < DATEADD(DAY,1,CONVERT(date,#{endTime})) THEN w.id END) AS count, " +
            "COUNT(DISTINCT CASE WHEN w.create_time >= DATEADD(DAY,-#{days},CONVERT(date,#{startTime})) " +
            "  AND w.create_time < CONVERT(date,#{startTime}) THEN w.id END) AS prevCount " +
            "FROM department d " +
            "LEFT JOIN employee e ON d.id = e.dept_id AND (e.status IS NULL OR e.status = 0) " +
            "LEFT JOIN ${warningSource} w ON e.emp_code = w.user_code " +
            "GROUP BY d.dept_name " +
            "HAVING COUNT(DISTINCT CASE WHEN w.create_time >= CONVERT(date, #{startTime}) " +
            "  AND w.create_time < DATEADD(DAY,1,CONVERT(date,#{endTime})) THEN w.id END) > 0 " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getDeptWarningWithTrendDirect(@Param("warningSource") String warningSource,
                                                            @Param("startTime")     String startTime,
                                                            @Param("endTime")       String endTime,
                                                            @Param("days")          int    days);

    /**
     * 获取每日各指标异常率（用于趋势折线图）
     */
    @Select("SELECT " +
            "CONVERT(VARCHAR(10), record_time, 120) AS date, " +
            "ROUND(COUNT(CASE WHEN heart_rate IS NOT NULL AND (heart_rate > 100 OR heart_rate < 60) THEN 1 END) * 100.0 " +
            "  / NULLIF(COUNT(CASE WHEN heart_rate IS NOT NULL THEN 1 END), 0), 1) AS heartRateRate, " +
            "ROUND(COUNT(CASE WHEN blood_oxygen IS NOT NULL AND blood_oxygen < 95 THEN 1 END) * 100.0 " +
            "  / NULLIF(COUNT(CASE WHEN blood_oxygen IS NOT NULL THEN 1 END), 0), 1) AS bloodOxygenRate, " +
            "ROUND(COUNT(CASE WHEN temperature IS NOT NULL AND (temperature > 375 OR temperature < 360) THEN 1 END) * 100.0 " +
            "  / NULLIF(COUNT(CASE WHEN temperature IS NOT NULL THEN 1 END), 0), 1) AS temperatureRate, " +
            "ROUND(COUNT(CASE WHEN pressure IS NOT NULL AND pressure > 75 THEN 1 END) * 100.0 " +
            "  / NULLIF(COUNT(CASE WHEN pressure IS NOT NULL THEN 1 END), 0), 1) AS pressureRate " +
            "FROM ${tableSource} " +
            "GROUP BY CONVERT(VARCHAR(10), record_time, 120) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> getDailyAnomalyRates(@Param("tableSource") String tableSource);

    // 从预聚合日汇总表读取（极快，每行=1天）
    @Select("SELECT CONVERT(VARCHAR(10), stat_date, 120) AS date, " +
            "heart_rate_rate AS heartRateRate, blood_oxygen_rate AS bloodOxygenRate, " +
            "temperature_rate AS temperatureRate, pressure_rate AS pressureRate " +
            "FROM health_daily_stats " +
            "WHERE stat_date >= CONVERT(date,#{startDate}) AND stat_date <= CONVERT(date,#{endDate}) " +
            "ORDER BY stat_date ASC")
    List<Map<String, Object>> getDailyStatsFromSummary(@Param("startDate") String startDate,
                                                        @Param("endDate")   String endDate);

    // 计算今日实时异常率（只扫当天数据，量小）
    @Select("SELECT CONVERT(VARCHAR(10), record_time, 120) AS date, " +
            "ROUND(COUNT(CASE WHEN heart_rate IS NOT NULL AND (heart_rate>100 OR heart_rate<60) THEN 1 END)*100.0 " +
            "  / NULLIF(COUNT(CASE WHEN heart_rate IS NOT NULL THEN 1 END),0),1) AS heartRateRate, " +
            "ROUND(COUNT(CASE WHEN blood_oxygen IS NOT NULL AND blood_oxygen<95 THEN 1 END)*100.0 " +
            "  / NULLIF(COUNT(CASE WHEN blood_oxygen IS NOT NULL THEN 1 END),0),1) AS bloodOxygenRate, " +
            "ROUND(COUNT(CASE WHEN temperature IS NOT NULL AND (temperature>375 OR temperature<360) THEN 1 END)*100.0 " +
            "  / NULLIF(COUNT(CASE WHEN temperature IS NOT NULL THEN 1 END),0),1) AS temperatureRate, " +
            "ROUND(COUNT(CASE WHEN pressure IS NOT NULL AND pressure>75 THEN 1 END)*100.0 " +
            "  / NULLIF(COUNT(CASE WHEN pressure IS NOT NULL THEN 1 END),0),1) AS pressureRate " +
            "FROM ${tableName} " +
            "WHERE record_time >= CONVERT(date, GETDATE()) " +
            "GROUP BY CONVERT(VARCHAR(10), record_time, 120)")
    List<Map<String, Object>> getTodayAnomalyRates(@Param("tableName") String tableName);

    /**
     * 按日期统计预警数量（用于柱状图，不受 TOP 限制）
     */
    @Select("SELECT CONVERT(VARCHAR(10), create_time, 120) AS stat_date, COUNT(DISTINCT user_code) AS cnt " +
            "FROM v_warning_record " +
            "WHERE create_time >= CONVERT(date, #{startTime}) " +
            "AND   create_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "GROUP BY CONVERT(VARCHAR(10), create_time, 120) " +
            "ORDER BY stat_date ASC")
    List<Map<String, Object>> getWarningCountsByDate(@Param("startTime") String startTime,
                                                     @Param("endTime")   String endTime);

    /**
     * 优化版：直接查分区表，避免 v_warning_record UNION ALL 视图全扫描
     */
    @Select("SELECT CONVERT(VARCHAR(10), create_time, 120) AS stat_date, COUNT(DISTINCT user_code) AS cnt " +
            "FROM ${warningSource} " +
            "WHERE create_time >= CONVERT(date, #{startTime}) " +
            "AND   create_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "GROUP BY CONVERT(VARCHAR(10), create_time, 120) " +
            "ORDER BY stat_date ASC")
    List<Map<String, Object>> getWarningCountsByDateDirect(@Param("warningSource") String warningSource,
                                                           @Param("startTime")     String startTime,
                                                           @Param("endTime")       String endTime);

    /**
     * 按小时统计当日预警数量（用于今日柱状图）
     */
    @Select("SELECT DATEPART(HOUR, create_time) AS hour_num, COUNT(*) AS cnt " +
            "FROM v_warning_record " +
            "WHERE create_time >= CONVERT(date, #{date}) " +
            "AND   create_time <  DATEADD(DAY, 1, CONVERT(date, #{date})) " +
            "GROUP BY DATEPART(HOUR, create_time) " +
            "ORDER BY hour_num ASC")
    List<Map<String, Object>> getWarningCountsByHour(@Param("date") String date);

    /**
     * 获取过去 N 天每日各指标均值（用于健康趋势图）
     */
    @Select("SELECT " +
            "CONVERT(VARCHAR(10), record_time, 120) AS date, " +
            "ROUND(AVG(CAST(heart_rate   AS FLOAT)), 0) AS avgHeartRate, " +
            "ROUND(AVG(CAST(blood_oxygen AS FLOAT)), 0) AS avgBloodOxygen, " +
            "ROUND(AVG(CAST(steps        AS FLOAT)), 0) AS avgSteps " +
            "FROM v_health_record " +
            "WHERE record_time >= CONVERT(date, #{startDate}) " +
            "AND   record_time <  DATEADD(DAY, 1, CONVERT(date, #{endDate})) " +
            "AND (heart_rate IS NOT NULL OR blood_oxygen IS NOT NULL OR steps IS NOT NULL) " +
            "GROUP BY CONVERT(VARCHAR(10), record_time, 120) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> getDailyHealthTrend(@Param("startDate") String startDate,
                                                  @Param("endDate")   String endDate);

    /**
     * 优化版：直接查分区表，避免 v_health_record UNION ALL 视图全扫描
     */
    @Select("SELECT " +
            "CONVERT(VARCHAR(10), record_time, 120) AS date, " +
            "ROUND(AVG(CAST(heart_rate   AS FLOAT)), 0) AS avgHeartRate, " +
            "ROUND(AVG(CAST(blood_oxygen AS FLOAT)), 0) AS avgBloodOxygen, " +
            "ROUND(AVG(CAST(steps        AS FLOAT)), 0) AS avgSteps " +
            "FROM ${healthSource} " +
            "WHERE record_time >= CONVERT(date, #{startDate}) " +
            "AND   record_time <  DATEADD(DAY, 1, CONVERT(date, #{endDate})) " +
            "AND (heart_rate IS NOT NULL OR blood_oxygen IS NOT NULL OR steps IS NOT NULL) " +
            "GROUP BY CONVERT(VARCHAR(10), record_time, 120) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> getDailyHealthTrendDirect(@Param("healthSource") String healthSource,
                                                        @Param("startDate")    String startDate,
                                                        @Param("endDate")      String endDate);

    /**
     * 获取部门健康排名（基于真实预警率）
     * healthScore = 100 - (warningCount / totalRecords * 100)，最低 0
     */
    @Select("SELECT TOP 10 " +
            "d.dept_name AS department, " +
            "COUNT(DISTINCT e.id) AS memberCount, " +
            "CASE WHEN ISNULL(h.record_count, 0) = 0 THEN 75 " +
            "     WHEN ISNULL(w.warning_count, 0) >= h.record_count THEN 0 " +
            "     ELSE CAST(100 - ISNULL(w.warning_count, 0) * 100 / h.record_count AS INT) " +
            "END AS healthScore " +
            "FROM department d " +
            "INNER JOIN employee e ON d.id = e.dept_id AND (e.status IS NULL OR e.status = 0) " +
            "JOIN ( " +
            "  SELECT user_code, COUNT(id) AS record_count " +
            "  FROM v_health_record " +
            "  WHERE record_time >= CONVERT(date, #{startTime}) " +
            "  AND   record_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "  GROUP BY user_code " +
            ") h ON h.user_code = e.emp_code " +
            "LEFT JOIN ( " +
            "  SELECT user_code, COUNT(id) AS warning_count " +
            "  FROM v_warning_record " +
            "  WHERE create_time >= CONVERT(date, #{startTime}) " +
            "  AND   create_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "  GROUP BY user_code " +
            ") w ON w.user_code = e.emp_code " +
            "GROUP BY d.id, d.dept_name, h.record_count, w.warning_count " +
            "ORDER BY healthScore DESC")
    List<Map<String, Object>> getDeptRanking(@Param("startTime") String startTime,
                                              @Param("endTime")   String endTime);

    /**
     * 获取指定日期范围内的预警记录
     */
    @Select("SELECT TOP (#{limit}) " +
            "w.id, " +
            "w.warning_type, " +
            "ISNULL(e.emp_name, w.user_code) AS real_name, " +
            "ISNULL(e.emp_code, w.user_code) AS emp_code, " +
            "w.indicator_name, " +
            "w.indicator_value, " +
            "w.warning_level, " +
            "w.is_handled, " +
            "w.create_time " +
            "FROM v_warning_record w " +
            "LEFT JOIN employee e ON w.user_code = e.emp_code " +
            "WHERE w.create_time >= CONVERT(date, #{startTime}) " +
            "AND   w.create_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "ORDER BY w.create_time DESC")
    List<Map<String, Object>> getWarningsByRange(@Param("limit")     int    limit,
                                                 @Param("startTime") String startTime,
                                                 @Param("endTime")   String endTime);

    /** 指定日期心率排行（偏离正常范围最大的排前面） */
    @Select("SELECT TOP 20 " +
            "ISNULL(e.emp_name, hr.user_code) AS empName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "CAST(ROUND(AVG(CAST(hr.heart_rate AS FLOAT)), 0) AS INT) AS avgHeartRate " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.record_time >= CONVERT(DATETIME, #{date}) " +
            "AND hr.record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{date})) " +
            "AND hr.heart_rate IS NOT NULL " +
            "GROUP BY hr.user_code, e.emp_name, d.dept_name " +
            "ORDER BY ABS(AVG(CAST(hr.heart_rate AS FLOAT)) - 80) DESC")
    List<Map<String, Object>> getDayHeartRateRank(@Param("date") String date);

    /** 指定日期血氧排行（最低血氧排前面） */
    @Select("SELECT TOP 20 " +
            "ISNULL(e.emp_name, hr.user_code) AS empName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "ROUND(AVG(CAST(hr.blood_oxygen AS FLOAT)), 1) AS avgBloodOxygen " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.record_time >= CONVERT(DATETIME, #{date}) " +
            "AND hr.record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{date})) " +
            "AND hr.blood_oxygen IS NOT NULL " +
            "GROUP BY hr.user_code, e.emp_name, d.dept_name " +
            "ORDER BY AVG(CAST(hr.blood_oxygen AS FLOAT)) ASC")
    List<Map<String, Object>> getDayBloodOxygenRank(@Param("date") String date);

    /** 指定日期步数排行（步数最少排前面） */
    @Select("SELECT TOP 20 " +
            "ISNULL(e.emp_name, hr.user_code) AS empName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "CAST(ROUND(AVG(CAST(hr.steps AS FLOAT)), 0) AS INT) AS avgSteps " +
            "FROM v_health_record hr " +
            "LEFT JOIN employee e ON hr.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.record_time >= CONVERT(DATETIME, #{date}) " +
            "AND hr.record_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{date})) " +
            "AND hr.steps IS NOT NULL " +
            "GROUP BY hr.user_code, e.emp_name, d.dept_name " +
            "ORDER BY AVG(CAST(hr.steps AS FLOAT)) ASC")
    List<Map<String, Object>> getDayStepsRank(@Param("date") String date);

    /** 指定日期预警列表（严重程度高的排前面） */
    @Select("SELECT TOP 50 " +
            "ISNULL(e.emp_name, w.user_code) AS empName, " +
            "ISNULL(d.dept_name, '') AS deptName, " +
            "w.warning_type AS warningType, " +
            "w.indicator_name AS indicatorName, " +
            "w.indicator_value AS warningValue, " +
            "w.warning_level AS warningLevel, " +
            "w.create_time AS createTime " +
            "FROM v_warning_record w " +
            "LEFT JOIN employee e ON w.user_code = e.emp_code " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE w.create_time >= CONVERT(DATETIME, #{date}) " +
            "AND w.create_time <  DATEADD(DAY, 1, CONVERT(DATETIME, #{date})) " +
            "ORDER BY CASE w.warning_level " +
            "  WHEN '危急' THEN 5 WHEN '高危' THEN 4 " +
            "  WHEN '高'   THEN 4 WHEN '危险' THEN 3 " +
            "  WHEN '中'   THEN 2 WHEN '警告' THEN 2 " +
            "  WHEN '低'   THEN 1 ELSE 0 END DESC, w.create_time DESC")
    List<Map<String, Object>> getDayWarnings(@Param("date") String date);

    /**
     * 获取指定日期范围内各指标的检测人数（DISTINCT user_code）
     * 使用 CTE 先 GROUP BY user_code 再 SUM，避免多次 COUNT DISTINCT 全表扫描（原 ~2.3s → CTE ~180ms）
     */
    @Select(";WITH user_flags AS ( " +
            "  SELECT user_code, " +
            "    MAX(CASE WHEN heart_rate   IS NOT NULL THEN 1 ELSE 0 END) AS has_hr, " +
            "    MAX(CASE WHEN blood_oxygen IS NOT NULL THEN 1 ELSE 0 END) AS has_bo, " +
            "    MAX(CASE WHEN steps        IS NOT NULL THEN 1 ELSE 0 END) AS has_st, " +
            "    MAX(CASE WHEN temperature  IS NOT NULL THEN 1 ELSE 0 END) AS has_tp, " +
            "    MAX(CASE WHEN pressure     IS NOT NULL THEN 1 ELSE 0 END) AS has_pr " +
            "  FROM v_health_record " +
            "  WHERE record_time >= CONVERT(date, #{startTime}) " +
            "  AND   record_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "  GROUP BY user_code " +
            ") " +
            "SELECT SUM(has_hr) AS heartRate, SUM(has_bo) AS bloodOxygen, " +
            "SUM(has_st) AS steps, SUM(has_tp) AS temperature, " +
            "SUM(has_pr) AS pressure, COUNT(*) AS totalPersons " +
            "FROM user_flags")
    Map<String, Object> getPersonCountsByRange(@Param("startTime") String startTime,
                                               @Param("endTime")   String endTime);

    /**
     * 优化版：直接查分区表，避免 v_health_record UNION ALL 全扫描
     */
    @Select(";WITH user_flags AS ( " +
            "  SELECT user_code, " +
            "    MAX(CASE WHEN heart_rate   IS NOT NULL THEN 1 ELSE 0 END) AS has_hr, " +
            "    MAX(CASE WHEN blood_oxygen IS NOT NULL THEN 1 ELSE 0 END) AS has_bo, " +
            "    MAX(CASE WHEN steps        IS NOT NULL THEN 1 ELSE 0 END) AS has_st, " +
            "    MAX(CASE WHEN temperature  IS NOT NULL THEN 1 ELSE 0 END) AS has_tp, " +
            "    MAX(CASE WHEN pressure     IS NOT NULL THEN 1 ELSE 0 END) AS has_pr " +
            "  FROM ${healthSource} " +
            "  WHERE record_time >= CONVERT(date, #{startTime}) " +
            "  AND   record_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "  GROUP BY user_code " +
            ") " +
            "SELECT SUM(has_hr) AS heartRate, SUM(has_bo) AS bloodOxygen, " +
            "SUM(has_st) AS steps, SUM(has_tp) AS temperature, " +
            "SUM(has_pr) AS pressure, COUNT(*) AS totalPersons " +
            "FROM user_flags")
    Map<String, Object> getPersonCountsByRangeDirect(@Param("healthSource") String healthSource,
                                                     @Param("startTime")    String startTime,
                                                     @Param("endTime")      String endTime);

    /**
     * 获取指定日期范围内各部门每日检测人数（用于弹窗折线图）
     */
    @Select("SELECT d.dept_name AS deptName, " +
            "CONVERT(VARCHAR(10), hr.record_time, 120) AS day, " +
            "COUNT(DISTINCT e.emp_code) AS personCount " +
            "FROM v_health_record hr " +
            "INNER JOIN employee e  ON hr.user_code = e.emp_code " +
            "INNER JOIN department d ON e.dept_id   = d.id " +
            "WHERE hr.record_time >= CONVERT(date, #{startTime}) " +
            "AND   hr.record_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "GROUP BY d.dept_name, CONVERT(VARCHAR(10), hr.record_time, 120) " +
            "ORDER BY d.dept_name, day")
    List<Map<String, Object>> getDeptDailyPersons(@Param("startTime") String startTime,
                                                   @Param("endTime")   String endTime);

    /**
     * 各部门检测人数 + 异常人数（用于部门综合看板图表）
     * 使用 CTE 分别聚合两张 UNION-ALL 视图，避免跨视图 JOIN 全表扫描（原写法 ~10s，CTE ~400ms）
     */
    @Select(";WITH dept_persons AS ( " +
            "  SELECT d.dept_name AS deptName, COUNT(DISTINCT hr.user_code) AS personCount " +
            "  FROM v_health_record hr " +
            "  INNER JOIN employee e   ON hr.user_code = e.emp_code " +
            "  INNER JOIN department d ON e.dept_id    = d.id " +
            "  WHERE hr.record_time >= CONVERT(date, #{startTime}) " +
            "  AND   hr.record_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "  GROUP BY d.dept_name " +
            "), " +
            "dept_abnormal AS ( " +
            "  SELECT d.dept_name AS deptName, COUNT(DISTINCT wr.user_code) AS abnormalPersonCount " +
            "  FROM v_warning_record wr " +
            "  INNER JOIN employee e   ON wr.user_code = e.emp_code " +
            "  INNER JOIN department d ON e.dept_id    = d.id " +
            "  WHERE wr.create_time >= CONVERT(date, #{startTime}) " +
            "  AND   wr.create_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "  GROUP BY d.dept_name " +
            ") " +
            "SELECT dp.deptName, dp.personCount, " +
            "ISNULL(da.abnormalPersonCount, 0) AS abnormalPersonCount " +
            "FROM dept_persons dp " +
            "LEFT JOIN dept_abnormal da ON dp.deptName = da.deptName " +
            "ORDER BY dp.personCount DESC")
    List<Map<String, Object>> getDeptPersonStats(@Param("startTime") String startTime,
                                                  @Param("endTime")   String endTime);

    // 直接查分区表，避免扫 UNION ALL 视图（由 Controller 传入具体表名）
    @Select(";WITH dept_persons AS ( " +
            "  SELECT d.dept_name AS deptName, COUNT(DISTINCT hr.user_code) AS personCount " +
            "  FROM ${healthSource} hr " +
            "  INNER JOIN employee e   ON hr.user_code = e.emp_code " +
            "  INNER JOIN department d ON e.dept_id    = d.id " +
            "  WHERE hr.record_time >= CONVERT(date, #{startTime}) " +
            "  AND   hr.record_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "  GROUP BY d.dept_name " +
            "), " +
            "dept_abnormal AS ( " +
            "  SELECT d.dept_name AS deptName, COUNT(DISTINCT wr.user_code) AS abnormalPersonCount " +
            "  FROM ${warningSource} wr " +
            "  INNER JOIN employee e   ON wr.user_code = e.emp_code " +
            "  INNER JOIN department d ON e.dept_id    = d.id " +
            "  WHERE wr.create_time >= CONVERT(date, #{startTime}) " +
            "  AND   wr.create_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "  GROUP BY d.dept_name " +
            ") " +
            "SELECT dp.deptName, dp.personCount, " +
            "ISNULL(da.abnormalPersonCount, 0) AS abnormalPersonCount " +
            "FROM dept_persons dp " +
            "LEFT JOIN dept_abnormal da ON dp.deptName = da.deptName " +
            "ORDER BY dp.personCount DESC")
    List<Map<String, Object>> getDeptPersonStatsDirect(@Param("healthSource")  String healthSource,
                                                        @Param("warningSource") String warningSource,
                                                        @Param("startTime")     String startTime,
                                                        @Param("endTime")       String endTime);

    /**
     * 单部门每日检测人数 + 异常人数（点击看板弹窗折线图用）
     */
    @Select(";WITH dept_persons AS ( " +
            "  SELECT CONVERT(VARCHAR(10), hr.record_time, 120) AS day, " +
            "  COUNT(DISTINCT e.emp_code) AS personCount " +
            "  FROM v_health_record hr " +
            "  INNER JOIN employee e   ON hr.user_code = e.emp_code " +
            "  INNER JOIN department d ON e.dept_id    = d.id " +
            "  WHERE d.dept_name = #{deptName} " +
            "  AND hr.record_time >= CONVERT(date, #{startTime}) " +
            "  AND hr.record_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "  GROUP BY CONVERT(VARCHAR(10), hr.record_time, 120) " +
            "), " +
            "dept_abnormal AS ( " +
            "  SELECT CONVERT(VARCHAR(10), wr.create_time, 120) AS day, " +
            "  COUNT(DISTINCT wr.user_code) AS abnormalPersonCount " +
            "  FROM v_warning_record wr " +
            "  INNER JOIN employee e   ON wr.user_code = e.emp_code " +
            "  INNER JOIN department d ON e.dept_id    = d.id " +
            "  WHERE d.dept_name = #{deptName} " +
            "  AND wr.create_time >= CONVERT(date, #{startTime}) " +
            "  AND wr.create_time <  DATEADD(DAY, 1, CONVERT(date, #{endTime})) " +
            "  GROUP BY CONVERT(VARCHAR(10), wr.create_time, 120) " +
            ") " +
            "SELECT dp.day, dp.personCount, ISNULL(da.abnormalPersonCount, 0) AS abnormalPersonCount " +
            "FROM dept_persons dp " +
            "LEFT JOIN dept_abnormal da ON dp.day = da.day " +
            "ORDER BY dp.day")
    List<Map<String, Object>> getDeptDailyDetail(@Param("deptName") String deptName,
                                                  @Param("startTime") String startTime,
                                                  @Param("endTime")   String endTime);

    /**
     * 指标每日检测人数 + 异常人数（点击指标卡片弹窗折线图用）
     */
    @SelectProvider(type = MetricDailySqlProvider.class, method = "getMetricDailyDetail")
    List<Map<String, Object>> getMetricDailyDetail(@Param("metricType") String metricType,
                                                    @Param("startTime")  String startTime,
                                                    @Param("endTime")    String endTime);

    /**
     * 班前健康达标率：今日有记录的员工中，最新一条记录符合准入标准的比例
     * 准入标准：心率60-100，血氧≥95，血压高<140，血压低<90
     */
    @SelectProvider(type = MetricDailySqlProvider.class, method = "getTodayPreShiftCompliance")
    Map<String, Object> getTodayPreShiftCompliance();

    /**
     * 入井准入名单：今日有健康记录的所有员工及其最新体征和准入状态
     */
    @SelectProvider(type = MetricDailySqlProvider.class, method = "getTodayMineEntryList")
    List<Map<String, Object>> getTodayMineEntryList(@Param("size") int size);

    /**
     * 部门健康对比：各部门近N天的多维均值
     */
    @Select("SELECT " +
            "  d.dept_name AS deptName, " +
            "  COUNT(DISTINCT hr.user_code) AS memberCount, " +
            "  ROUND(AVG(CAST(hr.heart_rate AS FLOAT)), 1) AS avgHeartRate, " +
            "  ROUND(AVG(CAST(hr.blood_oxygen AS FLOAT)), 1) AS avgBloodOxygen, " +
            "  ROUND(AVG(CAST(hr.blood_pressure_high AS FLOAT)), 1) AS avgSystolic, " +
            "  ROUND(AVG(CAST(hr.sleep_minutes AS FLOAT)), 0) AS avgSleepMinutes, " +
            "  ROUND(AVG(CAST(hr.steps AS FLOAT)), 0) AS avgSteps, " +
            "  ROUND(AVG(CAST(hr.pressure AS FLOAT)), 1) AS avgPressure " +
            "FROM v_health_record hr " +
            "JOIN employee e ON hr.user_code = e.emp_code " +
            "JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.record_time >= DATEADD(DAY, -#{days}, GETDATE()) " +
            "GROUP BY d.dept_name " +
            "HAVING COUNT(DISTINCT hr.user_code) >= 2 " +
            "ORDER BY memberCount DESC")
    List<Map<String, Object>> getDeptHealthComparison(@Param("days") int days);

    /**
     * 部门健康对比 — 直接查分区表，避免扫 v_health_record UNION ALL 视图
     * tableSource = "health_record_YYYYMM" 或 "(SELECT ... FROM t1 UNION ALL SELECT ... FROM t2) _dh"
     */
    @Select("SELECT " +
            "  d.dept_name AS deptName, " +
            "  COUNT(DISTINCT hr.user_code) AS memberCount, " +
            "  ROUND(AVG(CAST(hr.heart_rate AS FLOAT)), 1) AS avgHeartRate, " +
            "  ROUND(AVG(CAST(hr.blood_oxygen AS FLOAT)), 1) AS avgBloodOxygen, " +
            "  ROUND(AVG(CAST(hr.blood_pressure_high AS FLOAT)), 1) AS avgSystolic, " +
            "  ROUND(AVG(CAST(hr.sleep_minutes AS FLOAT)), 0) AS avgSleepMinutes, " +
            "  ROUND(AVG(CAST(hr.steps AS FLOAT)), 0) AS avgSteps, " +
            "  ROUND(AVG(CAST(hr.pressure AS FLOAT)), 1) AS avgPressure " +
            "FROM ${tableSource} hr " +
            "JOIN employee e ON hr.user_code = e.emp_code " +
            "JOIN department d ON e.dept_id = d.id " +
            "WHERE hr.record_time >= DATEADD(DAY, -#{days}, GETDATE()) " +
            "GROUP BY d.dept_name " +
            "HAVING COUNT(DISTINCT hr.user_code) >= 2 " +
            "ORDER BY memberCount DESC")
    List<Map<String, Object>> getDeptHealthComparisonDirect(@Param("tableSource") String tableSource,
                                                             @Param("days") int days);
}
