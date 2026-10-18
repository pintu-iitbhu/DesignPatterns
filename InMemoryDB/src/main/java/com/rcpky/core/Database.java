package com.rcpky.core;

import com.rcpky.exception.DatabaseException;

import java.util.*;

/**
 * Top-level container that owns a set of named {@link Table}s.
 */
public class Database {

    private final String name;
    private final Map<String, Table> tables;

    public Database(String name) {
        this.name = name;
        this.tables = new LinkedHashMap<>();
    }

    public void createTable(String tableName, List<ColumnDefinition> columns) {
        if (tables.containsKey(tableName)) {
            throw new DatabaseException("Table '" + tableName + "' already exists");
        }
        if (columns == null || columns.isEmpty()) {
            throw new DatabaseException("Table must have at least one column");
        }
        // Guard against duplicate column names
        Set<String> seen = new HashSet<>();
        for (ColumnDefinition col : columns) {
            if (!seen.add(col.getName())) {
                throw new DatabaseException("Duplicate column name: '" + col.getName() + "'");
            }
        }
        tables.put(tableName, new Table(tableName, columns));
        System.out.println("Table '" + tableName + "' created.");
    }

    public void dropTable(String tableName) {
        if (!tables.containsKey(tableName)) {
            throw new DatabaseException("Table '" + tableName + "' does not exist");
        }
        tables.remove(tableName);
        System.out.println("Table '" + tableName + "' dropped.");
    }

    public Table getTable(String tableName) {
        Table table = tables.get(tableName);
        if (table == null) {
            throw new DatabaseException("Table '" + tableName + "' does not exist");
        }
        return table;
    }

    public List<String> listTables() {
        return new ArrayList<>(tables.keySet());
    }

    public String getName() { return name; }
}
