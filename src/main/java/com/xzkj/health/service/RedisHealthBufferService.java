package com.xzkj.health.service;

import com.alibaba.fastjson2.JSON;
import com.xzkj.health.model.HealthRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    private static final String BUFFER_KEY = "health:buffer";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private HealthRecordService healthRecordService;

    /**
     * 将健康记录推入 Redis 缓冲队列
     * 由 DataProcessService 在 Netty 处理线程中调用，替代直接写库
     */
    public void push(HealthRecord record) {
        try {
            redisTemplate.opsForList().rightPush(BUFFER_KEY, JSON.toJSONString(record));
        } catch (Exception e) {
            // Redis 写入失败时降级直接写库，保证数据不丢失
            log.warn("Redis缓冲写入失败，降级直接写库: {}", e.getMessage());
            healthRecordService.save(record);
        }
    }

    /**
     * 定时将缓冲队列中的数据批量写入数据库
     * 每5秒执行一次，批量插入性能远优于逐条插入
     */
    @Scheduled(fixedDelay = 5000)
    public void flush() {
        Long size = redisTemplate.opsForList().size(BUFFER_KEY);
        if (size == null || size == 0) {
            return;
        }

        // 每次最多处理 2000 条，防止一次性读取过多导致 OOM
        // trim 必须在 batchInsert 成功后执行，否则 DB 失败会永久丢数据
        int batchSize = (int) Math.min(size, 2000);
        List<String> items = redisTemplate.opsForList().range(BUFFER_KEY, 0, batchSize - 1);
        if (items == null || items.isEmpty()) {
            return;
        }

        List<HealthRecord> records = items.stream()
                .map(json -> JSON.parseObject(json, HealthRecord.class))
                .collect(Collectors.toList());

        try {
            healthRecordService.batchInsert(records);
            // 写库成功后才从 Redis 删除，避免数据丢失
            redisTemplate.opsForList().trim(BUFFER_KEY, batchSize, -1);
            log.info("健康数据写库: {}条", records.size());
        } catch (Exception e) {
            // 写库失败：保留 Redis 数据，下次 flush 重试
            log.error("健康数据批量写库失败，保留 Redis buffer 等待重试: {}", e.getMessage());
        }
    }
}
