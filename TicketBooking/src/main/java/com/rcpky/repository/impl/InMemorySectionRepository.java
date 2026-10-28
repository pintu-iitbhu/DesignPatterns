package com.rcpky.repository.impl;

import com.rcpky.model.Section;
import com.rcpky.repository.ISectionRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySectionRepository implements ISectionRepository {
    private final ConcurrentHashMap<String, Section> store = new ConcurrentHashMap<>();

    @Override public void save(Section section)                { store.put(section.getId(), section); }
    @Override public Optional<Section> findById(String id)     { return Optional.ofNullable(store.get(id)); }
}
