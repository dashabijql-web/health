#!/usr/bin/env python3
"""
智能手表 TCP 客户端模拟器
模拟 50 个手表向服务器发送健康数据

配置说明：
  NUM_WATCHES : 手表数量（对应 EMP001~EMP050）
  DURATION    : 运行时长（秒），0 = 无限运行
  INTERVAL_MIN/MAX : 每次发送间隔（秒），降低以减少数据频率
  ANOMALY_RATE : 各指标异常概率（0.10 = 10%）
"""

import socket
import time
import random
import threading
from datetime import datetime
import sys
import signal

# ─── 全局配置 ─────────────────────────────────────────────────────────────────
SERVER_HOST   = '127.0.0.1'
SERVER_PORT   = 9000
NUM_WATCHES   = 1000     # 手表数量（数据库已有 1000 台，可调至 1~1000）
DURATION      = 0        # 运行时长(秒)，0=无限运行直到 Ctrl+C
INTERVAL_MIN  = 300      # 最短发送间隔(秒)
INTERVAL_MAX  = 600      # 最长发送间隔(秒)
ANOMALY_RATE  = 0.05     # 各健康指标异常概率 5%

# 全局停止标志
stop_event = threading.Event()


class WatchSimulator:
    def __init__(self, watch_id):
        self.watch_id = watch_id
        # IMEI 格式：35945678XXXXXXX（15位），与种子数据一致
        self.imei = f"3594567800{watch_id:05d}"
        self.sock = None
        self.connected = False

    # ──── 连接管理 ────────────────────────────────────────────────────────────

    def connect(self):
        try:
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock.settimeout(10)
            self.sock.connect((SERVER_HOST, SERVER_PORT))
            self.connected = True
            print(f"[{self.watch_id:04d}] ✓ 连接成功  IMEI={self.imei}")
            return True
        except Exception as e:
            print(f"[{self.watch_id:04d}] ✗ 连接失败: {e}")
            return False

    def send_packet(self, packet):
        try:
            if not self.connected:
                return False
            self.sock.sendall(packet.encode('utf-8'))
            self.sock.settimeout(1)
            try:
                resp = self.sock.recv(1024).decode('utf-8')
                if 'ERROR' in resp.upper() or 'FAIL' in resp.upper():
                    print(f"[{self.watch_id:04d}] ⚠ 响应异常: {resp.strip()}")
            except socket.timeout:
                pass
            return True
        except Exception:
            self.connected = False
            return False

    def close(self):
        if self.sock:
            try:
                self.sock.shutdown(socket.SHUT_RDWR)
                self.sock.close()
            except Exception:
                pass
        self.connected = False

    # ──── 协议包 ─────────────────────────────────────────────────────────────

    def login(self):
        """AP00 登录包"""
        return self.send_packet(f"IW*AP00*{self.imei}#")

    def send_heartbeat(self):
        """AP03 心跳包：状态,步数,翻身次数"""
        steps     = random.randint(3000, 12000)
        rollovers = random.randint(5, 30)
        return self.send_packet(f"IW*AP03*1,{steps},{rollovers}#")

    def send_health_data_apht(self):
        """APHT：心率,收缩压,舒张压"""
        if random.random() < ANOMALY_RATE:
            hr       = random.choice([random.randint(45, 59), random.randint(101, 130)])
            systolic = random.randint(141, 165)
            diastolic = random.randint(91, 105)
        else:
            hr        = random.randint(60, 100)
            systolic  = random.randint(110, 135)
            diastolic = random.randint(70, 85)
        return self.send_packet(f"IW*APHT*{hr},{systolic},{diastolic}#")

    def send_health_data_aphp(self):
        """APHP：心率,收缩压,舒张压,血氧,血糖,体温"""
        if random.random() < ANOMALY_RATE:
            hr        = random.choice([random.randint(45, 59), random.randint(101, 130)])
            systolic  = random.randint(141, 165)
            diastolic = random.randint(91, 105)
            oxygen    = random.randint(85, 94)
            temp      = random.choice([
                round(random.uniform(35.5, 35.9), 1),
                round(random.uniform(37.6, 38.5), 1)
            ])
        else:
            hr        = random.randint(60, 100)
            systolic  = random.randint(110, 135)
            diastolic = random.randint(70, 85)
            oxygen    = random.randint(95, 99)
            temp      = round(random.uniform(36.0, 37.5), 1)
        glucose = round(random.uniform(4.0, 6.5), 1)
        return self.send_packet(
            f"IW*APHP*{hr},{systolic},{diastolic},{oxygen},{glucose},{temp},,,,,,,#"
        )

    def send_temperature(self):
        """AP50：体温,电量"""
        if random.random() < ANOMALY_RATE:
            temp = random.choice([
                round(random.uniform(35.5, 35.9), 1),
                round(random.uniform(37.6, 38.5), 1)
            ])
        else:
            temp = round(random.uniform(36.0, 37.5), 1)
        battery = random.randint(20, 100)
        return self.send_packet(f"IW*AP50*{temp},{battery}#")

    def send_sleep_data(self):
        """AP97：深睡时长(分钟),浅睡时长(分钟)
        10% 概率睡眠不足（总计 < 360 分钟）
        """
        if random.random() < ANOMALY_RATE:
            # 睡眠不足：总时长 200~359 分钟（< 6 小时）
            total     = random.randint(200, 359)
            deep_pct  = random.uniform(0.3, 0.5)
            deep_sleep  = int(total * deep_pct)
            light_sleep = total - deep_sleep
        else:
            # 正常睡眠：总时长 360~540 分钟（6~9 小时）
            deep_sleep  = random.randint(120, 200)
            light_sleep = random.randint(240, 340)
        return self.send_packet(f"IW*AP97*{deep_sleep},{light_sleep}#")

    def send_gps_data(self):
        """AP01：GPS 定位数据（模拟矿区坐标）"""
        lat_deg = 37
        lat_min = round(random.uniform(30.0, 40.0), 4)
        lon_deg = 112
        lon_min = round(random.uniform(0.0, 15.0), 4)

        date_str  = datetime.now().strftime('%y%m%d')
        time_str  = datetime.now().strftime('%H%M%S')
        latitude  = f"{lat_deg:02d}{lat_min:07.4f}N"
        longitude = f"{lon_deg:03d}{lon_min:07.4f}E"
        speed     = f"{random.randint(0, 10):03d}.{random.randint(0, 9)}"
        direction = f"{random.randint(0, 359):03d}.{random.randint(0, 99):02d}"
        gsm       = f"{random.randint(10, 31):02d}"
        sats      = f"{random.randint(4, 12):03d}"
        batt      = f"{random.randint(20, 100):03d}"
        status_info = f"{gsm}{sats}{batt}001002"

        mcc = "460"; mnc = "0"
        lac = random.randint(9000, 9999)
        cid = random.randint(3000, 4000)
        lbs = f"{mcc},{mnc},{lac},{cid}"

        def rand_mac():
            return ':'.join(f'{random.randint(0,255):02X}' for _ in range(6))

        wifi = f"Mine{random.randint(1,9)}|{rand_mac()}|{random.randint(50,99)}"

        packet = (f"IW*AP01*{date_str}A{latitude}{longitude}{speed}"
                  f"{time_str}{direction}{status_info},{lbs},{wifi}#")
        return self.send_packet(packet)

    # ──── 主循环 ─────────────────────────────────────────────────────────────

    def run_simulation(self):
        if not self.connect():
            return
        if not self.login():
            print(f"[{self.watch_id:04d}] ✗ 登录失败")
            self.close()
            return

        # 等待服务器完成 IMEI 注册
        time.sleep(3)

        start_time   = time.time()
        packet_count = 0

        while not stop_event.is_set():
            if DURATION > 0 and time.time() - start_time >= DURATION:
                break
            try:
                data_type = random.choice([
                    'apht', 'aphp', 'temp', 'gps', 'heartbeat'
                ])
                dispatch = {
                    'apht':      self.send_health_data_apht,
                    'aphp':      self.send_health_data_aphp,
                    'temp':      self.send_temperature,
                    'gps':       self.send_gps_data,
                    'heartbeat': self.send_heartbeat,
                }
                success = dispatch[data_type]()

                if success:
                    packet_count += 1
                else:
                    if stop_event.is_set():
                        break
                    # 尝试重连
                    self.close()
                    time.sleep(5)
                    if not stop_event.is_set() and self.connect() and self.login():
                        time.sleep(3)
                    else:
                        break

                # 间隔 INTERVAL_MIN ~ INTERVAL_MAX 秒，每 0.5 秒检查停止信号
                wait = random.uniform(INTERVAL_MIN, INTERVAL_MAX)
                waited = 0.0
                while waited < wait and not stop_event.is_set():
                    time.sleep(0.5)
                    waited += 0.5

            except Exception as e:
                if not stop_event.is_set():
                    print(f"[{self.watch_id:04d}] ✗ 异常: {e}")
                break

        if packet_count > 0:
            print(f"[{self.watch_id:04d}] ⊕ 结束，共发送 {packet_count} 包")
        self.close()


