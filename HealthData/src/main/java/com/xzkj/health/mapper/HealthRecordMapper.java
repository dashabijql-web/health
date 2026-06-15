package com.xzkj.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzkj.health.model.HealthRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 健康记录Mapper接口
 *
 * 分表说明：
 *   - 读：继续使用视图 v_health_record（UNION ALL 所有月份表）
 *   - 写：insertToTable() 直接写入对应月份表，由 Service 层调用 TableNameUtil 计算表名
 */
@Mapper
public interface HealthRecordMapper extends BaseMapper<HealthRecord> {

    // ─── 写入：动态月份表 ─────────────────────────────────────────────

    /**
     * 向指定月份表插入一条健康记录。
     *
     * 由 HealthRecordServiceImpl.save() 调用，tableName 由 TableNameUtil 计算。
     * 使用 <if> 跳过 null 字段，行为与 MyBatis-Plus 自动生成的 INSERT 一致。
     * 注意：${tableName} 使用字符串替换，表名由内部工具类生成，不存在注入风险。
     *
     * 字段映射说明：
     *   record.time (String "yyyy-MM-dd HH:mm:ss") → 月份表 record_time 列
     *   月份表的 create_time / update_time 由数据库 DEFAULT GETDATE() 自动填充
     */
    @Insert("<script>" +
            "INSERT INTO ${tableName} " +
            "<trim prefix='(' suffix=')' suffixOverrides=','>" +
            "  user_code," +
            "  <if test='r.heartRate != null'>heart_rate,</if>" +
            "  <if test='r.bloodOxygen != null'>blood_oxygen,</if>" +
            "  <if test='r.sleepMinutes != null'>sleep_minutes,</if>" +
            "  <if test='r.steps != null'>steps,</if>" +
            "  <if test='r.pressure != null'>pressure,</if>" +
            "  <if test='r.temperature != null'>temperature,</if>" +
            "  <if test='r.bloodPressureHigh != null'>blood_pressure_high,</if>" +
            "  <if test='r.bloodPressureLow != null'>blood_pressure_low,</if>" +
            "  <if test='r.calories != null'>calories,</if>" +
            "  <if test='r.time != null'>record_time</if>" +
            "</trim>" +
            " VALUES " +
            "<trim prefix='(' suffix=')' suffixOverrides=','>" +
            "  #{r.userCode}," +
            "  <if test='r.heartRate != null'>#{r.heartRate},</if>" +
            "  <if test='r.bloodOxygen != null'>#{r.bloodOxygen},</if>" +
            "  <if test='r.sleepMinutes != null'>#{r.sleepMinutes},</if>" +
            "  <if test='r.steps != null'>#{r.steps},</if>" +
            "  <if test='r.pressure != null'>#{r.pressure},</if>" +
            "  <if test='r.temperature != null'>#{r.temperature},</if>" +
            "  <if test='r.bloodPressureHigh != null'>#{r.bloodPressureHigh},</if>" +
            "  <if test='r.bloodPressureLow != null'>#{r.bloodPressureLow},</if>" +
            "  <if test='r.calories != null'>#{r.calories},</if>" +
            "  <if test='r.time != null'>#{r.time}</if>" +
            "</trim>" +
            "</script>")
    int insertToTable(@Param("tableName") String tableName, @Param("r") HealthRecord record);

    // ─── 读取：通过视图 v_health_record ──────────────────────────────

    /**
     * 自定义查询：根据用户代码查询记录（查视图，涵盖所有月份）
     */
    @Select("SELECT * FROM v_health_record WHERE user_code = #{userCode} ORDER BY record_time DESC")
    List<HealthRecord> selectByUserCode(@Param("userCode") String userCode);

    /**
     * 自定义查询：按时间范围查询（查视图）
     */
    @Select("SELECT * FROM v_health_record WHERE record_time BETWEEN #{startTime} AND #{endTime}")
    List<HealthRecord> selectByTimeRange(@Param("startTime") String startTime,
                                         @Param("endTime") String endTime);

    /**
     * 自定义查询：查询异常心率（heart_rate < 60 或 > 100），查视图
     */
    @Select("SELECT * FROM v_health_record WHERE heart_rate < 60 OR heart_rate > 100")
    List<HealthRecord> selectAbnormalHeartRate();

    /**
     * 无过滤分页优化：按月分区表行数元数据计算总量，避免在 v_health_record 上做 COUNT/排序全扫。
     */
    @Select("SELECT COALESCE(SUM(CAST(rows AS BIGINT)), 0) " +
            "FROM sys.partitions " +
            "WHERE object_id = OBJECT_ID(#{tableName}) AND index_id IN (0, 1)")
    Long countRowsByTableName(@Param("tableName") String tableName);

    /**
     * 无过滤分页优化：对单月表或少量月表 UNION ALL 子查询做分页，避免直接排序整个 v_health_record 视图。
     */
    @Select("SELECT " +
            "id, heart_rate, blood_oxygen, sleep_minutes, steps, calories, pressure, " +
            "blood_pressure_high, blood_pressure_low, temperature, user_code, " +
            "CONVERT(VARCHAR(19), record_time, 120) AS record_time " +
            "FROM ${tableSource} " +
            "ORDER BY record_time DESC " +
            "OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY")
    List<HealthRecord> selectPageFromSource(@Param("tableSource") String tableSource,
                                            @Param("offset") long offset,
                                            @Param("size") long size);

    /**
     * 自定义查询：统计每个用户的记录数量（查视图）
     */
    @Select("SELECT user_code, COUNT(*) as record_count FROM v_health_record GROUP BY user_code")
    List<Map<String, Object>> countByUser();

    /**
     * 自定义查询：获取用户最新的健康记录（查视图）
     */
    @Select("SELECT TOP 1 * FROM v_health_record WHERE user_code = #{userCode} ORDER BY record_time DESC")
    HealthRecord selectLatestByUserCode(@Param("userCode") String userCode);

    /**
     * 自定义查询：获取平均心率最高的前N个用户（查视图）
     */
    @Select("SELECT TOP #{limit} user_code, AVG(heart_rate) as avg_heart_rate " +
            "FROM v_health_record " +
            "GROUP BY user_code " +
            "ORDER BY avg_heart_rate DESC")
    List<Map<String, Object>> selectTopHeartRateUsers(@Param("limit") int limit);
}
