package com.xzkj.health.dto.portrait;

public record PortraitVitalsView(
        Integer heartRate,
        Integer bloodOxygen,
        Double temperature,
        Integer systolic,
        Integer diastolic,
        Integer pressure,
        Integer steps,
        Integer calories
) {
}
