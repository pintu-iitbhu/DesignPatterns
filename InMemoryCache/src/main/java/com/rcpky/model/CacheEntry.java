package com.rcpky.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class CacheEntry<V> {
    V value;
    LocalDateTime expires;

    public CacheEntry(V value, LocalDateTime expires) {
        this.value = value;
        this.expires = expires;
    }

    public boolean isExpired() {
        return expires.isBefore(LocalDateTime.now());
    }
}
