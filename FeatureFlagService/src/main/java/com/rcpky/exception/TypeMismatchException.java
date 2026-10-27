package com.rcpky.exception;

import com.rcpky.flag.FlagType;

public class TypeMismatchException extends RuntimeException {
    public TypeMismatchException(String flagName, FlagType expected, Class<?> actual) {
        super("Type mismatch for flag '" + flagName + "': flag is " + expected
                + " but caller requested " + actual.getSimpleName());
    }

    public  TypeMismatchException(String flagName, Object defaultValue, FlagType expected) {
        super("Type mismatch for flag '" + flagName + "': flag is " + expected
                + " but caller requested " + defaultValue.getClass().getSimpleName());
    }
}
