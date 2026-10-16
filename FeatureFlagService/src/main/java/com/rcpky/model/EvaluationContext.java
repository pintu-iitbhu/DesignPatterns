package com.rcpky.model;

import java.util.Map;

public record EvaluationContext(
        String userId,
        Map<String, Object> attributes
) {
    public EvaluationContext {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static EvaluationContext of(String userId, Map<String, Object> attributes) {
        return new EvaluationContext(userId, attributes);
    }

    public static EvaluationContext anonymous() {
        return new EvaluationContext(null, Map.of());
    }
}
