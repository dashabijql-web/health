package com.xzkj.health.service;

import com.alibaba.fastjson2.JSON;
import com.xzkj.health.config.datasource.HealthDataSourceContext;
import com.xzkj.health.config.datasource.HealthDataSourceKey;
import com.xzkj.health.model.HealthRecord;
import com.xzkj.health.observability.HealthMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 健康数据 Redis 写缓冲服务
 *
 * 解决问题：1000个手表每隔几秒上报一次数据，直接逐条写库会产生大量小事务，
 * 高并发时数据库压力大。
 *
 * 解决方案：
 *   手表数据 → push() 写入 Redis List（内存操作，微秒级）
 *   定时任务  → 每5秒 flush() 一次，批量写入数据库（一次事务插入多条）
 *
 * Redis 数据结构：
 *   Key:   health:buffer
 *   Type:  List
 *   Value: HealthRecord 的 JSON 字符串，每条一个元素
 */
@Slf4j
@Service
public class RedisHealthBufferService {

    private static final String BUFFER_KEY_PREFIX = "health:buffer:";
    private static final String DEAD_LETTER_KEY_PREFIX = "health:buffer:dead:";
    private static final String RETRY_KEY_PREFIX = "health:buffer:retry:";
    private static final int MAX_BATCH_SIZE = 2000;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private HealthRecordService healthRecordService;

    @Autowired
    private HealthMetricsService healthMetricsService;

    /**
     * 将健康记录推入 Redis 缓冲队列
     * 由 DataProcessService 在 Netty 处理线程中调用，替代直接写库
     */
    public void push(HealthRecord record) {
        HealthDataSourceKey current = HealthDataSourceContext.get();
        HealthDataSourceKey source = current == null ? HealthDataSourceKey.NEW : current;
        String bufferKey = bufferKey(source);
        try {
            Long newSize = redisTemplate.opsForList().rightPush(bufferKey, JSON.toJSONString(record));
            healthMetricsService.recordBufferPush(source.key(), "redis");
            if (newSize != null) {
                healthMetricsService.updateBufferLength(source.key(), newSize);
            }
        } catch (Exception e) {
            // Redis 写入失败时降级直接写库，保证数据不丢失
            log.warn("Redis缓冲写入失败，降级直接写库: {}", e.getMessage());
            healthMetricsService.recordBufferPush(source.key(), "direct-write-fallback");
            healthRecordService.save(record);
        }
    }

    /**
     * 定时将缓冲队列中的数据批量写入数据库
     * 每5秒执行一次，批量插入性能远优于逐条插入
     */
    @Scheduled(fixedDelay = 5000)
    public void flush() {
        for (HealthDataSourceKey source : HealthDataSourceKey.values()) {
            flushOneSource(source);
        }
    }

    private void flushOneSource(HealthDataSourceKey source) {
        String bufferKey = bufferKey(source);
        Long size;
        try {
            size = redisTemplate.opsForList().size(bufferKey);
        } catch (Exception e) {
            log.error("健康数据读取buffer长度失败: source={}, key={}", source.key(), bufferKey, e);
            return;
        }
        healthMetricsService.updateBufferLength(source.key(), size == null ? 0L : size);
        if (size == null || size == 0) {
            return;
        }

        int batchSize = (int) Math.min(size, MAX_BATCH_SIZE);
        List<String> items;
        try {
            items = redisTemplate.opsForList().range(bufferKey, 0, batchSize - 1);
        } catch (Exception e) {
            log.error("健康数据读取buffer失败: source={}, key={}, batchSize={}", source.key(), bufferKey, batchSize, e);
            return;
        }
        if (items == null || items.isEmpty()) {
            return;
        }
        log.info("健康数据读取buffer成功: source={}, key={}, bufferSize={}, batchSize={}",
                source.key(), bufferKey, size, items.size());

        ParsedBatch batch = parseBatch(source, items);
        isolateBadRecords(source, bufferKey, batch.badEntries());
        if (!batch.badEntries().isEmpty()) {
            healthMetricsService.recordBufferDeadLetter(source.key(), batch.badEntries().size());
            log.warn("健康数据反序列化失败: source={}, badCount={}, deadLetterKey={}",
                    source.key(), batch.badEntries().size(), deadLetterKey(source));
        }
        healthMetricsService.updateBufferLength(source.key(), Math.max(0L, size - batch.badEntries().size()));
        if (batch.records().isEmpty()) {
            return;
        }

        try {
            HealthDataSourceContext.runWith(source, () -> healthRecordService.batchInsert(batch.records()));
            log.info("健康数据写库成功: source={}, count={}", source.key(), batch.records().size());
            redisTemplate.opsForList().trim(bufferKey, batch.records().size(), -1);
            clearRetryCount(source);
            healthMetricsService.recordBufferFlush(source.key(), "success", batch.records().size());
            healthMetricsService.updateBufferLength(source.key(), Math.max(0L, size - items.size()));
            log.info("健康数据trim成功: source={}, trimmedCount={}", source.key(), batch.records().size());
        } catch (Exception e) {
            long retryCount = incrementRetryCount(source);
            healthMetricsService.recordBufferFlush(source.key(), "failure", batch.records().size());
            log.error("健康数据写库失败: source={}, count={}, retryCount={}",
                    source.key(), batch.records().size(), retryCount, e);
        }
    }

    private String currentBufferKey() {
        HealthDataSourceKey current = HealthDataSourceContext.get();
        return bufferKey(current == null ? HealthDataSourceKey.NEW : current);
    }

    private String bufferKey(HealthDataSourceKey source) {
        return BUFFER_KEY_PREFIX + source.key();
    }

    private String deadLetterKey(HealthDataSourceKey source) {
        return DEAD_LETTER_KEY_PREFIX + source.key();
    }

    private String retryKey(HealthDataSourceKey source) {
        return RETRY_KEY_PREFIX + source.key();
    }

    private ParsedBatch parseBatch(HealthDataSourceKey source, List<String> items) {
        List<HealthRecord> records = new ArrayList<>();
        List<BadRecord> badEntries = new ArrayList<>();
        for (String item : items) {
            try {
                records.add(JSON.parseObject(item, HealthRecord.class));
            } catch (Exception ex) {
                badEntries.add(new BadRecord(item, ex.getMessage()));
                log.warn("健康数据反序列化失败明细: source={}, raw={}", source.key(), item);
            }
        }
        return new ParsedBatch(records, badEntries);
    }

    private void isolateBadRecords(HealthDataSourceKey source, String bufferKey, List<BadRecord> badEntries) {
        for (BadRecord badEntry : badEntries) {
            redisTemplate.opsForList().rightPush(deadLetterKey(source), JSON.toJSONString(Map.of(
                    "source", source.key(),
                    "failedAt", Instant.now().toString(),
                    "error", badEntry.errorMessage(),
                    "raw", badEntry.rawJson()
            )));
            redisTemplate.opsForList().remove(bufferKey, 1, badEntry.rawJson());
        }
    }

    private long incrementRetryCount(HealthDataSourceKey source) {
        Long value = redisTemplate.opsForValue().increment(retryKey(source));
        return value == null ? 1L : value;
    }

    private void clearRetryCount(HealthDataSourceKey source) {
        redisTemplate.delete(retryKey(source));
    }

    private record ParsedBatch(List<HealthRecord> records, List<BadRecord> badEntries) {
    }

    private record BadRecord(String rawJson, String errorMessage) {
    }
}
