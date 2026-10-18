package com.rcpky.repository;

import com.rcpky.rule.Rule;

import java.util.List;
import java.util.Optional;

public interface RuleRepository {
    void save(Rule rule);
    Optional<Rule> findById(String id);
    List<Rule> findAll();
    List<Rule> findAllEnabled();
    void delete(String id);
    void update(Rule rule);
}
