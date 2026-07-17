package com.xzkj.health.service;

import com.xzkj.health.dto.commandcenter.CommandCenterDashboardSummaryView;
import com.xzkj.health.dto.commandcenter.DeviceOperationalSummaryView;
import com.xzkj.health.dto.commandcenter.PreShiftReviewSummaryView;
import com.xzkj.health.dto.dashboard.PreShiftComplianceView;
import com.xzkj.health.dto.riskwarning.RiskWarningPageView;
import com.xzkj.health.mapper.CommandCenterIncidentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandCenterDashboardSummaryServiceTest {

    @Mock
    private RiskWarningService riskWarningService;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private CommandCenterIncidentMapper incidentMapper;

    @Mock
    private PreShiftReviewService preShiftReviewService;

    @Mock
    private DeviceOperationalService deviceOperationalService;

    @InjectMocks
    private CommandCenterDashboardSummaryService summaryService;

    @Test
    void buildsTotalsFromServerCountsAndOperationalFacts() {
        when(riskWarningService.getWarningListByTimeWindow(
                eq(null), eq(null), anyString(), anyString(), eq(1), eq(1)))
                .thenReturn(page(3401));
        when(riskWarningService.getWarningListByTimeWindow(
                eq(null), eq(false), anyString(), anyString(), eq(1), eq(1)))
                .thenReturn(page(200));
        when(riskWarningService.getWarningListByTimeWindow(
                eq("高危"), eq(false), anyString(), anyString(), eq(1), eq(1)))
                .thenReturn(page(94));
        when(incidentMapper.getOpenWorkflowSummary(anyString(), anyString()))
                .thenReturn(Map.of("assignedOpen", 12, "overdueOpen", 5));
        when(dashboardService.getPreShiftCompliance())
                .thenReturn(new PreShiftComplianceView(1116, 1089, 27, 98));
        when(preShiftReviewService.getTodaySummary())
                .thenReturn(new PreShiftReviewSummaryView(27, 4));
        when(deviceOperationalService.getSummary())
                .thenReturn(new DeviceOperationalSummaryView(1000, 960, 40, 96, 12, 6, 3));

        CommandCenterDashboardSummaryView result = summaryService.getSummary();

        assertEquals(3401, result.warning().todayNew());
        assertEquals(200, result.warning().pendingTotal());
        assertEquals(94, result.warning().criticalPending());
        assertEquals(188, result.warning().unassignedTotal());
        assertEquals(5, result.warning().overdueTotal());
        assertEquals(960, result.device().online());
        assertEquals(40, result.device().offline());
        assertEquals("AVAILABLE", result.device().lowBattery().status());
        assertEquals(12, result.device().lowBattery().value());
        assertEquals(6, result.device().dataInterrupted().value());
        assertEquals(3, result.device().faulted().value());
        assertEquals(27, result.admission().awaitingReview().value());
        assertEquals(4, result.admission().retestOverdue().value());
    }

    private RiskWarningPageView page(int total) {
        return new RiskWarningPageView(List.of(), total, 1, 1);
    }
}
