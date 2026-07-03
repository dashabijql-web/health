package com.xzkj.health.service.watch;

import com.xzkj.health.protocol.WatchMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WatchRawPacketService {

    private static final int MAX_PACKETS = 1000;
    private static final int DEFAULT_LIMIT = 200;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Object lock = new Object();
    private final ArrayDeque<RawPacketView> packets = new ArrayDeque<>();
    private final AtomicLong sequence = new AtomicLong();

    public void capture(WatchMessage message, String imei, String remoteAddress) {
        if (message == null) {
            return;
        }

        RawPacketView packet = new RawPacketView(
                sequence.incrementAndGet(),
                TIME_FORMATTER.format(LocalDateTime.now()),
                safe(message.getProtocolCode()),
                safe(imei),
                safe(remoteAddress),
                message.getParamCount(),
                safe(message.getRawMessage()),
                message.getParams() == null ? List.of() : List.of(message.getParams())
        );

        synchronized (lock) {
            packets.addFirst(packet);
            while (packets.size() > MAX_PACKETS) {
                packets.removeLast();
            }
        }
    }

    public RawPacketPage query(String imei, String protocolCode, Integer limit) {
        int safeLimit = clampLimit(limit);
        String imeiFilter = normalize(imei);
        String protocolFilter = normalize(protocolCode);

        List<RawPacketView> result = new ArrayList<>();
        int total;
        synchronized (lock) {
            total = packets.size();
            for (RawPacketView packet : packets) {
                if (!matches(packet, imeiFilter, protocolFilter)) {
                    continue;
                }
                result.add(packet);
                if (result.size() >= safeLimit) {
                    break;
                }
            }
        }

        return new RawPacketPage(result, total, result.size(), safeLimit, MAX_PACKETS);
    }

    private boolean matches(RawPacketView packet, String imeiFilter, String protocolFilter) {
        boolean imeiMatched = imeiFilter == null || packet.imei().contains(imeiFilter);
        boolean protocolMatched = protocolFilter == null || packet.protocolCode().contains(protocolFilter);
        return imeiMatched && protocolMatched;
    }

    private int clampLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_PACKETS));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record RawPacketPage(
            List<RawPacketView> list,
            int totalBuffered,
            int returnedCount,
            int limit,
            int capacity
    ) {
    }

    public record RawPacketView(
            long sequence,
            String receiveTime,
            String protocolCode,
            String imei,
            String remoteAddress,
            int paramCount,
            String rawMessage,
            List<String> params
    ) {
        public RawPacketView {
            params = params == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(params));
        }
    }
}
