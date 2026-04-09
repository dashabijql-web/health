package com.xzkj.health.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 对话核心服务 — Text2SQL RAG 实现
 *
 * 【RAG 的本质】
 *
 * RAG = Retrieval-Augmented Generation（检索增强生成）
 *
 * 标准 RAG 流程：
 *   用户问题 → 检索相关文档 → 把文档塞进 Prompt → LLM 生成回答
 *
 * Text2SQL RAG 的特殊之处：
 *   "检索" 不是向量相似搜索，而是 SQL 数据库查询
 *   "文档" 不是文本片段，而是结构化的数据库查询结果
 *
 * 【两次调用 LLM 的设计】
 *
 * 第一次调用：问题 → SQL（让 LLM 扮演 SQL 专家）
 * 第二次调用：SQL结果 → 自然语言（让 LLM 扮演健康顾问）
 *
 * 为什么不一次搞定？因为两个任务的特点不同：
 *   - 生成 SQL：需要低温度（temperature=0.1），精确，不要发挥
 *   - 解释结果：需要中等温度，自然，有一定表达灵活性
 *
 * 未来可以优化为一次调用（让 LLM 同时返回 SQL + 解释），但两次更清晰易调试。
 */
@Slf4j
@Service
public class AiChatService {

    // SQL 提取正则：匹配 ```sql ... ``` 代码块，忽略大小写，跨行匹配
    private static final Pattern SQL_PATTERN = Pattern.compile(
        "```sql\\s*([\\s\\S]+?)```", Pattern.CASE_INSENSITIVE
    );

    // 危险关键字黑名单（用于安全校验）
    private static final List<String> DANGEROUS_KEYWORDS = List.of(
        "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE",
        "ALTER", "CREATE", "EXEC", "EXECUTE", "MERGE", "GRANT", "REVOKE"
    );

    // 每个 session 最多保留最近 N 轮对话，防止 token 超限
    private static final int MAX_HISTORY_TURNS = 5;
    // Redis key 前缀，TTL 24小时
    private static final String SESSION_KEY_PREFIX = "ai:session:";
    private static final long SESSION_TTL_HOURS = 24;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SchemaProvider schemaProvider;

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private SqlExecutorMapper sqlExecutorMapper;

    /**
     * 处理用户问题（多轮对话版）
     *
     * 完整流程：
     *   Step 1: 取出该 session 的历史对话
     *   Step 2: 调用 DeepSeek 生成 SQL（带历史上下文）
     *   Step 3: 提取 SQL → 安全校验 → 执行
     *   Step 4: 调用 DeepSeek 解释结果（带历史上下文）
     *   Step 5: 把本轮 user/assistant 追加到 session 历史
     *
     * @param userQuestion 用户的自然语言问题
     * @param sessionId    会话ID，用于维护多轮上下文
     * @return AI 的中文回答
     */
    public String chat(String userQuestion, String sessionId) {
        log.info("收到用户问题: {} | session: {}", userQuestion, sessionId);

        // ─── Step 1: 从 Redis 加载 session 历史 ──────────────────────────
        List<Map<String, Object>> history = loadHistory(sessionId);

        // ─── Step 2: 第一次调用 DeepSeek → 生成 SQL ──────────────────────
        // SQL 生成不带历史（历史对话不影响SQL逻辑，且避免干扰LLM生成SQL）
        String schema = schemaProvider.getSchema();
        log.info("Step 2: 调用 DeepSeek 生成 SQL...");
        String llmResponse = deepSeekClient.chat(schema, userQuestion);
        log.info("DeepSeek 返回: {}", llmResponse);

        // ─── Step 3: 提取 SQL → 安全校验 → 执行（含自动修复重试）──────────
        String sql = extractSql(llmResponse);

        String finalAnswer;

        if (sql == null) {
            log.info("LLM 未生成 SQL，直接返回解释");
            finalAnswer = llmResponse;
        } else {
            log.info("提取到 SQL: {}", sql);
            validateSql(sql);

            List<Map<String, Object>> queryResult = executeWithRetry(sql, schema, userQuestion);

            // ─── Step 4: 第二次调用 DeepSeek → 解释查询结果（带历史）──────
            log.info("Step 4: 调用 DeepSeek 解释查询结果（history={} 轮）...", history.size() / 2);

            String interpreterSystemPrompt = """
                    你是一个煤矿工人健康管理系统的智能助手，拥有完整的对话记忆。
                    用户提问后，系统已经查询了数据库，请根据查询结果用自然、友好的中文回答用户问题。
                    如果用户引用了之前的问题（如"他"、"这个部门"、"刚才说的"），请结合对话历史理解。

                    要求：
                    1. 直接回答问题，不要说"根据查询结果"这类废话
                    2. 数据中的英文字段名要翻译成中文
                    3. 如果结果为空，告知用户没有找到相关数据
                    4. 适当给出健康建议（如心率偏高提示注意休息）
                    5. 回答简洁，不超过300字
                    """;

            String interpreterUserMessage = String.format(
                "用户问题：%s\n\n数据库查询结果（JSON格式）：\n%s",
                userQuestion,
                JSON.toJSONString(queryResult)
            );

            finalAnswer = deepSeekClient.chatWithHistory(interpreterSystemPrompt, history, interpreterUserMessage);
        }

        // ─── Step 5: 追加本轮对话并持久化到 Redis ──────────────────────────
        appendToHistory(history, userQuestion, finalAnswer, sessionId);

        log.info("最终回答生成完毕 | session={} history={}轮", sessionId, history.size() / 2);
        return finalAnswer;
    }

