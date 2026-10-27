package com.rcpky.flag;

import com.rcpky.rollout.Rollout;
import com.rcpky.rule.Rule;

import java.util.Objects;

/**
 * Type-safe container for a flag's value.
 *
 * <p>Replaces raw {@code Object} across {@link FlagDefinition#defaultValue()},
 * {@link Rule#targetValue()}, and {@link Rollout#value()}, eliminating all
 * unchecked casts from the evaluation pipeline.
 *
 * <p>Use the static factories:
 * <pre>{@code
 *   FlagValue.of(true)    // BoolValue
 *   FlagValue.of("v2")    // StringValue
 *   FlagValue.of(100)     // IntValue
 * }</pre>
 *
 * <p>{@link #as(Class)} extracts the wrapped value as the requested Java type.
 * It delegates to {@link Class#cast} — no unchecked cast, and a clear
 * {@link ClassCastException} message on type mismatch.
 */
public sealed interface FlagValue permits FlagValue.BoolValue, FlagValue.StringValue, FlagValue.IntValue {

    record BoolValue(boolean value)  implements FlagValue {}
    record StringValue(String value) implements FlagValue {}
    record IntValue(int value)       implements FlagValue {}

    static FlagValue of(boolean b) { return new BoolValue(b); }
    static FlagValue of(int i)     { return new IntValue(i); }
    static FlagValue of(String s) {
        return new StringValue(Objects.requireNonNull(s, "String flag value must not be null"));
    }

    /**
     * Extracts the wrapped value as {@code type}, using {@link Class#cast} so
     * any mismatch surfaces immediately with a meaningful message.
     */
    default <T> T as(Class<T> type) {
        Object raw = switch (this) {
            case BoolValue   bv -> bv.value();   // boolean → autoboxed Boolean
            case StringValue sv -> sv.value();
            case IntValue    iv -> iv.value();   // int → autoboxed Integer
        };
        try {
            return type.cast(raw);
        } catch (ClassCastException e) {
            throw new ClassCastException(String.format(
                "Cannot extract %s value as %s",
                this.getClass().getSimpleName(), type.getSimpleName()));
        }
    }
}
