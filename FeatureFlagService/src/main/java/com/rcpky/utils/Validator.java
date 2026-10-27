package com.rcpky.utils;

import com.rcpky.exception.TypeMismatchException;
import com.rcpky.flag.FlagDefinition;
import com.rcpky.flag.FlagValue;

public class Validator {

    public void validateType(FlagDefinition flag, Class<?> requested) {
        boolean valid = switch (flag.type()) {
            case BOOLEAN -> requested == Boolean.class;
            case STRING  -> requested == String.class;
            case INTEGER -> requested == Integer.class;
        };
        if (!valid) {
            throw new TypeMismatchException(flag.name(), flag.type(), requested);
        }
    }

    public void validateDefaultType(FlagDefinition flag, FlagValue defaultValue) {
        boolean ok = switch (flag.type()) {
            case BOOLEAN -> defaultValue instanceof FlagValue.BoolValue;
            case STRING  -> defaultValue instanceof FlagValue.StringValue;
            case INTEGER -> defaultValue instanceof FlagValue.IntValue;
        };
        if (!ok) {
            throw new TypeMismatchException(flag.name(), defaultValue, flag.type());
        }
    }
}
