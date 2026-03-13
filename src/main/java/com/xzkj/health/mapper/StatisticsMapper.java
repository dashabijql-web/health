package com.xzkj.health.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {

    @Select("SELECT d.id, d.dept_name, " +
            "COUNT(DISTINCT e.id) AS employee_count, " +
            "(SELECT COUNT(*) FROM v_warning_record w WHERE w.user_code IN (SELECT emp_code FROM employee WHERE dept_id = d.id)) AS warning_count " +
            "FROM department d " +
            "LEFT JOIN employee e ON e.dept_id = d.id " +
            "GROUP BY d.id, d.dept_name " +
            "ORDER BY warning_count DESC")
    List<Map<String, Object>> getDeptHealthSummary();

    // tableName = 具体分区表(如 health_record_202603)，避免 UNION ALL 全扫
    // startDate/endDate 用范围谓词，SARGable，可命中 record_time 索引
    @Select("SELECT e.emp_code, e.emp_name, d.id AS dept_id, d.dept_name, " +
            "COUNT(h.id) AS record_count, " +
            "AVG(CAST(h.heart_rate AS FLOAT)) AS avg_heart_rate, " +
            "AVG(CAST(h.blood_oxygen AS FLOAT)) AS avg_blood_oxygen, " +
            "AVG(CAST(h.temperature AS FLOAT)) / 10.0 AS avg_temperature, " +
            "CASE WHEN COUNT(h.id) = 0 THEN 75 " +
            "     WHEN COUNT(DISTINCT w.id) >= COUNT(h.id) THEN 0 " +
            "     ELSE CAST(100 - COUNT(DISTINCT w.id) * 100 / COUNT(h.id) AS INT) " +
            "END AS health_score " +
            "FROM employee e " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "LEFT JOIN ${tableName} h ON h.user_code = e.emp_code " +
            "  AND h.record_time >= #{startDate} AND h.record_time < #{endDate} " +
            "LEFT JOIN v_warning_record w ON w.user_code = e.emp_code " +
            "  AND w.create_time >= #{startDate} AND w.create_time < #{endDate} " +
            "WHERE (e.status IS NULL OR e.status = 0) " +
            "GROUP BY e.emp_code, e.emp_name, d.id, d.dept_name " +
            "HAVING COUNT(h.id) > 0 " +
            "ORDER BY record_count DESC")
    List<Map<String, Object>> getMonthlySummary(@Param("tableName") String tableName,
                                                 @Param("startDate") String startDate,
                                                 @Param("endDate") String endDate);

    /**
     * 按天统计指定月份的健康记录数（用于月度折线图）
     */
    @Select("SELECT DAY(record_time) AS day, COUNT(*) AS count " +
            "FROM ${tableName} " +
            "WHERE record_time >= #{startDate} AND record_time < #{endDate} " +
            "GROUP BY DAY(record_time) " +
            "ORDER BY DAY(record_time)")
    List<Map<String, Object>> getDailyRecordCounts(@Param("tableName") String tableName,
                                                    @Param("startDate") String startDate,
                                                    @Param("endDate") String endDate);

    /**
     * 按预警类型统计指定月份的预警数量（用于月度饼图）
     */
    @Select("SELECT indicator_name AS name, COUNT(*) AS value " +
            "FROM v_warning_record " +
            "WHERE create_time >= #{startDate} AND create_time < #{endDate} " +
            "AND indicator_name IS NOT NULL " +
            "GROUP BY indicator_name " +
            "ORDER BY value DESC")
    List<Map<String, Object>> getWarningTypeCounts(@Param("startDate") String startDate,
                                                    @Param("endDate") String endDate);
}
