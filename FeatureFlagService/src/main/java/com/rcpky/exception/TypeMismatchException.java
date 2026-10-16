package com.rcpky.exception;

import com.rcpky.model.FlagType;

public class TypeMismatchException extends RuntimeException {
    public TypeMismatchException(String flagName, FlagType expected, Class<?> actual) {
        super("Type mismatch for flag '" + flagName + "': flag is " + expected
                + " but caller requested " + actual.getSimpleName());
    }
}
