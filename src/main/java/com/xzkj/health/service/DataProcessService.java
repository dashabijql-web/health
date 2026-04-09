package com.xzkj.health.service;

import com.alibaba.fastjson2.JSON;
import com.xzkj.health.mapper.UserListMapper;
import com.xzkj.health.model.Device;
import com.xzkj.health.model.DeviceDataBuffer;
import com.xzkj.health.model.DeviceUser;
import com.xzkj.health.model.HealthRecord;
import com.xzkj.health.model.entity.AlertConfig;
import com.xzkj.health.protocol.WatchMessage;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备数据处理服务
 * 核心职责：根据设备绑定状态，决定数据保存到缓冲表还是健康记录表
 */
@Slf4j
@Service
public class DataProcessService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private HealthRecordService healthRecordService;

    @Autowired
    private DeviceManagerService deviceManager;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceUserService deviceUserService;

    @Autowired
    private DeviceDataBufferService deviceDataBufferService;

    @Autowired
    private UserListMapper userListMapper;

    @Autowired
    private RiskWarningService riskWarningService;

    @Autowired
    private RedisHealthBufferService redisHealthBufferService;

    @Autowired
    private AlertConfigService alertConfigService;

    @Autowired
    private com.xzkj.health.mapper.EmployeeMapper employeeMapper;

    // ─── 辅助方法 ─────────────────────────────────────────────────

    /**
     * 从 WatchMessage 中获取 IMEI
     */
    private String getImeiFromMessage(WatchMessage message) {
        if (message == null) {
            return null;
        }

        String imei = message.getImei();
        if (imei != null && !imei.isEmpty()) {
            return imei;
        }

        Channel channel = message.getChannel();
        if (channel != null && deviceManager != null) {
            imei = deviceManager.getImeiByChannel(channel);
            if (imei != null && !imei.isEmpty()) {
                log.debug("从 Channel 获取到 IMEI: {}", imei);
                return imei;
            }
        }

        log.warn("无法从消息中获取 IMEI: protocolCode={}", message.getProtocolCode());
        return null;
    }

    /**
     * 安全解析整数
     */
    private int parseIntSafe(String value, int defaultValue) {
        try {
            if (value == null || value.isEmpty()) return defaultValue;
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全解析浮点数
     */
    private double parseDoubleSafe(String value, double defaultValue) {
        try {
            if (value == null || value.isEmpty()) return defaultValue;
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取当前时间字符串
     */
    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    /**
     * 根据心率计算压力指数
     *
     * 压力指数范围：30-100
     * 正常范围：50-70
     *
     * 计算逻辑（简化版，基于心率）：
     *   静息心率 60-80 → 压力指数 50-60 (正常)
     *   心率偏高 80-100 → 压力指数 60-75 (轻度压力)
     *   心率高 100-120 → 压力指数 75-90 (中度压力)
     *   心率很高 >120 → 压力指数 90-100 (高压力)
     *   心率偏低 <60 → 压力指数 40-50 (放松/可能异常)
     *
     * @param heartRate 心率 (bpm)
     * @return 压力指数 (30-100)
     */
    private int calculatePressureIndex(int heartRate) {
        if (heartRate < 60) {
            // 心率偏低: 40-50
            return Math.max(30, 50 - (60 - heartRate) / 2);
        } else if (heartRate <= 80) {
            // 正常范围: 50-60
            return 50 + (heartRate - 60) / 2;
        } else if (heartRate <= 100) {
            // 轻度升高: 60-75
            return 60 + (heartRate - 80) * 3 / 4;
        } else if (heartRate <= 120) {
            // 中度升高: 75-90
            return 75 + (heartRate - 100) * 3 / 4;
        } else {
            // 高度升高: 90-100
            return Math.min(100, 90 + (heartRate - 120) / 4);
        }
    }

    /**
     * 保存数据到缓冲表
     */
    private void saveToBuffer(Device device, String imei, String dataType, Map<String, Object> dataMap) {
        try {
            DeviceDataBuffer buffer = new DeviceDataBuffer();
            buffer.setDeviceId(device.getId());
            buffer.setImei(imei);
            buffer.setDataType(dataType);
            buffer.setDataJson(JSON.toJSONString(dataMap));
            buffer.setRecordTime(LocalDateTime.now());
            buffer.setReceiveTime(LocalDateTime.now());
            buffer.setIsTransferred(false);

            deviceDataBufferService.save(buffer);
            log.debug("数据已暂存到缓冲表: IMEI={}, 类型={}, 数据={}", imei, dataType, dataMap);
        } catch (Exception e) {
            log.error("保存到缓冲表失败: IMEI={}, 类型={}", imei, dataType, e);
        }
    }

    /**
     * 保存数据到健康记录表
     */
    private void saveToHealthRecord(String imei, DeviceUser currentBind, Map<String, Object> dataMap) {
        try {
            // 查询员工详细信息
            Map<String, Object> empInfo = userListMapper.getEmpDetail(currentBind.getEmpId());
            if (empInfo == null) {
                log.error("无法获取员工信息: empId={}", currentBind.getEmpId());
                return;
            }

            HealthRecord record = new HealthRecord();
            String userCode = (String) empInfo.get("userCode");
            record.setUserCode(userCode != null ? userCode : "");

            // 根据dataMap中的数据填充健康记录
            // 使用 instanceof Number 而非 containsKey + 强转，防止 value 为 null 时 NPE
            Object v;
            if ((v = dataMap.get("heart_rate")) instanceof Number)
                record.setHeartRate(((Number) v).intValue());
            if ((v = dataMap.get("blood_oxygen")) instanceof Number)
                record.setBloodOxygen(((Number) v).intValue());
            if ((v = dataMap.get("temperature")) instanceof Number)
                record.setTemperature(((Number) v).intValue());
            if ((v = dataMap.get("blood_pressure_high")) instanceof Number)
                record.setBloodPressureHigh(((Number) v).intValue());
            if ((v = dataMap.get("blood_pressure_low")) instanceof Number)
                record.setBloodPressureLow(((Number) v).intValue());
            if ((v = dataMap.get("pressure")) instanceof Number)
                record.setPressure(((Number) v).intValue());
            if ((v = dataMap.get("steps")) instanceof Number)
                record.setSteps(((Number) v).intValue());
            if ((v = dataMap.get("calories")) instanceof Number)
                record.setCalories(((Number) v).intValue());
            if ((v = dataMap.get("sleep_minutes")) instanceof Number)
                record.setSleepMinutes(((Number) v).intValue());

            record.setTime(now());
            redisHealthBufferService.push(record);

            log.debug("数据已推入Redis缓冲: IMEI={}, userCode={}, 数据={}",
                    imei, record.getUserCode(), dataMap);

            // 检查健康数据并生成预警（使用员工工种对应的 risk_level 阈值）
            Object rlObj = empInfo.get("riskLevel");
            Integer riskLevel = rlObj instanceof Number ? ((Number) rlObj).intValue() : null;
            checkHealthDataAndGenerateWarnings(userCode, record, riskLevel);
        } catch (Exception e) {
            log.error("保存到健康记录失败: IMEI={}", imei, e);
        }
    }

    /**
     * 检查健康数据是否超标并生成预警
     * 阈值从 alert_config 表读取，按员工工种 risk_level 匹配对应行
     *
     * configType 含义：1=心率  2=血氧  3=体温  4=收缩压  5=压力指数
     */
    private void checkHealthDataAndGenerateWarnings(String userCode, HealthRecord record, Integer riskLevel) {
        try {
            Map<Integer, AlertConfig> cfgMap = alertConfigService.getConfigMap(riskLevel);

            // 告警冷却时间（分钟）：同用户同指标在冷却期内只产生一条未处理告警
            // 血氧使用 24h 冷却（每日一次已足够，避免高频采样导致预警率虚高）
            final int DEDUP_MINUTES    = 240; // 同一人同一指标 4 小时内不重复预警
            final int DEDUP_OXYGEN_MIN = 1440; // 血氧：24 小时冷却

            // 心率预警 (configType=1)
            if (record.getHeartRate() != null) {
                AlertConfig cfg = cfgMap.get(1);
                if (cfg != null && cfg.getEnabled() == 1) {
                    int hr = record.getHeartRate();
                    double critLow  = cfg.getCriticalLow().doubleValue();
                    double critHigh = cfg.getCriticalHigh().doubleValue();
                    double midLow   = cfg.getWarnMidLow()  != null ? cfg.getWarnMidLow().doubleValue()  : critLow;
                    double midHigh  = cfg.getWarnMidHigh() != null ? cfg.getWarnMidHigh().doubleValue() : critHigh;
                    double warnLow  = cfg.getWarnLow().doubleValue();
                    double warnHigh = cfg.getWarnHigh().doubleValue();
                    if ((hr < critLow || hr > critHigh) && !riskWarningService.hasRecentWarning(userCode, "心率", DEDUP_MINUTES)) {
                        riskWarningService.insertWarning(userCode, "心率异常", "心率", hr + " bpm", "高危");
                    } else if ((hr < midLow || hr > midHigh) && !riskWarningService.hasRecentWarning(userCode, "心率", DEDUP_MINUTES)) {
                        riskWarningService.insertWarning(userCode, "心率异常", "心率", hr + " bpm", "中危");
                    } else if ((hr < warnLow || hr > warnHigh) && !riskWarningService.hasRecentWarning(userCode, "心率", DEDUP_MINUTES)) {
                        riskWarningService.insertWarning(userCode, "心率异常", "心率", hr + " bpm", "低危");
                    }
                }
            }

            // 血氧预警 (configType=2)
            if (record.getBloodOxygen() != null) {
                AlertConfig cfg = cfgMap.get(2);
                if (cfg != null && cfg.getEnabled() == 1) {
                    int oxygen = record.getBloodOxygen();
                    double critLow = cfg.getCriticalLow().doubleValue();
                    double midLow  = cfg.getWarnMidLow() != null ? cfg.getWarnMidLow().doubleValue() : critLow;
                    double warnLow = cfg.getWarnLow().doubleValue();
                    if (oxygen < critLow && !riskWarningService.hasRecentWarning(userCode, "血氧", DEDUP_OXYGEN_MIN)) {
                        riskWarningService.insertWarning(userCode, "血氧过低", "血氧", oxygen + "%", "高危");
                    } else if (oxygen < midLow && !riskWarningService.hasRecentWarning(userCode, "血氧", DEDUP_OXYGEN_MIN)) {
                        riskWarningService.insertWarning(userCode, "血氧偏低", "血氧", oxygen + "%", "中危");
                    } else if (oxygen < warnLow && !riskWarningService.hasRecentWarning(userCode, "血氧", DEDUP_OXYGEN_MIN)) {
                        riskWarningService.insertWarning(userCode, "血氧偏低", "血氧", oxygen + "%", "低危");
                    }
                }
            }

            // 体温预警 (configType=3)，temperature 以整数×10存储（375=37.5°C）
            if (record.getTemperature() != null) {
                AlertConfig cfg = cfgMap.get(3);
                if (cfg != null && cfg.getEnabled() == 1) {
                    double realTemp = record.getTemperature() / 10.0;
                    String tempStr = String.format("%.1f°C", realTemp);
                    double critLow  = cfg.getCriticalLow().doubleValue();
                    double critHigh = cfg.getCriticalHigh().doubleValue();
                    double midLow   = cfg.getWarnMidLow()  != null ? cfg.getWarnMidLow().doubleValue()  : critLow;
                    double midHigh  = cfg.getWarnMidHigh() != null ? cfg.getWarnMidHigh().doubleValue() : critHigh;
                    double warnLow  = cfg.getWarnLow().doubleValue();
                    double warnHigh = cfg.getWarnHigh().doubleValue();
                    if ((realTemp < critLow || realTemp > critHigh) && !riskWarningService.hasRecentWarning(userCode, "体温", DEDUP_MINUTES)) {
                        riskWarningService.insertWarning(userCode, "体温异常", "体温", tempStr, "高危");
                    } else if ((realTemp < midLow || realTemp > midHigh) && !riskWarningService.hasRecentWarning(userCode, "体温", DEDUP_MINUTES)) {
                        riskWarningService.insertWarning(userCode, "体温异常", "体温", tempStr, "中危");
                    } else if ((realTemp < warnLow || realTemp > warnHigh) && !riskWarningService.hasRecentWarning(userCode, "体温", DEDUP_MINUTES)) {
                        riskWarningService.insertWarning(userCode, "体温异常", "体温", tempStr, "低危");
                    }
                }
            }

            // 收缩压预警 (configType=4)
            if (record.getBloodPressureHigh() != null) {
                AlertConfig cfg = cfgMap.get(4);
                if (cfg != null && cfg.getEnabled() == 1) {
                    int high = record.getBloodPressureHigh();
                    double critHigh = cfg.getCriticalHigh().doubleValue();
                    double midHigh  = cfg.getWarnMidHigh() != null ? cfg.getWarnMidHigh().doubleValue() : critHigh;
                    double warnHigh = cfg.getWarnHigh().doubleValue();
                    if (high > critHigh && !riskWarningService.hasRecentWarning(userCode, "收缩压", DEDUP_MINUTES)) {
                        riskWarningService.insertWarning(userCode, "血压过高", "收缩压", high + " mmHg", "高危");
                    } else if (high > midHigh && !riskWarningService.hasRecentWarning(userCode, "收缩压", DEDUP_MINUTES)) {
                        riskWarningService.insertWarning(userCode, "血压偏高", "收缩压", high + " mmHg", "中危");
                    } else if (high > warnHigh && !riskWarningService.hasRecentWarning(userCode, "收缩压", DEDUP_MINUTES)) {
                        riskWarningService.insertWarning(userCode, "血压偏高", "收缩压", high + " mmHg", "低危");
                    }
                }
            }

            // 压力指数预警 (configType=5)
            if (record.getPressure() != null) {
                AlertConfig cfg = cfgMap.get(5);
                if (cfg != null && cfg.getEnabled() == 1) {
                    int pressure = record.getPressure();
                    double critHigh = cfg.getCriticalHigh().doubleValue();
                    double midHigh  = cfg.getWarnMidHigh() != null ? cfg.getWarnMidHigh().doubleValue() : critHigh;
                    double warnHigh = cfg.getWarnHigh().doubleValue();
                    if (pressure > critHigh && !riskWarningService.hasRecentWarning(userCode, "压力指数", DEDUP_MINUTES)) {
                        riskWarningService.insertWarning(userCode, "压力过大", "压力指数", String.valueOf(pressure), "高危");
                    } else if (pressure > midHigh && !riskWarningService.hasRecentWarning(userCode, "压力指数", DEDUP_MINUTES)) {
                        riskWarningService.insertWarning(userCode, "压力偏高", "压力指数", String.valueOf(pressure), "中危");
                    } else if (pressure > warnHigh && !riskWarningService.hasRecentWarning(userCode, "压力指数", DEDUP_MINUTES)) {
                        riskWarningService.insertWarning(userCode, "压力偏高", "压力指数", String.valueOf(pressure), "低危");
                    }
                }
            }
        } catch (Exception e) {
            log.error("检查健康数据预警失败: userCode={}", userCode, e);
        }
    }

    // ─── 设备事件处理 ─────────────────────────────────────────────

    /**
     * 保存设备登录记录
     */
    @Async
    public void saveDeviceLogin(String imei, String remoteAddress) {
        log.info("设备上线: IMEI={}, 地址={}", imei, remoteAddress);
    }

    /**
     * 保存 GPS 定位数据
     */
    @Async
    public void saveGpsLocation(String imei, Object gpsData) {
        log.debug("GPS定位: IMEI={}, 数据={}", imei, gpsData);
    }

    /**
     * 保存基站定位数据
     */
    @Async
    public void saveBaseStationLocation(String imei, Object params) {
        log.debug("基站定位: IMEI={}", imei);
    }

    /**
     * 保存心跳数据（包含步数和卡路里）
     */
    @Async
    public void saveHeartbeat(String imei, String status, String steps, String rollovers, String calories) {
        try {
            int stepCount = parseIntSafe(steps, 0);
            int caloriesCount = parseIntSafe(calories, 0);

            // 如果步数和卡路里都为0，跳过保存
            if (stepCount <= 0 && caloriesCount <= 0) {
                return;
            }

            // 自动注册设备
            Device device = deviceService.getOrCreateByImei(imei);
            deviceService.updateOnlineStatus(device.getId(), null);

            // 检查绑定状态
            DeviceUser currentBind = deviceUserService.getCurrentBinding(device.getId());

            Map<String, Object> dataMap = new HashMap<>();
            if (stepCount > 0) {
                dataMap.put("steps", stepCount);
            }
            if (caloriesCount > 0) {
                dataMap.put("calories", caloriesCount);
            }

            if (currentBind == null) {
                saveToBuffer(device, imei, "heartbeat", dataMap);
            } else {
                saveToHealthRecord(imei, currentBind, dataMap);
            }
        } catch (Exception e) {
            log.error("保存心跳数据失败: IMEI={}", imei, e);
        }
    }

    /**
     * 保存报警数据
     */
    @Async
    public void saveAlert(String imei, String alertType, String alertData) {
        log.warn("设备报警: IMEI={}, 类型={}, 数据={}", imei, alertType, alertData);
        try {
            // 跌倒/SOS/房颤等行为类报警写入预警记录
            if (alertType == null || alertType.contains("未知") || alertType.contains("低电")
                    || alertType.contains("脱落") || alertType.contains("佩戴")) {
                return; // 这些类型不作为健康预警
            }
            Device device = deviceService.getOrCreateByImei(imei);
            DeviceUser binding = deviceUserService.getCurrentBinding(device.getId());
            if (binding == null) return;
            String userCode = binding.getEmpId() != null
                    ? employeeMapper.selectById(binding.getEmpId()).getEmpCode()
                    : imei;
            String level = alertType.contains("SOS") ? "高危" : alertType.contains("跌倒") || alertType.contains("房颤") ? "高危" : "中危";
            riskWarningService.insertWarning(userCode, alertType, "行为报警", alertType, level);
        } catch (Exception e) {
            log.error("保存报警记录失败: IMEI={}, 类型={}", imei, alertType, e);
        }
    }

    /**
     * 保存心率数据（AP49协议）
     */
    @Async
    public void saveHeartRate(String imei, String heartRate) {
        try {
            int hr = parseIntSafe(heartRate, 0);
            if (hr < 20 || hr > 300) {
                log.warn("心率数据超出合理范围，跳过保存: IMEI={}, 心率={}", imei, heartRate);
                return;
            }

            Device device = deviceService.getOrCreateByImei(imei);
            deviceService.updateOnlineStatus(device.getId(), null);
            DeviceUser currentBind = deviceUserService.getCurrentBinding(device.getId());

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("heart_rate", hr);
            // 自动计算压力指数
            dataMap.put("pressure", calculatePressureIndex(hr));

            if (currentBind == null) {
                saveToBuffer(device, imei, "heart_rate", dataMap);
            } else {
                saveToHealthRecord(imei, currentBind, dataMap);
            }
        } catch (Exception e) {
            log.error("保存心率失败: IMEI={}", imei, e);
        }
    }

    /**
     * 保存体温数据
     */
    @Async
    public void saveTemperature(WatchMessage message, String temperature, String battery) {
        try {
            String imei = getImeiFromMessage(message);
            if (imei == null || imei.isEmpty()) {
                log.warn("无法获取设备 IMEI，跳过体温数据保存");
                return;
            }

            double temp = parseDoubleSafe(temperature, 0);
            if (temp < 35.0 || temp > 42.0) {
                log.warn("体温数据超出合理范围: IMEI={}, 体温={}", imei, temperature);
                return;
            }

            Device device = deviceService.getOrCreateByImei(imei);
            deviceService.updateOnlineStatus(device.getId(), battery);
            DeviceUser currentBind = deviceUserService.getCurrentBinding(device.getId());

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("temperature", (int) Math.round(temp * 10));

            if (currentBind == null) {
                saveToBuffer(device, imei, "temperature", dataMap);
            } else {
                saveToHealthRecord(imei, currentBind, dataMap);
            }
        } catch (Exception e) {
            log.error("保存体温失败", e);
        }
    }

    /**
     * 保存血压数据（APHT协议：心率、收缩压、舒张压）
     */
    @Async
    public void saveBloodPressure(WatchMessage message, String heartRate,
                                  String highPressure, String lowPressure) {
        try {
            String imei = getImeiFromMessage(message);
            if (imei == null || imei.isEmpty()) {
                log.warn("无法获取设备 IMEI，跳过血压数据保存");
                return;
            }

            int high = parseIntSafe(highPressure, 0);
            int low = parseIntSafe(lowPressure, 0);
            int hr = parseIntSafe(heartRate, 0);

            if (high <= 0 || low <= 0) {
                log.warn("血压数据异常: IMEI={}, 高压={}, 低压={}", imei, highPressure, lowPressure);
                return;
            }

            Device device = deviceService.getOrCreateByImei(imei);
            deviceService.updateOnlineStatus(device.getId(), null);
            DeviceUser currentBind = deviceUserService.getCurrentBinding(device.getId());

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("blood_pressure_high", high);
            dataMap.put("blood_pressure_low", low);
            if (hr >= 20 && hr <= 300) {
                dataMap.put("heart_rate", hr);
                // 自动计算压力指数
                dataMap.put("pressure", calculatePressureIndex(hr));
            }

            if (currentBind == null) {
                saveToBuffer(device, imei, "blood_pressure", dataMap);
            } else {
                saveToHealthRecord(imei, currentBind, dataMap);
            }
        } catch (Exception e) {
            log.error("保存血压失败", e);
        }
    }

    /**
     * 保存睡眠数据
     */
    @Async
    public void saveSleep(WatchMessage message, String deepSleep, String lightSleep) {
        try {
            String imei = getImeiFromMessage(message);
            if (imei == null || imei.isEmpty()) {
                log.warn("无法获取设备 IMEI，跳过睡眠数据保存");
                return;
            }

            int deep = parseIntSafe(deepSleep, 0);
            int light = parseIntSafe(lightSleep, 0);

            if (deep <= 0 && light <= 0) {
                log.warn("睡眠数据无效: IMEI={}, 深睡={}, 浅睡={}", imei, deepSleep, lightSleep);
                return;
            }

            int totalSleep = deep + light;  // 总睡眠时长（分钟）

            Device device = deviceService.getOrCreateByImei(imei);
            deviceService.updateOnlineStatus(device.getId(), null);
            DeviceUser currentBind = deviceUserService.getCurrentBinding(device.getId());

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("sleep_minutes", totalSleep);

            if (currentBind == null) {
                saveToBuffer(device, imei, "sleep", dataMap);
            } else {
                saveToHealthRecord(imei, currentBind, dataMap);
            }

            log.debug("睡眠数据已保存: IMEI={}, 深睡={}分钟, 浅睡={}分钟, 总计={}分钟",
                    imei, deep, light, totalSleep);
        } catch (Exception e) {
            log.error("保存睡眠数据失败", e);
        }
    }

    /**
     * 保存综合健康数据（APHP协议：心率、血压、血氧、血糖、体温）
     */
    @Async
    public void saveHealthData(WatchMessage message, String heartRate, String highPressure,
                               String lowPressure, String bloodOxygen, String bloodSugar,
                               String temperature) {
        try {
            String imei = getImeiFromMessage(message);
            if (imei == null || imei.isEmpty()) {
                log.warn("无法获取设备 IMEI，跳过综合健康数据保存");
                return;
            }

            int hr = parseIntSafe(heartRate, 0);
            int high = parseIntSafe(highPressure, 0);
            int low = parseIntSafe(lowPressure, 0);
            int oxygen = parseIntSafe(bloodOxygen, 0);
            double temp = parseDoubleSafe(temperature, 0);

            Device device = deviceService.getOrCreateByImei(imei);
            deviceService.updateOnlineStatus(device.getId(), null);
            DeviceUser currentBind = deviceUserService.getCurrentBinding(device.getId());

            Map<String, Object> dataMap = new HashMap<>();
            boolean hasValidData = false;

            if (hr >= 20 && hr <= 300) {
                dataMap.put("heart_rate", hr);
                // 自动计算压力指数
                dataMap.put("pressure", calculatePressureIndex(hr));
                hasValidData = true;
            }
            // 修复：血压应该存入 blood_pressure_high/low，而不是 pressure
            if (high > 0 && low > 0) {
                dataMap.put("blood_pressure_high", high);
                dataMap.put("blood_pressure_low", low);
                hasValidData = true;
            }
            if (oxygen >= 50 && oxygen <= 100) {
                dataMap.put("blood_oxygen", oxygen);
                hasValidData = true;
            }
            if (temp >= 35.0 && temp <= 42.0) {
                dataMap.put("temperature", (int) Math.round(temp * 10));
                hasValidData = true;
            }

            if (!hasValidData) {
                log.warn("综合健康数据无有效字段: IMEI={}", imei);
                return;
            }

            if (currentBind == null) {
                saveToBuffer(device, imei, "health_all", dataMap);
            } else {
                saveToHealthRecord(imei, currentBind, dataMap);
            }
        } catch (Exception e) {
            log.error("保存综合健康数据失败", e);
        }
    }

    // ─── 原始协议日志 ──────────────────────────────────────────────

    /**
     * 记录上行数据日志
     */
    @Async
    public void saveUplinkData(String imei, String protocolCode, String rawMessage) {
        log.debug("上行数据: IMEI={}, 协议号={}", imei, protocolCode);
    }

    /**
     * 记录设备响应日志
     */
    @Async
    public void saveDownlinkResponse(String imei, String protocolCode, String rawMessage) {
        log.debug("下行响应: IMEI={}, 协议号={}", imei, protocolCode);
    }

    /**
     * 记录原始协议数据
     */
    @Async
    public void saveProtocolData(String imei, String protocolCode, String rawMessage, String direction) {
        log.debug("协议数据: IMEI={}, 协议号={}, 方向={}", imei, protocolCode, direction);
    }
}
