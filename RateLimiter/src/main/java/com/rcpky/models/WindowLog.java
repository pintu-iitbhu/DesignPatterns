package com.rcpky.models;

import java.util.List;

public record WindowLog(List<Long> timestamps) {
    public WindowLog {
        timestamps = List.copyOf(timestamps);
    }

    public static WindowLog empty() {
        return new WindowLog(List.of());
    }
}
