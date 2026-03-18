package com.xzkj.health.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface HealthPortraitMapper {

    @Select("SELECT e.emp_name AS empName, e.emp_code AS empCode, " +
            "e.gender, e.phone, e.birth_date AS birthDate, e.hire_date AS hireDate, " +
            "e.height, e.weight, e.blood_type AS bloodType, e.status, " +
            "d.dept_name AS deptName, j.type_name AS jobTypeName " +
            "FROM employee e " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "LEFT JOIN job_type j ON e.job_type_id = j.id " +
            "WHERE e.emp_code = #{empCode}")
    Map<String, Object> getEmployeeDetail(@Param("empCode") String empCode);

    @Select("SELECT TOP 1 " +
            "heart_rate AS heartRate, " +
            "blood_oxygen AS bloodOxygen, " +
            "CAST(temperature AS FLOAT) / 10.0 AS temperature, " +
            "blood_pressure_high AS systolic, " +
            "blood_pressure_low AS diastolic, " +
            "pressure, steps, calories " +
            "FROM v_health_record " +
            "WHERE user_code = #{empCode} " +
            "AND record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "ORDER BY record_time DESC")
    Map<String, Object> getLatestVitals(@Param("empCode") String empCode);

    @Select("SELECT " +
            "ISNULL(MAX(steps), 0) AS todaySteps, " +
            "ISNULL(MAX(calories), 0) AS todayCalories " +
            "FROM v_health_record " +
            "WHERE user_code = #{empCode} " +
            "AND CAST(record_time AS DATE) = CAST(GETDATE() AS DATE)")
    Map<String, Object> getTodayExercise(@Param("empCode") String empCode);

    @Select("SELECT CONVERT(VARCHAR(10), record_time, 120) AS date, " +
            "ROUND(AVG(CAST(heart_rate   AS FLOAT)), 0) AS avgHeartRate, " +
            "ROUND(AVG(CAST(blood_oxygen AS FLOAT)), 1) AS avgBloodOxygen " +
            "FROM v_health_record " +
            "WHERE user_code = #{empCode} " +
            "AND record_time >= DATEADD(DAY, -6, CAST(GETDATE() AS DATE)) " +
            "AND heart_rate IS NOT NULL " +
            "GROUP BY CONVERT(VARCHAR(10), record_time, 120) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> get7DayTrend(@Param("empCode") String empCode);

    @Select("SELECT TOP 50 " +
            "warning_type AS warningType, " +
            "indicator_name AS indicatorName, " +
            "indicator_value AS warningValue, " +
            "warning_level AS warningLevel, " +
            "create_time AS createTime " +
            "FROM v_warning_record " +
            "WHERE user_code = #{empCode} " +
            "AND create_time >= DATEADD(DAY, -30, GETDATE()) " +
            "ORDER BY create_time DESC")
    List<Map<String, Object>> get30DayWarnings(@Param("empCode") String empCode);

    @Select("SELECT DATEPART(HOUR, record_time) AS hour, " +
            "AVG(CAST(heart_rate AS FLOAT)) AS avgHr " +
            "FROM v_health_record " +
            "WHERE user_code = #{empCode} " +
            "AND CAST(record_time AS DATE) = #{date} " +
            "AND heart_rate IS NOT NULL AND heart_rate > 0 " +
            "GROUP BY DATEPART(HOUR, record_time) " +
            "ORDER BY hour ASC")
    List<Map<String, Object>> getHourlyHeartRate(@Param("empCode") String empCode, @Param("date") String date);
}
