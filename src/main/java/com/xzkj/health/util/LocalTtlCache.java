package com.xzkj.health.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级进程内 TTL 缓存。
 */
public class LocalTtlCache<T> {

    private final ConcurrentHashMap<String, Entry<T>> store = new ConcurrentHashMap<>();

    public T getIfFresh(String key) {
        Entry<T> entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() >= entry.expireAt()) {
            store.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    public void put(String key, T value, long ttlMillis) {
        store.put(key, new Entry<>(value, System.currentTimeMillis() + ttlMillis));
    }

    private record Entry<T>(T value, long expireAt) {}
}
