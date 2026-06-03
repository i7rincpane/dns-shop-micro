package ru.nvkz.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class IdempotencyCache {

    private final Map<UUID, Object> map;
    private static final Object DUMMY = new Object();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /*
    При capacity: 1000, приходит 10 сообщений в секунду -
    кеш будет помнить только последние 1.5 минуты накопленных сообщений (1000 / 10 = 100 сек).
    Если дубль придет через 2 минуты, кеш его уже забудет.
    При capacity: 6000 кеш будет помнить последние 10 минут - 10 (сообщений) * 60 (секунд) * 10 (минут) = 6000 элементов
     */
    public IdempotencyCache(@Value("${app.cache.idempotency-size:6000}") int capacity) {
        this.map = new LinkedHashMap<>(capacity) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, Object> eldest) {
                return size() > capacity;
            }
        };
    }

    public boolean contains(UUID id) {
        lock.readLock().lock();
        try {
            return map.containsKey(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void put(UUID id) {
        lock.writeLock().lock();
        try {
            map.put(id, DUMMY);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return map.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
