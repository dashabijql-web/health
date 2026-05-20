package com.xzkj.health.dto.portrait;

import lombok.Data;

@Data
public class PortraitVitalsRow {
    private Integer heartRate;
    private Integer bloodOxygen;
    private Double temperature;
    private Integer systolic;
    private Integer diastolic;
    private Integer pressure;
    private Integer steps;
    private Integer calories;
}
