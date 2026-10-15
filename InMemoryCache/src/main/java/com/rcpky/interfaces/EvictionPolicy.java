package com.rcpky.interfaces;

import com.rcpky.model.Node;

public interface EvictionPolicy<K> {
    K evictKey();
    void keyAccessed(K key);
}
