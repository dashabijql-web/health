package com.xzkj.health.common;

import java.util.Map;

/**
 * Map 取值工具类 — 安全地从 Map&lt;String, Object&gt; 中提取数值，
 * 兼容 Integer / Long / Double / BigDecimal / String 等类型。
 */
public final class MapValueUtil {

    private MapValueUtil() {}

    public static int getInt(Map<String, Object> map, String key) {
        if (map == null) return 0;
        Object v = map.get(key);
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return 0; }
    }

    public static long getLong(Map<String, Object> map, String key) {
        if (map == null) return 0L;
        Object v = map.get(key);
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return 0L; }
    }

    public static double getDouble(Map<String, Object> map, String key) {
        if (map == null) return 0.0;
        Object v = map.get(key);
        if (v == null) return 0.0;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (NumberFormatException e) { return 0.0; }
    }

    public static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }
}
