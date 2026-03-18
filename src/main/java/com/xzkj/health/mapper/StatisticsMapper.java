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
            "(SELECT COUNT(*) FROM v_warning_record w WHERE w.user_code IN (SELECT emp_code FROM employee WHERE dept_id = d.id) " +
            " AND w.create_time >= DATEADD(DAY, -30, GETDATE())) AS warning_count " +
            "FROM department d " +
            "LEFT JOIN employee e ON e.dept_id = d.id " +
            "GROUP BY d.id, d.dept_name " +
            "ORDER BY warning_count DESC")
    List<Map<String, Object>> getDeptHealthSummary();

    // 先分别聚合 health/warning 再 JOIN，避免两表笛卡尔积膨胀 + COUNT(DISTINCT) 极慢
    @Select("SELECT e.emp_code, e.emp_name, d.id AS dept_id, d.dept_name, " +
            "  h.record_count, h.avg_heart_rate, h.avg_blood_oxygen, h.avg_temperature, " +
            "  CASE WHEN ISNULL(w.warning_count,0) = 0 THEN 100 " +
            "       WHEN ISNULL(w.warning_count,0) >= h.record_count THEN 0 " +
            "       ELSE CAST(100 - ISNULL(w.warning_count,0) * 100 / h.record_count AS INT) " +
            "  END AS health_score " +
            "FROM employee e " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "JOIN ( " +
            "  SELECT user_code, COUNT(id) AS record_count, " +
            "    AVG(CAST(heart_rate AS FLOAT)) AS avg_heart_rate, " +
            "    AVG(CAST(blood_oxygen AS FLOAT)) AS avg_blood_oxygen, " +
            "    AVG(CAST(temperature AS FLOAT)) / 10.0 AS avg_temperature " +
            "  FROM ${tableName} " +
            "  WHERE record_time >= #{startDate} AND record_time < #{endDate} " +
            "  GROUP BY user_code " +
            ") h ON h.user_code = e.emp_code " +
            "LEFT JOIN ( " +
            "  SELECT user_code, COUNT(id) AS warning_count " +
            "  FROM ${warningTableName} " +
            "  WHERE create_time >= #{startDate} AND create_time < #{endDate} " +
            "  GROUP BY user_code " +
            ") w ON w.user_code = e.emp_code " +
            "WHERE (e.status IS NULL OR e.status = 0) " +
            "ORDER BY h.record_count DESC")
    List<Map<String, Object>> getMonthlySummary(@Param("tableName") String tableName,
                                                 @Param("warningTableName") String warningTableName,
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

    /**
     * 优化版：直接查分区表，避免 v_warning_record UNION ALL 全扫描
     */
    @Select("SELECT indicator_name AS name, COUNT(*) AS value " +
            "FROM ${warningTableName} " +
            "WHERE create_time >= #{startDate} AND create_time < #{endDate} " +
            "AND indicator_name IS NOT NULL " +
            "GROUP BY indicator_name " +
            "ORDER BY value DESC")
    List<Map<String, Object>> getWarningTypeCountsDirect(@Param("warningTableName") String warningTableName,
                                                         @Param("startDate")        String startDate,
                                                         @Param("endDate")          String endDate);
}
