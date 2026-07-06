# Windows Codex Handoff: ESP32-C3 Wi-Fi Health Watch

## Mission

Continue the first Wi-Fi prototype for the simplified health watch.

The current firmware target is:

- Board: ESP32-C3 dev board
- Network: Wi-Fi TCP client
- Backend: existing Health backend TCP port `9000`
- Upload protocol: `IW*AP00*...#`, `IW*AP03*...#`, `IW*APHP*...#`
- Sensors: MAX30102, MPU6050, SSD1306 OLED on I2C

This is not the 4G version yet. Keep the protocol layer stable so the Wi-Fi transport can later be replaced by a 4G module AT-command TCP transport.

## Included Project

Open this folder on Windows:

```text
firmware/esp32c3-wifi-watch
```

Important files:

```text
platformio.ini          PlatformIO config, board, libraries, Wi-Fi/server build flags
src/main.cpp            ESP32-C3 firmware
README.md               Mac/PlatformIO build and wiring notes
tools/protocol_probe.py Host-side TCP probe for AP00/AP03/APHP without hardware
```

## Windows Setup

Install Python 3, then:

```powershell
py -m pip install -U platformio
```

From the firmware folder:

```powershell
pio run
```

The project was already verified on Mac with PlatformIO and compiled successfully before packaging.

## Configure Before Flashing

Edit `platformio.ini`:

```ini
-DWATCH_WIFI_SSID=\"YOUR_WIFI\"
-DWATCH_WIFI_PASSWORD=\"YOUR_WIFI_PASSWORD\"
-DWATCH_SERVER_HOST=\"BACKEND_IP\"
-DWATCH_SERVER_PORT=9000
-DWATCH_IMEI=\"123456789012345\"
-DWATCH_I2C_SDA=8
-DWATCH_I2C_SCL=9
```

`WATCH_SERVER_HOST` must be reachable by the ESP32-C3 on the same Wi-Fi network.

On Windows, the host IP can usually be found with:

```powershell
ipconfig
```

Use the IPv4 address of the active Wi-Fi adapter.

## Flash

Connect ESP32-C3 with a data-capable USB cable.

List serial ports:

```powershell
pio device list
```

Flash automatically:

```powershell
pio run -t upload
```

Or specify the port:

```powershell
pio run -t upload --upload-port COM5
```

Serial monitor:

```powershell
pio device monitor -b 115200 --port COM5
```

If upload hangs at `Connecting...`, enter bootloader mode:

```text
Hold BOOT -> tap RESET -> release BOOT -> run upload again
```

## Backend Protocol Expectations

The firmware sends:

```text
IW*AP00*<IMEI>#
IW*AP03*1,<steps>,<rollovers>,<calories>#
IW*APHP*<heartRate>,0,0,<bloodOxygen>,0,0#
```

The Health backend currently recognizes these protocol codes through the watch handler registry and health handler. In this handoff package, see:

```text
backend-protocol/WatchDataHandler.java
backend-protocol/watch/WatchHealthProtocolHandler.java
backend-protocol/protocol/WatchProtocolDecoder.java
backend-protocol/protocol/WatchMessage.java
```

## Probe Without Hardware

If the backend TCP server is running, verify the protocol path from Windows:

```powershell
py tools/protocol_probe.py --host 127.0.0.1 --port 9000 --imei 123456789012345
```

Expected: the script prints each transmitted frame and any backend response. A missing response is useful evidence too, but connection failure means TCP `9000` is not reachable.

## First Acceptance Gate

Do not call the Windows handoff working until these pass on the Windows machine:

1. `pio run` completes.
2. `pio device list` sees the ESP32-C3 serial port.
3. `pio run -t upload` flashes the board.
4. `pio device monitor -b 115200` shows Wi-Fi/TCP/login attempts.
5. Backend logs show `AP00` login and at least one `APHP` health upload.

## Known Limits

- MAX30102 SpO2 in this firmware is a prototype estimate, not medical-grade.
- Blood pressure, blood sugar, and body temperature are placeholders set to `0`.
- 200mAh battery is enough for short bench demos, not a final wearable.
- A real 4G version needs a cellular module, SIM/APN, antenna, and stronger power design.
