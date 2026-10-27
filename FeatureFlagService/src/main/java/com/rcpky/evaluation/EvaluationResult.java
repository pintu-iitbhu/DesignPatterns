package com.rcpky.evaluation;

public record EvaluationResult<T>(
        T value,
        EvaluationReason reason
) { }
