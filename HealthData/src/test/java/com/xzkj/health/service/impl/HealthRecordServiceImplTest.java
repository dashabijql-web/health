package com.xzkj.health.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzkj.health.mapper.HealthRecordMapper;
import com.xzkj.health.model.HealthRecord;
import com.xzkj.health.util.TableNameUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceImplTest {

    @Mock
    private HealthRecordMapper healthRecordMapper;

    @InjectMocks
    private HealthRecordServiceImpl healthRecordService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(healthRecordService, "baseMapper", healthRecordMapper);
    }

    @Test
    void getPageFilteredWithoutFiltersUsesPartitionFastPath() {
        String currentTable = TableNameUtil.healthRecordTable();
        when(healthRecordMapper.countRowsByTableName(anyString())).thenReturn(0L);
        when(healthRecordMapper.countRowsByTableName(eq(currentTable))).thenReturn(128L);

        List<HealthRecord> records = List.of(record("EMP1001"), record("EMP1002"));
        when(healthRecordMapper.selectPageFromSource(eq(currentTable), eq(0L), eq(10L)))
                .thenReturn(records);

        Page<HealthRecord> result = healthRecordService.getPageFiltered(new Page<>(1, 10), null, null, null);

        assertEquals(128L, result.getTotal());
        assertSame(records, result.getRecords());
        verify(healthRecordMapper).countRowsByTableName(eq(currentTable));
        verify(healthRecordMapper).selectPageFromSource(eq(currentTable), eq(0L), eq(10L));
        verify(healthRecordMapper, never()).selectPage(any(), any());
    }

    @Test
    void getPageFilteredWithUserCodeKeepsWrapperPagination() {
        Page<HealthRecord> expected = new Page<>(1, 10);
        expected.setTotal(1);
        expected.setRecords(List.of(record("EMP2001")));
        when(healthRecordMapper.selectPage(any(), any())).thenReturn(expected);

        Page<HealthRecord> result = healthRecordService.getPageFiltered(new Page<>(1, 10), "EMP2001", null, null);

        assertSame(expected, result);
        verify(healthRecordMapper).selectPage(any(), any());
        verify(healthRecordMapper, never()).countRowsByTableName(anyString());
        verify(healthRecordMapper, never()).selectPageFromSource(anyString(), any(Long.class), any(Long.class));
    }

    private static HealthRecord record(String userCode) {
        HealthRecord record = new HealthRecord();
        record.setUserCode(userCode);
        record.setTime(LocalDateTime.now().toString());
        return record;
    }
}