# ─── 入口 ─────────────────────────────────────────────────────────────────────

def run_single_watch(watch_id):
    WatchSimulator(watch_id).run_simulation()


def signal_handler(sig, frame):
    print("\n\n🛑 收到停止信号，正在优雅关闭所有连接...")
    stop_event.set()


def main():
    signal.signal(signal.SIGINT, signal_handler)

    dur_desc = f"{DURATION} 秒" if DURATION > 0 else "无限（Ctrl+C 停止）"
    print("=" * 70)
    print("智能手表 TCP 模拟器".center(70))
    print("=" * 70)
    print(f"  服务器     : {SERVER_HOST}:{SERVER_PORT}")
    print(f"  手表数量   : {NUM_WATCHES}")
    print(f"  发送间隔   : {INTERVAL_MIN}~{INTERVAL_MAX} 秒")
    print(f"  异常概率   : {int(ANOMALY_RATE*100)}%")
    print(f"  运行时长   : {dur_desc}")
    print("=" * 70)
    print()

    threads = []
    try:
        for i in range(1, NUM_WATCHES + 1):
            if stop_event.is_set():
                break
            t = threading.Thread(
                target=run_single_watch,
                args=(i,),
                daemon=False
            )
            t.start()
            threads.append(t)

            # 每 10 个手表延迟 1 秒，避免同时连接涌入
            if i % 10 == 0:
                print(f"  已启动 {i}/{NUM_WATCHES} 个手表...")
                time.sleep(1)

        if not stop_event.is_set():
            print(f"\n✓ 全部 {NUM_WATCHES} 个手表已启动，等待数据上报...\n")

        while threads:
            threads = [t for t in threads if t.is_alive()]
            if stop_event.is_set() or not threads:
                break
            time.sleep(1)

        if stop_event.is_set():
            print("等待所有连接关闭（最多 5 秒）...")
            for t in threads:
                t.join(timeout=5)

        print("\n✓ 所有手表已停止!")

    except Exception as e:
        print(f"\n✗ 发生异常: {e}")
        stop_event.set()


if __name__ == '__main__':
    main()
