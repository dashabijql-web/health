package com.xzkj.health.controller;

import com.xzkj.health.common.Result;
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
import java.util.List;
import java.util.Map;
//        测试登录包：
//
//        text
//        IW*AP00*353456789012345#
//        应该收到响应：
//
//        text
//        IW*BP00*,20240115143025,8#
//        测试心跳包：
//
//        text
//        IW*AP03*,06000908000102,05555,30#
//        应该收到响应：
//
//        text
//        IWBP03#
//        测试定位包：
//
//        text
//        IW*AP01*080524A2232.9806N11404.9355E000.1061830323.8706000908000102,460,0,9520,3671#
//        应该收到响应：
//
//        text
//        IW*BP01*#
//        HTTP接口测试
//        查看在线设备：
//
//        text
//        GET http://localhost:8080/health/api/device/online
//        发送定位指令：
//
//        text
//        POST http://localhost:8080/health/api/device/locate/353456789012345
//        健康检查：
//
//        text
//        GET http://localhost:8080/health/api/device/health
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
     * 检查设备是否在线
     * GET http://localhost:8080/health/api/device/check/{imei}
     */
    @GetMapping("/check/{imei}")
    public Result<Map<String, Object>> checkDeviceOnline(@PathVariable String imei) {
        boolean online = deviceManager.isDeviceOnline(imei);
        Map<String, Object> data = new HashMap<>();
        data.put("online", online);
        data.put("imei", imei);
        return Result.ok(online ? "设备在线" : "设备离线", data);
    }

    /**
     * 发送指令到设备
     * POST http://localhost:8080/health/api/device/command
     * Content-Type: application/json
     * {
     *   "imei": "353456789012345",
     *   "protocolCode": "BP16",
     *   "params": ["080835"]
     * }
     */
    @PostMapping("/command")
    public Result<Boolean> sendCommand(@RequestBody Map<String, Object> request) {
        try {
            String imei = (String) request.get("imei");
            String protocolCode = (String) request.get("protocolCode");
            java.util.List<String> params = (java.util.List<String>) request.get("params");

            if (imei == null || imei.isEmpty() || protocolCode == null || protocolCode.isEmpty()) {
                return Result.error("参数错误: imei和protocolCode不能为空");
            }

            // 转换为数组
            String[] paramsArray = null;
            if (params != null && !params.isEmpty()) {
                paramsArray = params.toArray(new String[0]);
            }

            // 发送指令
            boolean sent = deviceManager.sendCommand(imei, protocolCode, paramsArray);

            if (sent) {
                return Result.ok("指令发送成功", true);
            } else {
                return Result.error("设备不在线，指令发送失败");
            }

        } catch (Exception e) {
            log.error("发送指令异常", e);
            return Result.error("系统异常: " + e.getMessage());
        }
    }

    /**
     * 发送立即定位指令
     * POST http://localhost:8080/health/api/device/locate/{imei}
     */
    @PostMapping("/locate/{imei}")
    public Result<Boolean> sendLocateCommand(@PathVariable String imei) {
        // 生成流水号（示例：时间戳后6位）
        String serial = String.format("%06d", System.currentTimeMillis() % 1000000);

        Map<String, Object> request = new HashMap<>();
        request.put("imei", imei);
        request.put("protocolCode", "BP16");
        request.put("params", java.util.Arrays.asList(serial));

        return sendCommand(request);
    }

    /**
     * 发送重启指令
     * POST http://localhost:8080/health/api/device/restart/{imei}
     */
    @PostMapping("/restart/{imei}")
    public Result<Boolean> sendRestartCommand(@PathVariable String imei) {
        String serial = String.format("%06d", System.currentTimeMillis() % 1000000);

        Map<String, Object> request = new HashMap<>();
        request.put("imei", imei);
        request.put("protocolCode", "BP18");
        request.put("params", java.util.Arrays.asList(serial));

        return sendCommand(request);
    }

    /**
     * 健康检查接口
     * GET http://localhost:8080/health/api/device/health
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        data.put("onlineDevices", deviceManager.getOnlineCount());
        data.put("service", "智能手表健康监测服务器");
        data.put("version", "1.0.0");
        return Result.ok("服务运行正常", data);
    }

    /**
     * 获取设备缓冲数据统计
     * GET http://localhost:8080/health/api/device/{deviceId}/buffer-count
     */
    @GetMapping("/{deviceId}/buffer-count")
    public Result<Map<String, Object>> getBufferDataCount(@PathVariable Long deviceId) {
        Map<String, Object> stats = deviceDataBufferService.getBufferStats(deviceId);
        return Result.ok("查询成功", stats);
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