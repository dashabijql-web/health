package com.xzkj.health.controller;

import com.xzkj.health.common.Result;
import com.xzkj.health.mapper.RiskWarningMapper;
import com.xzkj.health.model.Device;
import com.xzkj.health.model.DeviceUser;
import com.xzkj.health.service.DeviceDataBufferService;
import com.xzkj.health.service.DeviceManagerService;
import com.xzkj.health.service.DeviceService;
import com.xzkj.health.service.DeviceUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * 设备管理HTTP接口
 * 提供Web管理界面和API
 */
@RestController
@RequestMapping("/api/device")
@Slf4j
public class DeviceController {

    @Autowired
    private DeviceManagerService deviceManager;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceUserService deviceUserService;

    @Autowired
    private DeviceDataBufferService deviceDataBufferService;

    @Autowired
    private RiskWarningMapper riskWarningMapper;

    /**
     * 获取设备列表（所有设备，包含在线/离线状态）
     * GET http://localhost:8080/health/api/device/online
     */
    @GetMapping("/online")
    public Result<Map<String, Object>> getOnlineDevices() {
        try {
            // 查询数据库中所有设备
            List<Device> allDevices = deviceService.list();
            List<Map<String, Object>> deviceList = new ArrayList<>();

            // 批量查询绑定和缓冲计数（2次SQL替代N*2次）
            Map<Long, DeviceUser> bindingMap = deviceUserService.getAllCurrentBindings();
            Map<Long, Integer> bufferCountMap = deviceDataBufferService.getAllPendingCounts();

            // 查询近24小时有未处理预警的员工ID集合
            Set<Long> warningEmpIds;
            try {
                warningEmpIds = new HashSet<>(riskWarningMapper.getEmpIdsWithUnhandledWarnings());
            } catch (Exception e) {
                log.warn("查询预警员工ID失败，hasWarning 将全部置 false: {}", e.getMessage());
                warningEmpIds = new HashSet<>();
            }

            // 构建详细设备信息
            for (Device device : allDevices) {
                Map<String, Object> deviceInfo = new HashMap<>();
                deviceInfo.put("id", device.getId());
                deviceInfo.put("imei", device.getImei());

                // 实时检查设备是否在线
                boolean isOnline = deviceManager.isDeviceOnline(device.getImei());
                deviceInfo.put("status", isOnline ? 1 : 0);
                deviceInfo.put("lastOnlineTime", device.getLastOnlineTime());

                // 从批量查询结果取绑定信息
                DeviceUser binding = bindingMap.get(device.getId());
                deviceInfo.put("bindStatus", binding != null);
                deviceInfo.put("userName", binding != null ? binding.getRealName() : null);
                deviceInfo.put("deptName", binding != null ? binding.getDeptName() : null);

                // 标记是否有未处理预警
                boolean hasWarning = binding != null && warningEmpIds.contains(binding.getEmpId());
                deviceInfo.put("hasWarning", hasWarning);

                // 电量（0-100，null=未知）
                deviceInfo.put("batteryLevel", device.getBatteryLevel());

                // 从批量查询结果取缓冲计数
                deviceInfo.put("bufferCount", bufferCountMap.getOrDefault(device.getId(), 0));

                deviceList.add(deviceInfo);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("devices", deviceList);
            data.put("count", deviceList.size());
            data.put("timestamp", System.currentTimeMillis());
            return Result.ok("查询成功", data);
        } catch (Exception e) {
            log.error("获取设备列表失败", e);
            return Result.error("获取设备列表失败: " + e.getMessage());
        }
    }

    /**
     * 转移缓冲数据到用户
     * POST http://localhost:8080/health/api/device/{deviceId}/transfer-buffer?userId=1
     */
    @PostMapping("/{deviceId}/transfer-buffer")
    public Result<Map<String, Object>> transferBufferData(
            @PathVariable Long deviceId,
            @RequestParam Long userId) {
        Map<String, Object> result = deviceService.transferBufferDataToUser(deviceId, userId);
        Integer resultCode = (Integer) result.get("result");
        if (resultCode != null && resultCode == 1) {
            return Result.ok("转移成功", result);
        } else {
            String message = (String) result.get("message");
            return Result.error(message != null ? message : "转移失败");
        }
    }

    /**
     * 删除设备缓冲数据
     * DELETE http://localhost:8080/health/api/device/{deviceId}/buffer
     */
    @DeleteMapping("/{deviceId}/buffer")
    public Result<Void> deleteBufferData(@PathVariable Long deviceId) {
        deviceDataBufferService.deleteByDeviceId(deviceId);
        return Result.ok("删除成功", null);
    }

    /**
     * 发送文字消息到手表 (BP40 协议)
     * POST /api/device/message
     * { "imei": "...", "text": "..." }
     */
    @PostMapping("/message")
    public Result<Boolean> sendTextMessage(@RequestBody Map<String, String> request) {
        String imei = request.get("imei");
        String text = request.get("text");
        if (imei == null || imei.isEmpty() || text == null || text.trim().isEmpty()) {
            return Result.error("参数错误: imei 和 text 不能为空");
        }
        if (text.length() > 50) {
            return Result.error("消息内容不能超过 50 个字符");
        }
        try {
            // 转为 Unicode hex（UTF-16BE，每个字符 → 4位十六进制）
            StringBuilder unicode = new StringBuilder();
            for (char c : text.toCharArray()) {
                unicode.append(String.format("%04x", (int) c));
            }
            String serial = String.format("%06d", System.currentTimeMillis() % 1000000);
            boolean sent = deviceManager.sendCommand(imei, "BP40", serial, unicode.toString());
            if (sent) {
                log.info("手表消息发送成功: imei={}, text={}", imei, text);
                return Result.ok("消息发送成功", true);
            } else {
                return Result.error("设备不在线，消息发送失败");
            }
        } catch (Exception e) {
            log.error("发送手表消息异常", e);
            return Result.error("系统异常: " + e.getMessage());
        }
    }

    /**
     * 绑定设备到用户
     * POST http://localhost:8080/health/api/device/{deviceId}/bind?userId=1
     */
    @PostMapping("/{deviceId}/bind")
    public Result<Void> bindDeviceToUser(
            @PathVariable Long deviceId,
            @RequestParam Long userId) {
        try {
            deviceUserService.bindDeviceToUser(deviceId, userId);
            return Result.ok("绑定成功", null);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("绑定设备失败: deviceId={}, userId={}", deviceId, userId, e);
            return Result.error("绑定失败: " + e.getMessage());
        }
    }

    /**
     * 解绑设备
     * POST http://localhost:8080/health/api/device/{deviceId}/unbind
     */
    @PostMapping("/{deviceId}/unbind")
    public Result<Void> unbindDevice(@PathVariable Long deviceId) {
        try {
            deviceUserService.unbindDevice(deviceId);
            return Result.ok("解绑成功", null);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("解绑设备失败: deviceId={}", deviceId, e);
            return Result.error("解绑失败: " + e.getMessage());
        }
    }
}