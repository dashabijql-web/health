package com.xzkj.health.handler.watch;

import lombok.extern.slf4j.Slf4j;
import io.netty.util.AttributeKey;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class WatchLoginProtocolHandler implements WatchProtocolHandler {

    private static final DateTimeFormatter LOGIN_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
    private static final AttributeKey<ScheduledFuture<?>> MONITORING_LOOP_KEY =
            AttributeKey.valueOf("watch.monitoring.loop");
    private static final AtomicInteger SERIAL = new AtomicInteger((int) (System.currentTimeMillis() % 1_000_000L));
    private static final long MONITORING_REFRESH_SECONDS = 60L;
    private static final long COMMAND_INTERVAL_MS = 800L;
    private static final int MEASURE_PERIOD_MINUTES = 1;

    @Override
    public void handle(WatchMessageHandlerContext context) {
        String imei = context.param(0);
        if (imei == null || !imei.matches("\\d{15}")) {
            log.warn("无效的IMEI: {}", imei);
            context.sendErrorResponse("IMEI无效");
            return;
        }

        log.info("设备登录: IMEI={}, 地址={}", imei, context.channel().remoteAddress());

        String response = "IWBP00," + LOGIN_TIME_FORMAT.format(Instant.now()) + ",8#";
        log.info("发送登录响应: {}", response);
        context.writeAscii(response);

        context.dataService().saveDeviceLogin(imei, String.valueOf(context.channel().remoteAddress()));
        String[] initialConfigurationCommands = buildInitialConfigurationCommands(imei);
        sendCommandBatch(context, imei, initialConfigurationCommands);
        ensureMonitoringLoop(context, imei, initialConfigurationCommands.length);
    }

    private void ensureMonitoringLoop(WatchMessageHandlerContext context, String imei, int initialCommandCount) {
        ScheduledFuture<?> existing = context.channel().attr(MONITORING_LOOP_KEY).get();
        if (existing != null && !existing.isCancelled() && !existing.isDone()) {
            log.debug("手表监测循环已存在，跳过重复创建: IMEI={}", imei);
            return;
        }

        ScheduledFuture<?> future = context.channel().eventLoop().scheduleAtFixedRate(
                () -> sendCommandBatch(context, imei, buildMonitoringCommands(imei)),
                initialCommandCount * COMMAND_INTERVAL_MS,
                TimeUnit.SECONDS.toMillis(MONITORING_REFRESH_SECONDS),
                TimeUnit.MILLISECONDS);
        context.channel().attr(MONITORING_LOOP_KEY).set(future);
        context.channel().closeFuture().addListener(f -> {
            ScheduledFuture<?> current = context.channel().attr(MONITORING_LOOP_KEY).getAndSet(null);
            if (current != null) {
                current.cancel(false);
            }
        });
    }

    private void sendCommandBatch(WatchMessageHandlerContext context, String imei, String[] commands) {
        for (int i = 0; i < commands.length; i++) {
            String command = commands[i];
            long delayMs = (long) i * COMMAND_INTERVAL_MS;
            context.channel().eventLoop().schedule(() -> {
                if (!context.channel().isActive()) {
                    log.warn("跳过手表命令，连接已断开: IMEI={}, command={}", imei, command);
                    return;
                }
                log.info("发送手表命令: IMEI={}, command={}", imei, command);
                context.writeAscii(command);
            }, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    static String[] buildInitialConfigurationCommands(String imei) {
        return new String[]{
                "IWBP33," + imei + "," + nextSerial() + ",1#",
                "IWBP86," + imei + "," + nextSerial() + ",1," + MEASURE_PERIOD_MINUTES + "#",
                "IWBP87," + imei + "," + nextSerial() + ",1," + MEASURE_PERIOD_MINUTES + "#"
        };
    }

    static String[] buildMonitoringCommands(String imei) {
        return new String[]{
                "IWBPXL," + imei + "," + nextSerial() + "#",
                "IWBPXY," + imei + "," + nextSerial() + "#",
                "IWBPXZ," + imei + "," + nextSerial() + "#",
                "IWBPXT," + imei + "," + nextSerial() + "#",
                "IWBP16," + imei + "," + nextSerial() + "#"
        };
    }

    private static String nextSerial() {
        return String.format("%06d", SERIAL.updateAndGet(current -> current >= 999_999 ? 0 : current + 1));
    }
}
