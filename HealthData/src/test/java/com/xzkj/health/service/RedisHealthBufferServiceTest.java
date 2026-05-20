package com.xzkj.health.service;

import com.alibaba.fastjson2.JSON;
import com.xzkj.health.config.datasource.HealthDataSourceContext;
import com.xzkj.health.config.datasource.HealthDataSourceKey;
import com.xzkj.health.model.HealthRecord;
import com.xzkj.health.observability.HealthMetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisHealthBufferServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HealthRecordService healthRecordService;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HealthMetricsService healthMetricsService;

    @InjectMocks
    private RedisHealthBufferService redisHealthBufferService;

    @AfterEach
    void clearContext() {
        HealthDataSourceContext.clear();
    }

    @Test
    void pushUsesCurrentDataSourceBuffer() {
        HealthRecord record = new HealthRecord();
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        HealthDataSourceContext.set(HealthDataSourceKey.OLD);

        redisHealthBufferService.push(record);

        verify(listOperations).rightPush(eq("health:buffer:old"), anyString());
        verify(healthMetricsService).recordBufferPush("old", "redis");
        assertEquals(HealthDataSourceKey.OLD, HealthDataSourceContext.get());
    }

    @Test
    void pushDefaultsToNewBufferWhenNoDataSourceContextExists() {
        HealthRecord record = new HealthRecord();
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        redisHealthBufferService.push(record);

        verify(listOperations).rightPush(eq("health:buffer:new"), anyString());
        verify(healthMetricsService).recordBufferPush("new", "redis");
        assertNull(HealthDataSourceContext.get());
    }

    @Test
    void flushIsolatesBadJsonAndStillWritesGoodRecords() {
        String goodJson = JSON.toJSONString(new HealthRecord());
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.size("health:buffer:old")).thenReturn(2L);
        when(listOperations.range("health:buffer:old", 0, 1)).thenReturn(List.of("bad-json", goodJson));
        when(listOperations.size("health:buffer:new")).thenReturn(0L);

        redisHealthBufferService.flush();

        verify(listOperations).rightPush(eq("health:buffer:dead:old"), argThat(contains("bad-json")));
        verify(listOperations).remove("health:buffer:old", 1, "bad-json");
        verify(healthRecordService).batchInsert(argThat(list -> list.size() == 1));
        verify(listOperations).trim("health:buffer:old", 1, -1);
        verify(redisTemplate).delete("health:buffer:retry:old");
        verify(healthMetricsService).recordBufferDeadLetter("old", 1);
        verify(healthMetricsService).recordBufferFlush("old", "success", 1);
    }

    @Test
    void flushKeepsGoodRecordsWhenBatchInsertFails() {
        String goodJson = JSON.toJSONString(new HealthRecord());
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(listOperations.size("health:buffer:old")).thenReturn(1L);
        when(listOperations.range("health:buffer:old", 0, 0)).thenReturn(List.of(goodJson));
        when(listOperations.size("health:buffer:new")).thenReturn(0L);
        when(valueOperations.increment("health:buffer:retry:old")).thenReturn(1L);
        doThrow(new RuntimeException("db down")).when(healthRecordService).batchInsert(anyList());

        redisHealthBufferService.flush();

        verify(valueOperations).increment("health:buffer:retry:old");
        verify(listOperations, never()).trim("health:buffer:old", 1, -1);
        verify(healthMetricsService).recordBufferFlush("old", "failure", 1);
    }

    @Test
    void flushProcessesOldAndNewBuffersInTheirOwnDataSourceContexts() {
        HealthRecord oldRecord = new HealthRecord();
        oldRecord.setUserCode("old-user");
        HealthRecord newRecord = new HealthRecord();
        newRecord.setUserCode("new-user");

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.size("health:buffer:old")).thenReturn(1L);
        when(listOperations.range("health:buffer:old", 0, 0)).thenReturn(List.of(JSON.toJSONString(oldRecord)));
        when(listOperations.size("health:buffer:new")).thenReturn(1L);
        when(listOperations.range("health:buffer:new", 0, 0)).thenReturn(List.of(JSON.toJSONString(newRecord)));

        List<HealthDataSourceKey> seenSources = new ArrayList<>();
        List<String> seenUsers = new ArrayList<>();
        doAnswer(invocation -> {
            seenSources.add(HealthDataSourceContext.get());
            List<HealthRecord> batch = invocation.getArgument(0);
            seenUsers.add(batch.get(0).getUserCode());
            return null;
        }).when(healthRecordService).batchInsert(anyList());

        redisHealthBufferService.flush();

        assertEquals(List.of(HealthDataSourceKey.OLD, HealthDataSourceKey.NEW), seenSources);
        assertEquals(List.of("old-user", "new-user"), seenUsers);
        verify(listOperations).trim("health:buffer:old", 1, -1);
        verify(listOperations).trim("health:buffer:new", 1, -1);
        verify(redisTemplate).delete("health:buffer:retry:old");
        verify(redisTemplate).delete("health:buffer:retry:new");
        assertNull(HealthDataSourceContext.get());
    }

    private ArgumentMatcher<String> contains(String value) {
        return item -> item != null && item.contains(value);
    }
}
