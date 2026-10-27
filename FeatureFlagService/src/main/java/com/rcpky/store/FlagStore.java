package com.rcpky.store;

import com.rcpky.flag.FlagDefinition;

import java.util.Optional;

public interface FlagStore {
    void put(FlagDefinition flag);
    Optional<FlagDefinition> get(String flagName);
    void remove(String flagName);
}
