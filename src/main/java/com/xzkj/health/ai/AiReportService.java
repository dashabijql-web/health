package com.xzkj.health.ai;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI 健康诊断报告服务
 *
 * 流程：
 *   1. 查询员工基本信息（姓名、部门、岗位、年龄）
 *   2. 查询近30天体征平均值（心率、血氧、血压、体温、睡眠、步数、卡路里、压力）
 *   3. 查询近30天预警汇总
 *   4. 将以上数据拼成 Prompt 传给 DeepSeek，生成 Markdown 格式诊断报告
 */
@Slf4j
@Service
public class AiReportService {

    @Autowired
    private SqlExecutorMapper sqlExecutorMapper;

    @Autowired
    private DeepSeekClient deepSeekClient;

    private static final String REPORT_SYSTEM_PROMPT = """
            你是一名专业的煤矿职业健康医师，擅长根据体征数据出具健康评估报告。
            请根据提供的员工健康数据，生成一份详细、专业且易于理解的健康诊断报告。

            【报告格式要求 - 严格遵守】
            使用 Markdown 格式，结构如下：

            ## 🏥 健康诊断报告

            **员工：** 姓名 | **部门：** 部门名 | **岗位：** 岗位名 | **年龄：** XX岁

            ---

            ## 📊 体征数据概览（近30天）

            | 指标 | 均值 | 参考范围 | 状态 |
            |------|------|---------|------|
            | 心率 | XX bpm | 60~100 bpm | ✅正常 / ⚠️偏高 / 🔴异常 |
            ...（其他指标）

            ---

            ## 🔍 重点分析

            逐项分析异常或需关注的指标，说明可能原因及影响。

            ---

            ## ⚠️ 预警记录（近30天）

            列出预警类型和次数，如无则写"近30天无预警记录"。

            ---

            ## 💡 健康建议

            针对该员工具体数据，给出3-5条具体、可执行的健康改善建议。

            ---

            ## 🎯 综合评分

            **健康评分：XX / 100**（根据各项指标偏离正常范围的程度综合打分）

            > 本报告基于近30天体征数据自动生成，仅供参考，不替代专业医疗诊断。

            【内容要求】
            - 异常值用 ⚠️ 或 🔴 标注，正常用 ✅
            - 心率正常范围 60~100 bpm；血氧正常 ≥ 95%；体温正常 36.0~37.5℃
            - 收缩压正常 < 140 mmHg；舒张压正常 < 90 mmHg
            - 睡眠正常 360~480 分钟（6~8小时）；压力正常 < 60
            - 数值保留1位小数
            - 煤矿工人职业特点：高体力劳动，需关注心肺功能和疲劳积累
            """;

    private static final String DEPT_REPORT_SYSTEM_PROMPT = """
            你是一名专业的煤矿职业健康医师，擅长根据部门整体体征数据出具健康评估报告。
            请根据提供的部门健康数据，生成一份详细、专业且易于理解的部门健康诊断报告。

            【报告格式要求】
            使用 Markdown 格式：

            ## 🏥 部门健康诊断报告

            **部门：** 部门名 | **分析周期：** 近30天

            ---

            ## 📊 体征均值概览（近30天）

            | 指标 | 均值 | 参考范围 | 状态 |
            |------|------|---------|------|
            | 心率 | XX bpm | 60~100 | ✅/⚠️/🔴 |
            ...

            ---

            ## 🔍 重点分析

            分析部门整体健康状况，说明突出的风险点和良好点。

            ---

            ## ⚠️ 预警情况（近30天）

            汇总预警频次和类型分布。

            ---

            ## 💡 管理建议

            针对部门整体数据，给出3-5条具体的健康管理改进建议。

            ---

            ## 🎯 部门健康评分

            **健康评分：XX / 100**

            > 本报告基于近30天体征数据自动生成，仅供参考。
            """;

