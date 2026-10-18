package com.rcpky.builder;

import com.rcpky.rule.Rule;
import com.rcpky.rule.RuleAction;
import com.rcpky.rule.RuleCondition;

import java.util.Objects;

public class RuleBuilder {

    private String id;
    private String name;
    private int priority = Integer.MAX_VALUE;
    private RuleCondition condition;
    private RuleAction action;
    private boolean enabled = true;

    private RuleBuilder() {}

    public static RuleBuilder builder() {
        return new RuleBuilder();
    }

    public RuleBuilder id(String id) {
        this.id = id;
        return this;
    }

    public RuleBuilder name(String name) {
        this.name = name;
        return this;
    }

    public RuleBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    public RuleBuilder when(RuleCondition condition) {
        this.condition = condition;
        return this;
    }

    public RuleBuilder then(RuleAction action) {
        this.action = action;
        return this;
    }

    public RuleBuilder disabled() {
        this.enabled = false;
        return this;
    }

    public Rule build() {
        Objects.requireNonNull(id, "Rule id is required");
        Objects.requireNonNull(name, "Rule name is required");
        Objects.requireNonNull(condition, "Rule condition is required");
        Objects.requireNonNull(action, "Rule action is required");
        return new Rule(id, name, priority, condition, action, enabled);
    }
}
