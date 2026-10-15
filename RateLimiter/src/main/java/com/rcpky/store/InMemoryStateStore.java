package com.rcpky.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.function.BiFunction;

public class InMemoryStateStore<V> implements StateStore<V> {

    private final Cache<String, V> cache;

    public InMemoryStateStore(Duration ttl) {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(ttl)
                .build();
    }

    @Override
    public V compute(String key, BiFunction<String, V, V> remappingFunction) {
        return cache.asMap().compute(key, remappingFunction);
    }
}
