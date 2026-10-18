package com.rcpky.filter;

import com.rcpky.model.Message;

/**
 * Functional interface for filtering messages during consumption.
 * Implement this to customise what a subscriber receives from a topic.
 * Default implementations: {@link KeyValueFilter} (metadata key-value matching).
 * Extension points: regex on payload, composite AND/OR filters, etc.
 */
@FunctionalInterface
public interface MessageFilter {
    boolean matches(Message message);
}
