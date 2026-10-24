package com.rcpky.utils;

import com.rcpky.exception.TypeMismatchException;
import com.rcpky.model.FlagDefinition;
import com.rcpky.model.FlagType;

public class Validator {

    public void validateType(FlagDefinition flag, Class<?> requested) {
        boolean valid = switch (flag.type()) {
            case BOOLEAN -> requested == Boolean.class;
            case STRING -> requested == String.class;
            case INTEGER -> requested == Integer.class;
        };
        if (!valid) {
            throw new TypeMismatchException(flag.name(), flag.type(), requested);
        }
    }

    public void validateDefaultType(FlagDefinition flag, Object defaultValue) {

        boolean ok = switch (flag.type()) {
            case BOOLEAN -> defaultValue instanceof Boolean;
            case STRING -> defaultValue instanceof String;
            case INTEGER -> defaultValue instanceof Integer;
        };

        if (!ok) {
            throw new TypeMismatchException(flag.name(), defaultValue,flag.type());
        }

    }
}
