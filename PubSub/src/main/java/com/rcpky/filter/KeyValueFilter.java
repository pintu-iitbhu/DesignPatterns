package com.rcpky.filter;

import com.rcpky.model.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Matches a message when ALL supplied key-value pairs are present
 * in the message metadata (AND semantics).
 *
 * Extension: swap the allMatch() for anyMatch() to get OR semantics.
 */
public class KeyValueFilter implements MessageFilter {

    private final Map<String, String> criteria;

    public KeyValueFilter(Map<String, String> criteria) {
        this.criteria = new HashMap<>(criteria);
    }

    @Override
    public boolean matches(Message message) {
        return criteria.entrySet().stream()
                .allMatch(e -> e.getValue().equals(message.getMetadata().get(e.getKey())));
    }

    @Override
    public String toString() {
        return "KeyValueFilter" + criteria;
    }
}
