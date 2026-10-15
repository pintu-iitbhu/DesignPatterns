package com.rcpky.models;

public record WindowState(
        long windowId,
        long previousCount,
        long currentCount
) {}
