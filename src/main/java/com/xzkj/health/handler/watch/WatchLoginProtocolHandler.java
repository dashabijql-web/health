package com.xzkj.health.handler.watch;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
public class WatchLoginProtocolHandler implements WatchProtocolHandler {

    private static final DateTimeFormatter LOGIN_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    @Override
    public void handle(WatchMessageHandlerContext context) {
        String imei = context.param(0);
        if (imei == null || !imei.matches("\\d{15}")) {
            log.warn("无效的IMEI: {}", imei);
            context.sendErrorResponse("IMEI无效");
            return;
        }

        log.info("设备登录: IMEI={}, 地址={}", imei, context.channel().remoteAddress());

        String response = "IW*BP00*," + LOGIN_TIME_FORMAT.format(Instant.now()) + ",8#";
        log.info("发送登录响应: {}", response);
        context.writeAscii(response);

        context.dataService().saveDeviceLogin(imei, String.valueOf(context.channel().remoteAddress()));
    }
}
