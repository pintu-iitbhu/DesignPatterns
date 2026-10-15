package com.rcpky.store;

import java.util.function.BiFunction;

public interface StateStore<V> {
    V compute(String key, BiFunction<String, V, V> remappingFunction);
}
