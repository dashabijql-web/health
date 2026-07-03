package com.xzkj.health.controller;

import com.xzkj.health.common.Result;
import com.xzkj.health.service.watch.WatchRawPacketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watch")
public class WatchRawPacketController {

    private final WatchRawPacketService rawPacketService;

    public WatchRawPacketController(WatchRawPacketService rawPacketService) {
        this.rawPacketService = rawPacketService;
    }

    @GetMapping("/raw-packets")
    public Result<WatchRawPacketService.RawPacketPage> listRawPackets(
            @RequestParam(required = false) String imei,
            @RequestParam(required = false) String protocolCode,
            @RequestParam(defaultValue = "200") Integer limit) {
        return Result.ok("获取成功", rawPacketService.query(imei, protocolCode, limit));
    }
}
