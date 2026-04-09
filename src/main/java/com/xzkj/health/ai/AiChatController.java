package com.xzkj.health.ai;

import com.xzkj.health.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * AI 对话 Controller
 *
 * 接口：POST /ai/chat
 * 请求体：{ "question": "综采一队本周有几人心率超标？" }
 * 响应：{ "code": 200, "data": { "answer": "综采一队本周共5人..." } }
 */
@Slf4j
@RestController
@RequestMapping("/ai/chat")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    /**
     * 发送问题，获取 AI 回答（支持多轮对话）
     *
     * 请求体：{ "question": "...", "sessionId": "xxx" }
     * 响应：  { "answer": "...", "sessionId": "xxx" }
     *
     * sessionId 由前端生成并持久化，同一会话每次传相同的 sessionId。
     */
    @PostMapping
    public Result<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String question  = body.get("question");
        String sessionId = body.get("sessionId");

        if (question == null || question.isBlank()) {
            return Result.error(400, "问题不能为空");
        }
        if (question.length() > 500) {
            return Result.error(400, "问题太长，请控制在500字以内");
        }
        // 没传 sessionId 时自动生成（兼容旧调用）
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = java.util.UUID.randomUUID().toString();
        }

        try {
            String answer = aiChatService.chat(question.trim(), sessionId);
            Map<String, String> data = new java.util.LinkedHashMap<>();
            data.put("answer", answer);
            data.put("sessionId", sessionId);
            return Result.ok(data);
        } catch (IllegalArgumentException e) {
            log.warn("安全校验失败: {}", e.getMessage());
            return Result.error("抱歉，该问题无法安全处理：" + e.getMessage());
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return Result.error("AI服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 流式对话接口（SSE）— 逐字输出，像 ChatGPT 一样
     *
     * 【SSE 是什么？】
     * Server-Sent Events：服务器主动推送数据给浏览器的技术。
     * 连接建立后，服务器可以持续发送 "data: ...\n\n" 格式的文本块，
     * 浏览器用 EventSource 或 fetch+ReadableStream 接收。
     *
     * 【流程】
     * 1. 前端用 fetch POST 这个接口
     * 2. 后端先执行 SQL 查询（非流式），再流式调用 DeepSeek 解释结果
     * 3. 每收到一个 DeepSeek 的 token，立刻发给前端
     * 4. 结束时发 [DONE] 信号
     *
     * 请求体：{ "question": "...", "sessionId": "xxx" }
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> body) {
        String question  = body.get("question");
        String sessionId = body.get("sessionId");

        SseEmitter emitter = new SseEmitter(120_000L); // 2分钟超时

        if (question == null || question.isBlank()) {
            try {
                emitter.send(SseEmitter.event().data("{\"error\":\"问题不能为空\"}"));
                emitter.complete();
            } catch (IOException e) { emitter.completeWithError(e); }
            return emitter;
        }
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = java.util.UUID.randomUUID().toString();
        }
        final String finalSessionId = sessionId;

        // 在新线程中执行，避免阻塞 HTTP 线程
        new Thread(() -> {
            try {
                aiChatService.chatStream(question.trim(), finalSessionId, emitter);
            } catch (Exception e) {
                log.error("流式对话失败", e);
                try {
                    emitter.send(SseEmitter.event().data("[ERROR]" + e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) { emitter.completeWithError(ex); }
            }
        }).start();

        return emitter;
    }

    /**
     * 清除会话历史（"开始新对话"功能）
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> clearSession(@PathVariable String sessionId) {
        aiChatService.clearSession(sessionId);
        return Result.ok(null);
    }
}
