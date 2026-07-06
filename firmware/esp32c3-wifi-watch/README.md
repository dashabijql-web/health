# ESP32-C3 Wi-Fi 健康手表原型

这是第一版 Wi-Fi 手表固件，用 ESP32-C3 连接本项目后端 TCP `9000`，按现有手表协议上传健康数据。

交给 Windows 上的 Codex 时，先读 `WINDOWS_CODEX_HANDOFF.md`。

## 硬件

- ESP32-C3 开发板
- MAX30102 心率/血氧模块
- MPU6050 加速度/陀螺仪模块
- SSD1306 0.96 寸 I2C OLED
- 3.7V 锂电池 + TP4056 充电保护模块

默认 I2C：

| 信号 | ESP32-C3 默认 GPIO |
| --- | --- |
| SDA | GPIO8 |
| SCL | GPIO9 |
| 3V3 | 3.3V |
| GND | GND |

如果你的 ESP32-C3 开发板默认 I2C 不是 GPIO8/GPIO9，改 `platformio.ini` 里的 `WATCH_I2C_SDA` 和 `WATCH_I2C_SCL`。

## 协议

固件启动后会连接 Wi-Fi，再连接后端 TCP：

```text
WATCH_SERVER_HOST:WATCH_SERVER_PORT
```

连接成功后发送登录包：

```text
IW*AP00*<WATCH_IMEI>#
```

定时上传步数心跳：

```text
IW*AP03*1,<steps>,<rollovers>,<calories>#
```

定时上传综合健康包：

```text
IW*APHP*<heartRate>,0,0,<bloodOxygen>,0,0#
```

第一版只真实采集心率、血氧、步数。血压、血糖、体温先填 `0`，后端会忽略这些无效字段。

## Mac 开发环境

推荐先在 Mac 上做这一版，因为当前后端和仓库就在 Mac 工作区。

安装 PlatformIO：

```bash
python3 -m pip install -U platformio
```

编译：

```bash
cd /Users/jiangqianli/Documents/Codex/health-mac/health/firmware/esp32c3-wifi-watch
pio run
```

烧录：

```bash
pio run -t upload
```

串口监视：

```bash
pio device monitor -b 115200
```

## 必改配置

编辑 `platformio.ini`：

```ini
-DWATCH_WIFI_SSID=\"你的WiFi\"
-DWATCH_WIFI_PASSWORD=\"你的WiFi密码\"
-DWATCH_SERVER_HOST=\"你的Mac局域网IP\"
-DWATCH_SERVER_PORT=9000
-DWATCH_IMEI=\"123456789012345\"
```

Mac 局域网 IP 可用：

```bash
ipconfig getifaddr en0
```

后端 TCP 端口要能从同一 Wi-Fi 下访问，默认是 `9000`。

## 注意

- MAX30102 的血氧算法是原型估算，不是医疗级算法。
- 200mAh 电池只适合短时演示。
- 后续改 4G 时，保留 `sendFrame()` 以上的协议层，把 Wi-Fi/TCP 连接替换成 4G 模块 AT TCP 透传即可。