    /** 兼容旧的无 session 调用（生成随机 sessionId） */
    public String chat(String userQuestion) {
        return chat(userQuestion, "anonymous-" + System.currentTimeMillis());
    }

    /**
     * 流式对话：先同步执行 SQL 查询，再流式调用 DeepSeek 解释结果
     *
     * 流程：
     *   1. SQL 生成 + 执行（同步，不能流式）
     *   2. 解释结果时流式调用 DeepSeek，每收到 token 发送 SSE 事件
     *   3. 完成后发 [DONE] 事件，追加历史
     *
     * SSE 事件格式（前端按 EventSource 协议解析）：
     *   data: token文本\n\n
     *   data: [DONE]\n\n
     *   data: [SESSION]:sessionId\n\n
     */
    public void chatStream(String userQuestion, String sessionId, SseEmitter emitter) throws IOException {
        log.info("流式对话开始: {} | session: {}", userQuestion, sessionId);

        List<Map<String, Object>> history = loadHistory(sessionId);

        // Step 1: 生成 SQL（同步调用，因为要先拿到查询结果才能解释）
        String schema = schemaProvider.getSchema();
        String llmResponse = deepSeekClient.chat(schema, userQuestion);
        String sql = extractSql(llmResponse);

        String interpreterUserMessage;

        if (sql == null) {
            // 无 SQL：直接流式输出 LLM 的原始回复（逐字发送）
            for (char c : llmResponse.toCharArray()) {
                emitter.send(SseEmitter.event().data(String.valueOf(c)));
            }
            appendToHistory(history, userQuestion, llmResponse, sessionId);
            emitter.send(SseEmitter.event().data("[SESSION]:" + sessionId));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
            return;
        }

        // Step 2: 安全校验 + 执行 SQL（含自动修复）
        validateSql(sql);
        List<Map<String, Object>> queryResult;
        try {
            queryResult = executeWithRetry(sql, schema, userQuestion);
        } catch (Exception e) {
            String errMsg = "抱歉，查询执行失败：" + e.getMessage();
            emitter.send(SseEmitter.event().data(errMsg));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
            return;
        }

        // Step 3: 流式调用 DeepSeek 解释结果
        String interpreterSystemPrompt = """
                你是一个煤矿工人健康管理系统的智能助手，拥有完整的对话记忆。
                用户提问后，系统已经查询了数据库，请根据查询结果用自然、友好的中文回答用户问题。
                如果用户引用了之前的问题（如"他"、"这个部门"、"刚才说的"），请结合对话历史理解。

                要求：
                1. 直接回答问题，不要说"根据查询结果"这类废话
                2. 数据中的英文字段名要翻译成中文
                3. 如果结果为空，告知用户没有找到相关数据
                4. 适当给出健康建议（如心率偏高提示注意休息）
                5. 回答简洁，不超过300字
                """;

        interpreterUserMessage = String.format(
            "用户问题：%s\n\n数据库查询结果（JSON格式）：\n%s",
            userQuestion, JSON.toJSONString(queryResult)
        );

        final String finalSessionId = sessionId;
        final String finalQuestion = userQuestion;
        final String finalSql = sql;

        deepSeekClient.chatStream(
            interpreterSystemPrompt, history, interpreterUserMessage,
            // onToken：每收到一个 token，立刻发给前端
            token -> {
                try {
                    emitter.send(SseEmitter.event().data(token));
                } catch (IOException e) {
                    throw new RuntimeException("SSE 发送失败", e);
                }
            },
            // onDone：流结束，发 SQL 调试信息、追加历史并发 DONE
            fullContent -> {
                appendToHistory(history, finalQuestion, fullContent, finalSessionId);
                try {
                    // 发送 SQL 调试信息（Base64 编码避免换行符破坏 SSE 格式）
                    String sqlB64 = java.util.Base64.getEncoder()
                        .encodeToString(finalSql.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    emitter.send(SseEmitter.event().data("[SQL]:" + sqlB64));
                    emitter.send(SseEmitter.event().data("[SESSION]:" + finalSessionId));
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    emitter.complete();
                    log.info("流式对话完成: session={}", finalSessionId);
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }
        );
    }

    /**
     * 清除指定 session 的历史（用于"开始新对话"按钮）
     */
    public void clearSession(String sessionId) {
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
        log.info("清除 session 历史: {}", sessionId);
    }

    /**
     * 追加一轮对话到历史，并持久化到 Redis
     */
    private void appendToHistory(List<Map<String, Object>> history, String question, String answer,
                                 String sessionId) {
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", question);

        Map<String, Object> assistantMsg = new LinkedHashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", answer);

        history.add(userMsg);
        history.add(assistantMsg);

        // 超过最大轮数时，删除最早的一轮（2条消息）
        while (history.size() > MAX_HISTORY_TURNS * 2) {
            history.remove(0);
            history.remove(0);
        }

        // 持久化到 Redis，刷新 TTL
        saveHistory(sessionId, history);
    }

    /** 从 Redis 加载 session 历史，不存在则返回空列表 */
    private List<Map<String, Object>> loadHistory(String sessionId) {
        String json = redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + sessionId);
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return JSON.parseObject(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("解析 session 历史失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /** 保存 session 历史到 Redis，重置 TTL */
    private void saveHistory(String sessionId, List<Map<String, Object>> history) {
        redisTemplate.opsForValue().set(
            SESSION_KEY_PREFIX + sessionId,
            JSON.toJSONString(history),
            SESSION_TTL_HOURS, TimeUnit.HOURS
        );
    }

    /**
     * 从 LLM 回复中提取 SQL 代码块
     *
     * LLM 通常会把 SQL 包裹在 ```sql ... ``` 里，
     * 我们用正则把它提取出来。
     *
     * 如果没有代码块，说明 LLM 判断无法用 SQL 回答（如问"你好"），
     * 直接返回 null，调用方会把 LLM 的原始回复返回给用户。
     *
     * @return SQL 字符串，或 null（如果 LLM 没有生成 SQL）
     */
    private String extractSql(String llmResponse) {
        Matcher matcher = SQL_PATTERN.matcher(llmResponse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * SQL 安全校验
     *
     * 确保 LLM 生成的 SQL 只包含 SELECT，防止误操作数据库。
     *
     * @throws IllegalArgumentException 如果 SQL 不安全
     */
    /**
     * 执行 SQL，失败时自动让 LLM 修正重试（最多 2 次）
     *
     * 自修复流程：
     *   1. 执行 SQL → 成功返回结果
     *   2. 失败 → 把原 SQL + 错误信息发给 LLM，让它生成修正后的 SQL
     *   3. 再次执行 → 成功返回；仍失败 → 抛出异常（不再重试）
     */
    private List<Map<String, Object>> executeWithRetry(String sql, String schema, String userQuestion) {
        int maxRetry = 2;
        String currentSql = sql;
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                List<Map<String, Object>> result = sqlExecutorMapper.executeQuery(currentSql);
                if (attempt > 1) log.info("SQL 自修复成功（第{}次）", attempt);
                else log.info("SQL 执行成功，返回 {} 行数据", result.size());
                return result;
            } catch (Exception e) {
                lastError = e;
                log.warn("SQL 执行失败（第{}/{}次）: {} | 错误: {}", attempt, maxRetry, currentSql, e.getMessage());

                if (attempt < maxRetry) {
                    // 让 LLM 修正 SQL
                    String fixPrompt = String.format(
                        "以下 SQL 执行时出错，请修正后重新生成一条正确的 SQL。\n\n" +
                        "原始问题：%s\n\n" +
                        "错误的 SQL：\n```sql\n%s\n```\n\n" +
                        "错误信息：%s\n\n" +
                        "请只返回修正后的 SQL，用 ```sql ... ``` 包裹。",
                        userQuestion, currentSql, e.getMessage()
                    );
                    try {
                        String fixResponse = deepSeekClient.chat(schema, fixPrompt);
                        String fixedSql = extractSql(fixResponse);
                        if (fixedSql != null) {
                            validateSql(fixedSql);
                            currentSql = fixedSql;
                            log.info("LLM 修正后的 SQL: {}", currentSql);
                        } else {
                            break; // LLM 没返回 SQL，放弃重试
                        }
                    } catch (Exception fixEx) {
                        log.warn("SQL 自修复失败: {}", fixEx.getMessage());
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("查询执行失败（已尝试自动修复）：" + lastError.getMessage());
    }

    private void validateSql(String sql) {
        String upperSql = sql.toUpperCase().trim();

        // 必须以 SELECT 开头
        if (!upperSql.startsWith("SELECT")) {
            throw new IllegalArgumentException("安全校验失败：只允许 SELECT 查询，当前SQL不是SELECT语句");
        }

        // 检查危险关键字
        for (String keyword : DANGEROUS_KEYWORDS) {
            // 用单词边界匹配，避免误判（如 SELECTION 不应该被匹配到 SELECT）
            if (Pattern.compile("\\b" + keyword + "\\b").matcher(upperSql).find()) {
                throw new IllegalArgumentException("安全校验失败：SQL 包含禁止的关键字: " + keyword);
            }
        }
    }
}
