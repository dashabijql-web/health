package com.xzkj.health.dto.realtime;

public record RealtimeUserView(
        Long id,
        String userCode,
        String userName,
        Integer gender,
        Integer age,
        String deptName,
        Integer heartRate,
        Integer bloodOxygen,
        Integer steps,
        Integer calories,
        Double temperature,
        Double sleepHours,
        Integer bloodPressureHigh,
        Integer bloodPressureLow,
        Integer pressure,
        String status,
        String lastUpdate,
        String imei
) {
}