    /**
     * 生成部门健康诊断报告
     */
    public String generateDepartmentReport(String deptName) {
        log.info("生成部门健康报告: {}", deptName);

        // Step 1: 查询部门基本信息
        String deptInfoSql = String.format(
            "SELECT d.id, d.dept_name, d.risk_level, COUNT(e.id) AS emp_count " +
            "FROM department d LEFT JOIN employee e ON e.dept_id = d.id " +
            "WHERE d.dept_name = '%s' GROUP BY d.id, d.dept_name, d.risk_level", deptName
        );
        List<Map<String, Object>> deptInfo = sqlExecutorMapper.executeQuery(deptInfoSql);
        if (deptInfo.isEmpty()) return "未找到部门：" + deptName;

        // Step 2: 查询部门近30天体征均值
        String healthSql = String.format(
            "SELECT AVG(CAST(h.heart_rate AS FLOAT)) AS avg_heart_rate, " +
            "MIN(h.heart_rate) AS min_heart_rate, MAX(h.heart_rate) AS max_heart_rate, " +
            "AVG(CAST(h.blood_oxygen AS FLOAT)) AS avg_blood_oxygen, " +
            "MIN(h.blood_oxygen) AS min_blood_oxygen, " +
            "AVG(CAST(h.blood_pressure_high AS FLOAT)) AS avg_bp_high, " +
            "AVG(CAST(h.blood_pressure_low AS FLOAT)) AS avg_bp_low, " +
            "AVG(CAST(h.temperature AS FLOAT)) AS avg_temperature, " +
            "AVG(CAST(h.sleep_minutes AS FLOAT)) AS avg_sleep_minutes, " +
            "AVG(CAST(h.steps AS FLOAT)) AS avg_steps, " +
            "AVG(CAST(h.calories AS FLOAT)) AS avg_calories, " +
            "AVG(CAST(h.pressure AS FLOAT)) AS avg_pressure, " +
            "COUNT(DISTINCT h.user_code) AS monitored_emp_count, COUNT(*) AS record_count " +
            "FROM v_health_record h " +
            "JOIN employee e ON h.user_code = e.emp_code " +
            "JOIN department d ON e.dept_id = d.id " +
            "WHERE d.dept_name = '%s' " +
            "  AND h.record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "  AND h.heart_rate IS NOT NULL AND h.heart_rate > 0", deptName
        );
        List<Map<String, Object>> healthData = sqlExecutorMapper.executeQuery(healthSql);

        // Step 3: 查询近30天预警汇总
        String warningSql = String.format(
            "SELECT w.warning_type, w.warning_level, COUNT(*) AS cnt " +
            "FROM v_warning_record w " +
            "JOIN employee e ON w.user_code = e.emp_code " +
            "JOIN department d ON e.dept_id = d.id " +
            "WHERE d.dept_name = '%s' " +
            "  AND w.create_time >= DATEADD(DAY, -30, GETDATE()) " +
            "GROUP BY w.warning_type, w.warning_level ORDER BY cnt DESC", deptName
        );
        List<Map<String, Object>> warnings;
        try { warnings = sqlExecutorMapper.executeQuery(warningSql); }
        catch (Exception e) { warnings = List.of(); }

        String userMessage = String.format(
            "请根据以下数据生成部门健康诊断报告：\n\n" +
            "【部门基本信息】\n%s\n\n" +
            "【近30天体征数据（全部门汇总均值）】\n%s\n\n" +
            "【近30天预警记录】\n%s",
            JSON.toJSONString(deptInfo.get(0)),
            healthData.isEmpty() ? "暂无体征数据" : JSON.toJSONString(healthData.get(0)),
            warnings.isEmpty() ? "无预警记录" : JSON.toJSONString(warnings)
        );

        log.info("调用 DeepSeek 生成部门报告...");
        return deepSeekClient.chat(DEPT_REPORT_SYSTEM_PROMPT, userMessage);
    }

