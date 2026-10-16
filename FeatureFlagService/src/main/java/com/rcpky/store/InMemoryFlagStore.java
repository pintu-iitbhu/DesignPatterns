package com.rcpky.store;

import com.rcpky.model.FlagDefinition;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryFlagStore implements FlagStore {

    private final ConcurrentMap<String, FlagDefinition> flags = new ConcurrentHashMap<>();

    @Override
    public void put(FlagDefinition flag) {
        flags.put(flag.name(), flag);
    }

    @Override
    public Optional<FlagDefinition> get(String flagName) {
        return Optional.ofNullable(flags.get(flagName));
    }

    @Override
    public void remove(String flagName) {
        flags.remove(flagName);
    }
}
