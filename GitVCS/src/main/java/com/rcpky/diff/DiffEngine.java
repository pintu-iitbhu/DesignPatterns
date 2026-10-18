package com.rcpky.diff;

import java.util.Map;

public interface DiffEngine {
    void diff(Map<String, String> before, Map<String, String> after);
}
