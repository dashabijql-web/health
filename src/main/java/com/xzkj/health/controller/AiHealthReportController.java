package com.xzkj.health.controller;

import com.xzkj.health.common.Result;
import com.xzkj.health.service.AiHealthReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai/health-report")
public class AiHealthReportController {

    @Autowired
    private AiHealthReportService aiHealthReportService;

    /**
     * 查询缓存报告（不触发生成）
     * GET /ai/health-report?empCode=EMP0001
     */
    @GetMapping
    public Result<Map<String, Object>> getCachedReport(@RequestParam String empCode) {
        Map<String, Object> report = aiHealthReportService.getCachedReport(empCode);
        if (report == null) {
            return Result.error("暂无有效报告，请点击生成");
        }
        return Result.ok(report);
    }

    /**
     * 生成报告（有缓存则返回缓存，force=true 强制重新生成）
     * POST /ai/health-report/generate?empCode=EMP0001&force=false
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generateReport(
            @RequestParam String empCode,
            @RequestParam(defaultValue = "false") boolean force) {
        try {
            Map<String, Object> report = aiHealthReportService.generateReport(empCode, force);
            return Result.ok(report);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("AI报告生成失败: empCode={}", empCode, e);
            return Result.error("AI分析服务暂时不可用，请稍后重试");
        }
    }
}
