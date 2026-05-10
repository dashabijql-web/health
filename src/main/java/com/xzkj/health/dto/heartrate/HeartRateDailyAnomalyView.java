package com.xzkj.health.dto.heartrate;

public record HeartRateDailyAnomalyView(
        String date,
        int anomalyCount
) {
}
