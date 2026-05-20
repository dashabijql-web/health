package com.xzkj.health.service;

import com.xzkj.health.dto.riskwarning.RiskWarningDeptStatView;
import com.xzkj.health.dto.riskwarning.RiskWarningOverviewView;
import com.xzkj.health.dto.riskwarning.RiskWarningPageView;
import com.xzkj.health.dto.riskwarning.RiskWarningTrendView;
import com.xzkj.health.dto.riskwarning.RiskWarningTypeCountView;
import com.xzkj.health.mapper.RiskWarningMapper;
import com.xzkj.health.observability.HealthMetricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskWarningServiceTest {

    @Mock
    private RiskWarningMapper riskWarningMapper;

    @Mock
    private HealthMetricsService healthMetricsService;

    @InjectMocks
    private RiskWarningService riskWarningService;

    @Test
    void mapsCoreQueriesToTypedViews() {
        when(riskWarningMapper.getWarningOverview("2026-05-01", "2026-05-07"))
                .thenReturn(Map.of(
                        "heartRateCount", 3,
                        "sleepCount", 2,
                        "bloodOxygenCount", 4,
                        "temperatureCount", 5,
                        "pressureCount", 6,
                        "totalWarnings", 20,
                        "handledWarnings", 12,
                        "pendingWarnings", 8,
                        "dangerCount", 7,
                        "warningCount", 13
                ));
        when(riskWarningMapper.getWarningList(null, null, null, null, "2026-05-01", "2026-05-07", 0, 10))
                .thenReturn(List.of(warningRow()));
        when(riskWarningMapper.countWarnings(null, null, null, null, "2026-05-01", "2026-05-07"))
                .thenReturn(1);
        when(riskWarningMapper.getWarningTrendByType(7))
                .thenReturn(List.of(Map.of(
                        "date", "2026-05-07",
                        "heartRate", 1,
                        "bloodOxygen", 2,
                        "sleep", 3,
                        "temperature", 4,
                        "pressure", 5
                )));
        when(riskWarningMapper.getDeptWarningStats("2026-05-01", "2026-05-07"))
                .thenReturn(List.of(Map.of(
                        "deptName", "综采队",
                        "heartRate", 1,
                        "bloodOxygen", 2,
                        "sleep", 3,
                        "temperature", 4,
                        "pressure", 5,
                        "total", 15
                )));
        when(riskWarningMapper.getTypeDistribution())
                .thenReturn(List.of(Map.of("type", "心率异常", "count", 9)));

        RiskWarningOverviewView overview = riskWarningService.getWarningStats("2026-05-01", "2026-05-07");
        RiskWarningPageView page = riskWarningService.getWarningList(null, null, null, null, "2026-05-01", "2026-05-07", 1, 10);
        RiskWarningTrendView trend = riskWarningService.getWarningTrend(7);
        List<RiskWarningDeptStatView> deptStats = riskWarningService.getDeptWarningStats("2026-05-01", "2026-05-07");
        List<RiskWarningTypeCountView> typeDistribution = riskWarningService.getTypeDistribution();

        assertEquals(3, overview.heartRateCount());
        assertEquals(60, overview.handledRate());
        assertEquals(1, page.list().size());
        assertEquals("心率异常", page.list().get(0).warningType());
        assertEquals("05-07", trend.dates().get(0));
        assertEquals(15, deptStats.get(0).total());
        assertEquals(9, typeDistribution.get(0).count());
    }

    private Map<String, Object> warningRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1L);
        row.put("userName", "张三");
        row.put("userCode", "EMP1001");
        row.put("deptName", "综采队");
        row.put("gender", 1);
        row.put("age", 32);
        row.put("warningType", "心率异常");
        row.put("warningLevel", "高危");
        row.put("warningValue", "120");
        row.put("indicatorName", "heartRate");
        row.put("handled", false);
        row.put("createTime", "2026-05-07 10:00:00");
        return row;
    }
}
