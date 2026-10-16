package com.rcpky.model;

public record EvaluationResult<T>(
        T value,
        EvaluationReason reason
) { }
