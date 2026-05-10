package com.xzkj.health.service.impl;

import com.xzkj.health.dto.heartrate.HeartRateAgeStatView;
import com.xzkj.health.dto.heartrate.HeartRateDepartmentStatView;
import com.xzkj.health.dto.heartrate.HeartRateOverviewView;
import com.xzkj.health.dto.heartrate.HeartRateRealtimeView;
import com.xzkj.health.dto.heartrate.HeartRateTopUserView;
import com.xzkj.health.dto.heartrate.HeartRateTrendView;
import com.xzkj.health.mapper.HeartRateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeartRateServiceImplTest {

    @Mock
    private HeartRateMapper heartRateMapper;

    @InjectMocks
    private HeartRateServiceImpl heartRateService;

    @Test
    void getOverviewCachesTypedView() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("avgHeartRate", 76);
        row.put("minHeartRate", 52);
        row.put("maxHeartRate", 132);
        row.put("detectionRate", 88);
        row.put("normalCount", 120);
        row.put("abnormalCount", 9);
        row.put("totalCount", 129);

        when(heartRateMapper.getHeartRateOverviewDirect(anyString(), eq("2026-05-01"), eq("2026-05-07")))
                .thenReturn(row);

        HeartRateOverviewView first = heartRateService.getHeartRateOverview("2026-05-01", "2026-05-07");
        HeartRateOverviewView second = heartRateService.getHeartRateOverview("2026-05-01", "2026-05-07");

        assertEquals(76, first.avgHeartRate());
        assertEquals(132, first.maxHeartRate());
        assertSame(first, second);
        verify(heartRateMapper, times(1))
                .getHeartRateOverviewDirect(anyString(), eq("2026-05-01"), eq("2026-05-07"));
    }

    @Test
    void getTopUsersMapsTypedRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userCode", "EMP1001");
        row.put("userName", "张三");
        row.put("count", 7);
        row.put("anomalyDays", 3);
        rows.add(row);

        when(heartRateMapper.getTopUsersDirect(anyString(), eq(10), eq("2026-05-01"), eq("2026-05-07")))
                .thenReturn(rows);

        List<HeartRateTopUserView> result = heartRateService.getTopUsers(10, "2026-05-01", "2026-05-07");

        assertEquals(1, result.size());
        assertEquals("EMP1001", result.get(0).userCode());
        assertEquals(3, result.get(0).anomalyDays());
    }

    @Test
    void getAgeDistributionCachesTypedRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ageRange", "30-40");
        row.put("avgHeartRate", 79);
        rows.add(row);

        when(heartRateMapper.getAgeDistributionDirect(anyString(), eq("2026-05-01"), eq("2026-05-07")))
                .thenReturn(rows);

        List<HeartRateAgeStatView> first = heartRateService.getAgeDistribution("2026-05-01", "2026-05-07");
        List<HeartRateAgeStatView> second = heartRateService.getAgeDistribution("2026-05-01", "2026-05-07");

        assertEquals(1, first.size());
        assertEquals("30-40", first.get(0).ageRange());
        assertEquals(79, first.get(0).avgHeartRate());
        assertSame(first, second);
        verify(heartRateMapper, times(1))
                .getAgeDistributionDirect(anyString(), eq("2026-05-01"), eq("2026-05-07"));
    }

    @Test
    void getTrendCachesTypedSeries() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("date", "2026-05-07");
        row.put("avgHeartRate", 82);
        rows.add(row);

        when(heartRateMapper.getHeartRateTrendDirect(anyString(), eq(7))).thenReturn(rows);

        HeartRateTrendView first = heartRateService.getHeartRateTrend(7);
        HeartRateTrendView second = heartRateService.getHeartRateTrend(7);

        assertEquals(1, first.dates().size());
        assertEquals("5.7", first.dates().get(0));
        assertEquals(82, first.values().get(0));
        assertSame(first, second);
        verify(heartRateMapper, times(1)).getHeartRateTrendDirect(anyString(), eq(7));
    }

    @Test
    void getDepartmentStatsCachesTypedRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("deptName", "综采队");
        row.put("avgHeartRate", 84);
        row.put("lowCount", 1);
        row.put("highCount", 5);
        row.put("abnormalCount", 6);
        row.put("totalCount", 30);
        rows.add(row);

        when(heartRateMapper.getDepartmentStatsDirect(anyString(), eq("2026-05-01"), eq("2026-05-07")))
                .thenReturn(rows);

        List<HeartRateDepartmentStatView> first = heartRateService.getDepartmentStats("2026-05-01", "2026-05-07");
        List<HeartRateDepartmentStatView> second = heartRateService.getDepartmentStats("2026-05-01", "2026-05-07");

        assertEquals(1, first.size());
        assertEquals("综采队", first.get(0).deptName());
        assertEquals(6, first.get(0).abnormalCount());
        assertSame(first, second);
        verify(heartRateMapper, times(1))
                .getDepartmentStatsDirect(anyString(), eq("2026-05-01"), eq("2026-05-07"));
    }

    @Test
    void getRealtimeMapsEmpCodeFromUserCode() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userCode", "EMP1001");
        row.put("userName", "张三");
        row.put("deptName", "综采队");
        row.put("gender", "男");
        row.put("age", 32);
        row.put("jobType", "采煤工");
        row.put("heartRate", 88);
        row.put("recordTime", "2026-05-07 10:00:00");
        rows.add(row);

        when(heartRateMapper.getRealtimeDirect(anyString(), eq(20))).thenReturn(rows);

        List<HeartRateRealtimeView> result = heartRateService.getRealtime(20);

        assertEquals(1, result.size());
        assertEquals("EMP1001", result.get(0).userCode());
        assertEquals("EMP1001", result.get(0).empCode());
        assertEquals("男", result.get(0).gender());
    }
}
