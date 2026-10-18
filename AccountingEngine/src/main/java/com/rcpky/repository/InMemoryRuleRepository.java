package com.rcpky.repository;

import com.rcpky.exception.RuleNotFoundException;
import com.rcpky.rule.Rule;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRuleRepository implements RuleRepository {

    private final Map<String, Rule> store = new ConcurrentHashMap<>();

    @Override
    public void save(Rule rule) {
        if (store.containsKey(rule.id())) {
            throw new IllegalArgumentException("Rule already exists: " + rule.id());
        }
        store.put(rule.id(), rule);
    }

    @Override
    public Optional<Rule> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Rule> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public List<Rule> findAllEnabled() {
        return store.values().stream()
                .filter(Rule::enabled)
                .toList();
    }

    @Override
    public void delete(String id) {
        if (!store.containsKey(id)) throw new RuleNotFoundException(id);
        store.remove(id);
    }

    @Override
    public void update(Rule rule) {
        if (!store.containsKey(rule.id())) throw new RuleNotFoundException(rule.id());
        store.put(rule.id(), rule);
    }
}
