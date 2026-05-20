package com.xzkj.health.service;

import com.xzkj.health.config.datasource.HealthDataSourceContext;
import com.xzkj.health.config.datasource.HealthDataSourceKey;
import com.xzkj.health.config.datasource.WatchDataSourceResolver;
import com.xzkj.health.model.Device;
import com.xzkj.health.service.watch.WatchBehaviorAlertService;
import com.xzkj.health.service.watch.WatchDataPersistenceService;
import com.xzkj.health.service.watch.WatchDeviceContext;
import com.xzkj.health.service.watch.WatchDeviceContextService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataProcessServiceTest {

    @Mock
    private DeviceManagerService deviceManager;

    @Mock
    private WatchDataSourceResolver watchDataSourceResolver;

    @Mock
    private WatchDeviceContextService watchDeviceContextService;

    @Mock
    private WatchDataPersistenceService watchDataPersistenceService;

    @Mock
    private WatchBehaviorAlertService watchBehaviorAlertService;

    @InjectMocks
    private DataProcessService dataProcessService;

    @AfterEach
    void clearContext() {
        HealthDataSourceContext.clear();
    }

    @Test
    void saveHeartbeatUsesResolvedSimulatorSourceWhenPersistingData() {
        String imei = "359456780012345";
        Device device = new Device();
        device.setId(101L);
        WatchDeviceContext context = new WatchDeviceContext(device, null);

        when(watchDataSourceResolver.resolveWatchSource(imei)).thenReturn(HealthDataSourceKey.OLD);
        when(watchDeviceContextService.resolve(imei, null)).thenReturn(context);

        AtomicReference<HealthDataSourceKey> seenSource = new AtomicReference<>();
        doAnswer(invocation -> {
            seenSource.set(HealthDataSourceContext.get());
            return null;
        }).when(watchDataPersistenceService).persist(eq(context), eq(imei), eq("heartbeat"), anyMap());

        dataProcessService.saveHeartbeat(imei, "ok", "123", "0", "456");

        verify(watchDataSourceResolver).resolveWatchSource(imei);
        verify(watchDeviceContextService).resolve(imei, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(watchDataPersistenceService).persist(eq(context), eq(imei), eq("heartbeat"), dataCaptor.capture());
        assertEquals(123, dataCaptor.getValue().get("steps"));
        assertEquals(456, dataCaptor.getValue().get("calories"));
        assertEquals(HealthDataSourceKey.OLD, seenSource.get());
        assertNull(HealthDataSourceContext.get());
    }

    @Test
    void saveAlertUsesResolvedSourceWhenDelegatingBehaviorWarning() {
        String imei = "359456780012345";

        when(watchDataSourceResolver.resolveWatchSource(imei)).thenReturn(HealthDataSourceKey.OLD);

        AtomicReference<HealthDataSourceKey> seenSource = new AtomicReference<>();
        doAnswer(invocation -> {
            seenSource.set(HealthDataSourceContext.get());
            return null;
        }).when(watchBehaviorAlertService).saveAlert(imei, "SOS");

        dataProcessService.saveAlert(imei, "SOS", "raw");

        verify(watchBehaviorAlertService).saveAlert(imei, "SOS");
        assertEquals(HealthDataSourceKey.OLD, seenSource.get());
        assertNull(HealthDataSourceContext.get());
    }
}
