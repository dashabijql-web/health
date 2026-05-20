package com.xzkj.health.service;

import com.xzkj.health.common.exception.BusinessException;
import com.xzkj.health.config.datasource.HealthCacheKeys;
import com.xzkj.health.dto.realtime.RealtimeAlertRow;
import com.xzkj.health.dto.realtime.RealtimeAlertView;
import com.xzkj.health.dto.realtime.RealtimeOverviewRow;
import com.xzkj.health.dto.realtime.RealtimeOverviewView;
import com.xzkj.health.dto.realtime.RealtimeStatisticsRow;
import com.xzkj.health.dto.realtime.RealtimeStatisticsView;
import com.xzkj.health.dto.realtime.RealtimeUserDetailView;
import com.xzkj.health.dto.realtime.RealtimeUserPageView;
import com.xzkj.health.dto.realtime.RealtimeUserRow;
import com.xzkj.health.mapper.RealtimeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealtimeServiceTest {

    @Mock
    private RealtimeMapper realtimeMapper;

    @InjectMocks
    private RealtimeService realtimeService;

    @Test
    void getTodayAvgOverviewMapsTypedOverview() {
        RealtimeOverviewRow row = new RealtimeOverviewRow();
        row.setAvgHeartRate(76.4);
        row.setAvgBloodOxygen(97.2);
        row.setAvgSteps(6521.8);
        row.setAvgTemperature(36.46);
        row.setAvgSleep(6.38);
        row.setTodayWarningCount(12L);

        when(realtimeMapper.getTodayAvgData()).thenReturn(row);

        RealtimeOverviewView result = realtimeService.getTodayAvgOverview();

        assertEquals(76L, result.avgHeartRate());
        assertEquals(97L, result.avgBloodOxygen());
        assertEquals(6522L, result.avgSteps());
        assertEquals(36.5, result.avgTemperature(), 0.001);
        assertEquals(6.4, result.avgSleep(), 0.001);
        assertEquals(12L, result.todayWarningCount());
    }

    @Test
    void getOnlineUsersMapsRowsAndSkipsCountOnShortPage() {
        List<RealtimeUserRow> rows = new ArrayList<>();
        RealtimeUserRow row = new RealtimeUserRow();
        row.setId(1L);
        row.setUserCode("E001");
        row.setUserName("张三");
        row.setGender(1);
        row.setAge(32);
        row.setDeptName("机电队");
        row.setHeartRate(78);
        row.setBloodOxygen(97);
        row.setSteps(5432);
        row.setCalories(320);
        row.setTemperature(36.46);
        row.setSleepHours(6.38);
        row.setBloodPressureHigh(126);
        row.setBloodPressureLow(82);
        row.setPressure(45);
        row.setStatus("normal");
        row.setLastUpdate("2026-05-07 10:30:00");
        row.setImei("359456780012345");
        rows.add(row);

        when(realtimeMapper.getOnlineUsersDirect(anyString(), anyInt(), anyInt())).thenReturn(rows);

        RealtimeUserPageView result = realtimeService.getOnlineUsers(1, 20);

        assertEquals(1, result.total());
        assertEquals(1, result.list().size());
        assertFalse(result.stale());
        assertEquals("E001", result.list().get(0).userCode());
        assertEquals(36.5, result.list().get(0).temperature(), 0.001);
        assertEquals(6.4, result.list().get(0).sleepHours(), 0.001);
        verify(realtimeMapper, times(0)).countOnlineUsersDirect(anyString());
    }

    @Test
    void getOnlineUsersReturnsStaleCacheOnFailureForCachedFullRequest() {
        List<RealtimeUserRow> rows = new ArrayList<>();
        RealtimeUserRow row = new RealtimeUserRow();
        row.setUserCode("E001");
        row.setUserName("张三");
        row.setStatus("warning");
        rows.add(row);

        when(realtimeMapper.getOnlineUsersDirect(anyString(), anyInt(), anyInt()))
                .thenReturn(rows)
                .thenThrow(new RuntimeException("db down"));

        RealtimeUserPageView first = realtimeService.getOnlineUsers(1, 200);
        forceOnlineUsersCacheExpired(first, 1, 200);
        RealtimeUserPageView second = realtimeService.getOnlineUsers(1, 200);

        assertFalse(first.stale());
        assertTrue(second.stale());
        assertSame(first.list(), second.list());
    }

    @Test
    void getOnlineUsersThrowsBusinessExceptionWhenNoStaleCacheExists() {
        when(realtimeMapper.getOnlineUsersDirect(anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("db down"));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> realtimeService.getOnlineUsers(1, 200)
        );

        assertEquals(503, ex.getCode());
        assertEquals("实时监控数据加载失败，请稍后重试", ex.getMessage());
    }

    @Test
    void getStatisticsCachesTypedResult() {
        RealtimeStatisticsRow row = new RealtimeStatisticsRow();
        row.setOnlineUsers(12L);
        row.setTotalUsers(48L);
        row.setWeekRecords(360L);
        row.setTodayRecords(55L);
        row.setOnlineRate(25.2);
        row.setNormalRate(91.4);

        when(realtimeMapper.getStatisticsDirect()).thenReturn(row);

        RealtimeStatisticsView first = realtimeService.getStatistics();
        RealtimeStatisticsView second = realtimeService.getStatistics();

        assertEquals(12L, first.onlineUsers());
        assertEquals(25L, first.onlineRate());
        assertEquals(91L, first.normalRate());
        assertSame(first, second);
        verify(realtimeMapper, times(1)).getStatisticsDirect();
    }

    @Test
    void getUserRealtimeDataReturnsOfflineWhenMissing() {
        when(realtimeMapper.getUserRealtimeData("E404")).thenReturn(null);

        RealtimeUserDetailView result = realtimeService.getUserRealtimeData("E404");

        assertEquals("E404", result.userCode());
        assertEquals("offline", result.status());
    }

    @Test
    void getRealtimeAlertsMapsTypedRows() {
        List<RealtimeAlertRow> rows = new ArrayList<>();
        rows.add(null);
        RealtimeAlertRow row = new RealtimeAlertRow();
        row.setId(9001L);
        row.setUserCode("E009");
        row.setUserName("赵六");
        row.setDeptName("通风队");
        row.setWarningType("心率过高");
        row.setIndicatorName("心率");
        row.setIndicatorValue("122");
        row.setWarningLevel("高危");
        row.setHandled(null);
        row.setCreateTime("2026-05-07 11:00:00");
        rows.add(row);

        when(realtimeMapper.getRecentAlerts(10)).thenReturn(rows);

        List<RealtimeAlertView> result = realtimeService.getRealtimeAlerts(10);

        assertEquals(1, result.size());
        assertEquals(9001L, result.get(0).id());
        assertEquals("E009", result.get(0).userCode());
        assertEquals("高危", result.get(0).warningLevel());
        assertFalse(result.get(0).handled());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void forceOnlineUsersCacheExpired(RealtimeUserPageView cachedPage, int page, int size) {
        try {
            Field cacheField = RealtimeService.class.getDeclaredField("onlineUsersCache");
            cacheField.setAccessible(true);
            ConcurrentHashMap cache = (ConcurrentHashMap) cacheField.get(realtimeService);

            Class<?> entryClass = Class.forName("com.xzkj.health.service.RealtimeService$CacheEntry");
            Constructor<?> constructor = entryClass.getDeclaredConstructor(Object.class, long.class);
            constructor.setAccessible(true);
            Object expiredEntry = constructor.newInstance(cachedPage, 0L);

            cache.put(HealthCacheKeys.key("online-users", page, size), expiredEntry);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("failed to expire realtime cache for test", ex);
        }
    }
}
