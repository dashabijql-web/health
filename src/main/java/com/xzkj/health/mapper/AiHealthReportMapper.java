package com.xzkj.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzkj.health.model.AiHealthReport;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface AiHealthReportMapper extends BaseMapper<AiHealthReport> {

    /** 查询未过期的缓存报告 */
    @Select("SELECT TOP 1 * FROM ai_health_report " +
            "WHERE emp_code = #{empCode} AND expires_at > GETDATE() " +
            "ORDER BY generate_time DESC")
    AiHealthReport findValidCachedReport(@Param("empCode") String empCode);

    /** 删除员工的历史报告（插入前清理） */
    @Delete("DELETE FROM ai_health_report WHERE emp_code = #{empCode}")
    void deleteByEmpCode(@Param("empCode") String empCode);

    /** 30天健康数据聚合（用于拼装AI prompt） */
    @Select("SELECT " +
            "COUNT(*) AS recordCount, " +
            "ROUND(AVG(CAST(heart_rate AS FLOAT)), 1) AS avgHeartRate, " +
            "MAX(heart_rate) AS maxHeartRate, " +
            "MIN(heart_rate) AS minHeartRate, " +
            "ROUND(AVG(CAST(blood_oxygen AS FLOAT)), 1) AS avgBloodOxygen, " +
            "MIN(blood_oxygen) AS minBloodOxygen, " +
            "ROUND(AVG(CAST(temperature AS FLOAT)) / 10.0, 1) AS avgTemperature, " +
            "ROUND(AVG(CAST(sleep_minutes AS FLOAT)) / 60.0, 1) AS avgSleepHours " +
            "FROM v_health_record " +
            "WHERE user_code = #{empCode} " +
            "AND record_time >= DATEADD(DAY, -30, GETDATE())")
    Map<String, Object> get30DayHealthStats(@Param("empCode") String empCode);

    /** 30天预警统计 */
    @Select("SELECT " +
            "COUNT(*) AS totalWarnings, " +
            "SUM(CASE WHEN warning_level IN ('危急','高危') THEN 1 ELSE 0 END) AS highRiskCount " +
            "FROM v_warning_record " +
            "WHERE user_code = #{empCode} " +
            "AND create_time >= DATEADD(DAY, -30, GETDATE())")
    Map<String, Object> get30DayWarningStats(@Param("empCode") String empCode);
}
