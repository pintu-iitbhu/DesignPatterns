package com.rcpky.service;

import com.rcpky.interfaces.Cache;
import com.rcpky.interfaces.EvictionPolicy;
import com.rcpky.model.CacheEntry;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class InMemoryCache<K, V> implements Cache<K, V> {
    private final Map<K, CacheEntry<V>> cacheEntryMap = new HashMap<>();
    private final EvictionPolicy<K> evictionPolicy;
    private final int capacity;
    ReentrantLock lock = new ReentrantLock();

    public InMemoryCache(EvictionPolicy<K> evictionPolicy, int capacity) {
        this.evictionPolicy = evictionPolicy;
        this.capacity = capacity;
    }


    @Override
    public V get(K key) {
        lock.lock();

        try {
            CacheEntry<V> cacheEntry = cacheEntryMap.get(key);
            if (cacheEntry==null || cacheEntry.isExpired()) {
                cacheEntryMap.remove(key);
                return null;
            }
            evictionPolicy.keyAccessed(key);
            return cacheEntry.getValue();
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void put(K key, V value) {
        lock.lock();
        try {
            if (cacheEntryMap.size() >= capacity) {
                K evict = evictionPolicy.evictKey();
                if (evict != null) cacheEntryMap.remove(evict);
            }
            cacheEntryMap.put(key, new CacheEntry<>(value, LocalDateTime.now().plusMinutes(1)));
            evictionPolicy.keyAccessed(key);
        }finally {
            lock.unlock();
        }

    }

    @Override
    public void remove(K key) {
        lock.lock();
        try {
            cacheEntryMap.remove(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return cacheEntryMap.size();
        } finally {
            lock.unlock();
        }
    }
}
