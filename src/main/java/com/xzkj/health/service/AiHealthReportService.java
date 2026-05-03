package com.xzkj.health.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xzkj.health.mapper.AiHealthReportMapper;
import com.xzkj.health.mapper.HealthPortraitMapper;
import com.xzkj.health.model.AiHealthReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiHealthReportService {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-key-file:}")
    private String apiKeyFile;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.model}")
    private String model;

    @Value("${deepseek.timeout-seconds:60}")
    private int timeoutSeconds;

    @Autowired
    private AiHealthReportMapper aiReportMapper;

    @Autowired
    private HealthPortraitMapper healthPortraitMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private volatile String resolvedApiKey;

    public Map<String, Object> getCachedReportByKey(String key) {
        AiHealthReport cached = aiReportMapper.findValidCachedReportByKey(key);
        return cached != null ? toResponse(cached) : null;
    }

    /**
     * 获取缓存的报告（不生成，仅查询）
     */
    public Map<String, Object> getCachedReport(String empCode) {
        AiHealthReport cached = aiReportMapper.findValidCachedReport(empCode);
        if (cached == null) return null;
        return toResponse(cached);
    }

    /**
     * 生成报告（有效缓存直接返回，否则调用 DeepSeek）
     */
    public Map<String, Object> generateReport(String empCode, boolean force) {
        // 非强制刷新时先查缓存
        if (!force) {
            AiHealthReport cached = aiReportMapper.findValidCachedReport(empCode);
            if (cached != null) {
                log.info("AI报告命中缓存: empCode={}", empCode);
                return toResponse(cached);
            }
        }

        // 查员工基本信息
        Map<String, Object> employee = healthPortraitMapper.getEmployeeDetail(empCode);
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在: " + empCode);
        }

        // 查健康数据和预警统计
        Map<String, Object> healthStats  = aiReportMapper.get30DayHealthStats(empCode);
        Map<String, Object> warningStats = aiReportMapper.get30DayWarningStats(empCode);
        if (healthStats == null) healthStats = new LinkedHashMap<>();
        if (warningStats == null) warningStats = new LinkedHashMap<>();

        // 检查是否有足够数据
        int recordCount = getInt(healthStats, "recordCount");
        if (recordCount == 0) {
            throw new IllegalArgumentException("该员工近30天暂无健康数据，无法生成分析报告");
        }

        // 构建 prompt 并调用 DeepSeek
        String prompt = buildPrompt(employee, healthStats, warningStats);
        String reportContent = callDeepSeek(prompt);

        // 保存到数据库（先删旧的）
        aiReportMapper.deleteByEmpCode(empCode);
        AiHealthReport report = new AiHealthReport();
        report.setEmpCode(empCode);
        report.setEmpName(str(employee, "empName"));
        report.setReportContent(reportContent);
        report.setGenerateTime(LocalDateTime.now());
        report.setExpiresAt(LocalDateTime.now().plusHours(24));
        aiReportMapper.insert(report);

        log.info("AI报告生成成功: empCode={}, length={}", empCode, reportContent.length());
        return toResponse(report);
    }

    // ─── 全矿报告 ────────────────────────────────────────────────────────

    public Map<String, Object> generateMineReport(boolean force) {
        final String KEY = "MINE";
        if (!force) {
            AiHealthReport cached = aiReportMapper.findValidCachedReportByKey(KEY);
            if (cached != null) { log.info("全矿AI报告命中缓存"); return toResponse(cached); }
        }
        Map<String, Object> hs = aiReportMapper.getMineHealthStats();
        Map<String, Object> ws = aiReportMapper.getMineWarningStats();
        if (hs == null) hs = new LinkedHashMap<>();
        if (ws == null) ws = new LinkedHashMap<>();
        if (getInt(hs, "recordCount") == 0) throw new IllegalArgumentException("暂无全矿健康数据");
        String prompt = buildMinePrompt(hs, ws);
        String content = callDeepSeek(prompt);
        aiReportMapper.deleteByEmpCode(KEY);
        AiHealthReport r = new AiHealthReport();
        r.setEmpCode(KEY); r.setEmpName("全矿"); r.setReportContent(content);
        r.setGenerateTime(LocalDateTime.now()); r.setExpiresAt(LocalDateTime.now().plusHours(6));
        aiReportMapper.insert(r);
        log.info("全矿AI报告生成成功, length={}", content.length());
        return toResponse(r);
    }

    // ─── 部门报告 ────────────────────────────────────────────────────────

    public Map<String, Object> generateDeptReport(String deptName, boolean force) {
        final String KEY = "DEPT_" + deptName;
        if (!force) {
            AiHealthReport cached = aiReportMapper.findValidCachedReportByKey(KEY);
            if (cached != null) { log.info("部门AI报告命中缓存: {}", deptName); return toResponse(cached); }
        }
        Map<String, Object> hs = aiReportMapper.getDeptHealthStats(deptName);
        Map<String, Object> ws = aiReportMapper.getDeptWarningStats(deptName);
        if (hs == null) hs = new LinkedHashMap<>();
        if (ws == null) ws = new LinkedHashMap<>();
        if (getInt(hs, "recordCount") == 0) throw new IllegalArgumentException("该部门暂无健康数据");
        String prompt = buildDeptPrompt(deptName, hs, ws);
        String content = callDeepSeek(prompt);
        aiReportMapper.deleteByEmpCode(KEY);
        AiHealthReport r = new AiHealthReport();
        r.setEmpCode(KEY); r.setEmpName(deptName); r.setReportContent(content);
        r.setGenerateTime(LocalDateTime.now()); r.setExpiresAt(LocalDateTime.now().plusHours(6));
        aiReportMapper.insert(r);
        log.info("部门AI报告生成成功: {}, length={}", deptName, content.length());
        return toResponse(r);
    }

    // ─── 构建 Prompt ────────────────────────────────────────────────────

    private String buildPrompt(Map<String, Object> emp, Map<String, Object> hs, Map<String, Object> ws) {
        String empName    = str(emp, "empName");
        String deptName   = str(emp, "deptName");
        String jobType    = str(emp, "jobTypeName");
        Object genderObj  = emp.get("gender");
        String gender     = genderObj == null ? "未知"
                          : "1".equals(genderObj.toString()) || Integer.valueOf(1).equals(genderObj) ? "男" : "女";

        int    recordCount   = getInt(hs, "recordCount");
        double avgHr         = getDbl(hs, "avgHeartRate");
        int    maxHr         = getInt(hs, "maxHeartRate");
        int    minHr         = getInt(hs, "minHeartRate");
        double avgSpo2       = getDbl(hs, "avgBloodOxygen");
        int    minSpo2       = getInt(hs, "minBloodOxygen");
        double avgTemp       = getDbl(hs, "avgTemperature");
        double avgSleep      = getDbl(hs, "avgSleepHours");

        int    totalWarnings = getInt(ws, "totalWarnings");
        int    highRisk      = getInt(ws, "highRiskCount");

        return String.format(
            "你是一名专业的职业健康管理医生，专注于矿山工人的职业健康。\n" +
            "请根据以下员工近30天的健康监测数据，生成一份专业、通俗易懂的个人健康分析报告。\n\n" +
            "【员工信息】\n" +
            "- 姓名：%s\n" +
            "- 性别：%s\n" +
            "- 部门：%s\n" +
            "- 工种：%s\n\n" +
            "【近30天健康数据】\n" +
            "- 有效记录：%d 条\n" +
            "- 平均心率：%.1f bpm（正常范围：60-100 bpm）\n" +
            "- 最高/最低心率：%d / %d bpm\n" +
            "- 平均血氧饱和度：%.1f%%（正常范围：≥95%%）\n" +
            "- 最低血氧：%d%%\n" +
            "- 平均体温：%.1f°C（正常范围：36.0-37.3°C）\n" +
            "- 平均睡眠时长：%.1f 小时/天（建议：7-9小时）\n" +
            "- 预警总次数：%d 次（其中高危/危急：%d 次）\n\n" +
            "请按以下结构生成报告（使用简洁中文，总字数400-600字）：\n" +
            "## 综合健康评价\n（1-2句话概括整体状态）\n\n" +
            "## 各指标分析\n（逐项分析心率、血氧、体温、睡眠）\n\n" +
            "## 风险提示\n（指出需要关注的问题，如无异常则说明）\n\n" +
            "## 改善建议\n（针对矿山工人工作特点，给出3-5条具体可行的建议）",
            empName, gender, deptName, jobType,
            recordCount, avgHr, maxHr, minHr,
            avgSpo2, minSpo2, avgTemp, avgSleep,
            totalWarnings, highRisk
        );
    }

    private String buildMinePrompt(Map<String, Object> hs, Map<String, Object> ws) {
        return String.format(
            "你是一名职业健康管理专家，专注于矿山企业群体健康管理。\n" +
            "请根据以下全矿近30天的整体健康监测数据，生成一份企业级健康分析报告。\n\n" +
            "【全矿健康数据（近30天）】\n" +
            "- 监测人员：%d 人，有效记录：%d 条\n" +
            "- 平均心率：%.1f bpm（正常范围：60-100）\n" +
            "- 心率极值：最高 %d bpm / 最低 %d bpm\n" +
            "- 平均血氧：%.1f%%（正常范围：≥95%%）\n" +
            "- 最低血氧：%d%%\n" +
            "- 平均体温：%.1f°C\n" +
            "- 平均睡眠：%.1f 小时/天\n" +
            "- 预警总次数：%d（高危/危急：%d，中危：%d），涉及人员：%d 人\n\n" +
            "请按以下结构生成报告（中文，总字数400-600字）：\n" +
            "## 整体健康状况评估\n## 重点风险分析\n## 群体健康趋势\n## 管理建议",
            getInt(hs,"empCount"), getInt(hs,"recordCount"),
            getDbl(hs,"avgHeartRate"), getInt(hs,"maxHeartRate"), getInt(hs,"minHeartRate"),
            getDbl(hs,"avgBloodOxygen"), getInt(hs,"minBloodOxygen"),
            getDbl(hs,"avgTemperature"), getDbl(hs,"avgSleepHours"),
            getInt(ws,"totalWarnings"), getInt(ws,"highRiskCount"), getInt(ws,"midRiskCount"), getInt(ws,"affectedEmp")
        );
    }

    private String buildDeptPrompt(String deptName, Map<String, Object> hs, Map<String, Object> ws) {
        return String.format(
            "你是一名职业健康管理专家，专注于矿山企业群体健康管理。\n" +
            "请根据以下【%s】部门近30天的健康监测数据，生成一份部门健康分析报告。\n\n" +
            "【部门健康数据（近30天）】\n" +
            "- 监测人员：%d 人，有效记录：%d 条\n" +
            "- 平均心率：%.1f bpm，极值：%d / %d bpm\n" +
            "- 平均血氧：%.1f%%，最低：%d%%\n" +
            "- 平均体温：%.1f°C，平均睡眠：%.1f 小时/天\n" +
            "- 预警次数：%d（高危/危急：%d，中危：%d）\n\n" +
            "请按以下结构生成报告（中文，350-500字）：\n" +
            "## 部门健康评估\n## 主要风险指标\n## 重点关注人员特征\n## 改善建议",
            deptName,
            getInt(hs,"empCount"), getInt(hs,"recordCount"),
            getDbl(hs,"avgHeartRate"), getInt(hs,"maxHeartRate"), getInt(hs,"minHeartRate"),
            getDbl(hs,"avgBloodOxygen"), getInt(hs,"minBloodOxygen"),
            getDbl(hs,"avgTemperature"), getDbl(hs,"avgSleepHours"),
            getInt(ws,"totalWarnings"), getInt(ws,"highRiskCount"), getInt(ws,"midRiskCount")
        );
    }

    // ─── 调用 DeepSeek API ──────────────────────────────────────────────

    private String callDeepSeek(String prompt) {
        try {
            String token = resolveApiKey();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            body.put("max_tokens", 1200);
            body.put("temperature", 0.7);
            body.put("stream", false);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("DeepSeek API 返回错误: status={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException(toUserMessage(response.statusCode()));
            }

            JSONObject json = JSON.parseObject(response.body());
            return json.getJSONArray("choices")
                       .getJSONObject(0)
                       .getJSONObject("message")
                       .getString("content");

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败", e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());
        }
    }

    // ─── 工具方法 ────────────────────────────────────────────────────────

    private Map<String, Object> toResponse(AiHealthReport r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("empCode",       r.getEmpCode());
        m.put("empName",       r.getEmpName());
        m.put("reportContent", r.getReportContent());
        m.put("generateTime",  r.getGenerateTime().format(FMT));
        m.put("expiresAt",     r.getExpiresAt().format(FMT));
        return m;
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "--";
    }

    private int getInt(Map<String, Object> m, String key) {
        if (m == null) return 0;
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return 0;
    }

    private double getDbl(Map<String, Object> m, String key) {
        if (m == null) return 0.0;
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return 0.0;
    }

    private String resolveApiKey() {
        String envKey = trimToNull(apiKey);
        if (envKey != null) return envKey;

        String cached = trimToNull(resolvedApiKey);
        if (cached != null) return cached;

        String file = trimToNull(apiKeyFile);
        if (file != null) {
            try {
                String fileKey = trimToNull(Files.readString(Path.of(file)));
                if (fileKey != null) {
                    resolvedApiKey = fileKey;
                    return fileKey;
                }
            } catch (Exception e) {
                log.warn("读取 DeepSeek API Key 文件失败: {}", e.getMessage());
            }
        }

        throw new RuntimeException("AI服务密钥未配置：请设置 DEEPSEEK_API_KEY，或在 deepseek-key.txt 中填写有效密钥后重启后端");
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private static String toUserMessage(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return "AI服务认证失败：DeepSeek API Key 无效或已过期，请检查密钥配置";
        }
        if (statusCode == 429) {
            return "AI服务调用过于频繁或额度不足，请稍后重试";
        }
        return "AI服务暂时不可用，请稍后重试";
    }
}
