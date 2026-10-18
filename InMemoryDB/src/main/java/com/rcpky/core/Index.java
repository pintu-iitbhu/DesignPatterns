package com.rcpky.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hash-based index on a single column.
 * Lookup: O(1) average. Build from existing records: O(n).
 */
public class Index {

    private final String columnName;
    private final Map<Object, List<Row>> indexMap;

    public Index(String columnName) {
        this.columnName = columnName;
        this.indexMap = new HashMap<>();
    }

    public void add(Object value, Row record) {
        indexMap.computeIfAbsent(value, k -> new ArrayList<>()).add(record);
    }

    public List<Row> lookup(Object value) {
        return indexMap.getOrDefault(value, new ArrayList<>());
    }

    public String getColumnName() { return columnName; }
}