    /**
     * 生成员工健康诊断报告
     *
     * @param empCode 员工工号（如 EMP001）
     * @return Markdown 格式的报告文本
     */
    public String generateEmployeeReport(String empCode) {
        log.info("生成员工健康报告: {}", empCode);

        // Step 1: 查询员工基本信息
        String empInfoSql = String.format(
            "SELECT e.emp_name, e.emp_code, e.gender, e.birth_date, e.hire_date, " +
            "d.dept_name, j.type_name AS job_name " +
            "FROM employee e " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "LEFT JOIN job_type j ON e.job_type_id = j.id " +
            "WHERE e.emp_code = '%s'", empCode
        );
        List<Map<String, Object>> empInfo = sqlExecutorMapper.executeQuery(empInfoSql);
        if (empInfo.isEmpty()) {
            return "未找到员工工号 " + empCode + " 的信息";
        }

        // Step 2: 查询近30天体征均值
        String healthSql = String.format(
            "SELECT " +
            "  AVG(CAST(heart_rate AS FLOAT)) AS avg_heart_rate, " +
            "  MIN(heart_rate) AS min_heart_rate, MAX(heart_rate) AS max_heart_rate, " +
            "  AVG(CAST(blood_oxygen AS FLOAT)) AS avg_blood_oxygen, " +
            "  MIN(blood_oxygen) AS min_blood_oxygen, " +
            "  AVG(CAST(blood_pressure_high AS FLOAT)) AS avg_bp_high, " +
            "  MAX(blood_pressure_high) AS max_bp_high, " +
            "  AVG(CAST(blood_pressure_low AS FLOAT)) AS avg_bp_low, " +
            "  MAX(blood_pressure_low) AS max_bp_low, " +
            "  AVG(CAST(temperature AS FLOAT)) AS avg_temperature, " +
            "  MAX(temperature) AS max_temperature, " +
            "  AVG(CAST(sleep_minutes AS FLOAT)) AS avg_sleep_minutes, " +
            "  MIN(sleep_minutes) AS min_sleep_minutes, " +
            "  AVG(CAST(steps AS FLOAT)) AS avg_steps, " +
            "  AVG(CAST(calories AS FLOAT)) AS avg_calories, " +
            "  AVG(CAST(pressure AS FLOAT)) AS avg_pressure, " +
            "  MAX(pressure) AS max_pressure, " +
            "  COUNT(*) AS record_count " +
            "FROM v_health_record " +
            "WHERE user_code = '%s' " +
            "  AND record_time >= DATEADD(DAY, -30, GETDATE()) " +
            "  AND heart_rate IS NOT NULL AND heart_rate > 0", empCode
        );
        List<Map<String, Object>> healthData = sqlExecutorMapper.executeQuery(healthSql);

        // Step 3: 查询近30天预警汇总
        String warningSql = String.format(
            "SELECT warning_type, warning_level, COUNT(*) AS cnt " +
            "FROM v_warning_record " +
            "WHERE user_code = '%s' " +
            "  AND create_time >= DATEADD(DAY, -30, GETDATE()) " +
            "GROUP BY warning_type, warning_level " +
            "ORDER BY cnt DESC", empCode
        );
        List<Map<String, Object>> warnings;
        try {
            warnings = sqlExecutorMapper.executeQuery(warningSql);
        } catch (Exception e) {
            log.warn("查询预警记录失败: {}", e.getMessage());
            warnings = List.of();
        }

        // Step 4: 组装 Prompt
        String userMessage = String.format(
            "请根据以下数据生成健康诊断报告：\n\n" +
            "【员工基本信息】\n%s\n\n" +
            "【近30天体征数据】\n%s\n\n" +
            "【近30天预警记录】\n%s",
            JSON.toJSONString(empInfo.get(0)),
            healthData.isEmpty() ? "暂无体征数据" : JSON.toJSONString(healthData.get(0)),
            warnings.isEmpty() ? "无预警记录" : JSON.toJSONString(warnings)
        );

        log.info("调用 DeepSeek 生成报告...");
        String report = deepSeekClient.chat(REPORT_SYSTEM_PROMPT, userMessage);
        log.info("报告生成完毕: {} chars", report.length());
        return report;
    }
}
