package com.rcpky.core;

import com.rcpky.exception.DatabaseException;

import java.util.*;

/**
 * Represents a single in-memory table.
 *
 * Type constraints:
 *   STRING  — max length 20
 *   INT     — range [-1024, 1024]
 *
 * Column constraints: NOT_NULL, UNIQUE, PRIMARY_KEY (→ NOT_NULL + UNIQUE + auto-indexed).
 *
 * Indexes: hash-based, built lazily via createIndex(). Filter queries prefer index when available,
 * otherwise fall back to a linear scan.
 */
public class Table {

    private static final int MAX_STRING_LENGTH = 20;
    private static final int MIN_INT_VALUE      = -1024;
    private static final int MAX_INT_VALUE      =  1024;

    private final String name;
    private final List<ColumnDefinition> columns;
    private final Map<String, ColumnDefinition> columnMap;   // column name → definition
    private final List<Row> records;
    private final Map<String, Index> indexes;                // column name → index
    private final Map<String, Set<Object>> uniqueValues;     // column name → seen values (UNIQUE enforcement)

    public Table(String name, List<ColumnDefinition> columns) {
        this.name = name;
        this.columns = new ArrayList<>(columns);
        this.columnMap = new LinkedHashMap<>();
        this.records = new ArrayList<>();
        this.indexes = new HashMap<>();
        this.uniqueValues = new HashMap<>();

        for (ColumnDefinition col : columns) {
            columnMap.put(col.getName(), col);
            if (col.hasConstraint(Constraint.UNIQUE)) {
                uniqueValues.put(col.getName(), new HashSet<>());
            }
            // PRIMARY_KEY columns get an automatic index
            if (col.hasConstraint(Constraint.PRIMARY_KEY)) {
                indexes.put(col.getName(), new Index(col.getName()));
            }
        }
    }

    // ── Insert ───────────────────────────────────────────────────────────────

    public void insert(Map<String, Object> values) {
        // Reject unknown column names
        for (String key : values.keySet()) {
            if (!columnMap.containsKey(key)) {
                throw new DatabaseException("Unknown column: '" + key + "'");
            }
        }

        Map<String, Object> rowData = new LinkedHashMap<>();

        for (ColumnDefinition col : columns) {
            Object value = values.get(col.getName());

            // NOT_NULL check
            if (value == null && col.hasConstraint(Constraint.NOT_NULL)) {
                throw new DatabaseException(
                        "NOT_NULL violation: column '" + col.getName() + "' cannot be null");
            }

            if (value != null) {
                validateType(col, value);

                // UNIQUE check
                if (col.hasConstraint(Constraint.UNIQUE)
                        && uniqueValues.get(col.getName()).contains(value)) {
                    throw new DatabaseException(
                            "UNIQUE violation: duplicate value '" + value
                            + "' for column '" + col.getName() + "'");
                }
            }

            rowData.put(col.getName(), value);
        }

        // All validations passed — commit
        Row record = new Row(rowData);
        records.add(record);

        // Update unique-value tracking and indexes
        for (ColumnDefinition col : columns) {
            Object value = rowData.get(col.getName());
            if (value != null && col.hasConstraint(Constraint.UNIQUE)) {
                uniqueValues.get(col.getName()).add(value);
            }
            if (indexes.containsKey(col.getName())) {
                indexes.get(col.getName()).add(value, record);
            }
        }
    }

    private void validateType(ColumnDefinition col, Object value) {
        switch (col.getType()) {
            case STRING:
                if (!(value instanceof String)) {
                    throw new DatabaseException(
                            "Type error: column '" + col.getName() + "' expects a String");
                }
                if (((String) value).length() > MAX_STRING_LENGTH) {
                    throw new DatabaseException(
                            "String too long: column '" + col.getName()
                            + "' max length is " + MAX_STRING_LENGTH);
                }
                break;

            case INT:
                if (!(value instanceof Integer)) {
                    throw new DatabaseException(
                            "Type error: column '" + col.getName() + "' expects an Integer");
                }
                int intVal = (Integer) value;
                if (intVal < MIN_INT_VALUE || intVal > MAX_INT_VALUE) {
                    throw new DatabaseException(
                            "INT out of range: column '" + col.getName()
                            + "' value " + intVal + " must be in ["
                            + MIN_INT_VALUE + ", " + MAX_INT_VALUE + "]");
                }
                break;
        }
    }

    // ── Index ────────────────────────────────────────────────────────────────

    public void createIndex(String columnName) {
        if (!columnMap.containsKey(columnName)) {
            throw new DatabaseException(
                    "Column '" + columnName + "' does not exist in table '" + name + "'");
        }
        if (indexes.containsKey(columnName)) {
            throw new DatabaseException(
                    "Index already exists on column '" + columnName + "'");
        }
        Index index = new Index(columnName);
        for (Row r : records) {
            index.add(r.get(columnName), r);
        }
        indexes.put(columnName, index);
        System.out.println("Index created on column '" + columnName + "' in table '" + name + "'.");
    }

    // ── Query ────────────────────────────────────────────────────────────────

    public List<Row> findAll() {
        return Collections.unmodifiableList(records);
    }

    /**
     * Returns records where {@code columnName} equals {@code value}.
     * Uses the index when available; otherwise performs a linear scan.
     */
    public List<Row> findByColumnValue(String columnName, Object value) {
        if (!columnMap.containsKey(columnName)) {
            throw new DatabaseException(
                    "Column '" + columnName + "' does not exist in table '" + name + "'");
        }
        if (indexes.containsKey(columnName)) {
            return Collections.unmodifiableList(indexes.get(columnName).lookup(value));
        }
        // Linear scan
        List<Row> result = new ArrayList<>();
        for (Row r : records) {
            if (Objects.equals(r.get(columnName), value)) {
                result.add(r);
            }
        }
        return Collections.unmodifiableList(result);
    }

    // ── Print ────────────────────────────────────────────────────────────────

    public void printAll() {
        System.out.println("Table: " + name);
        renderTable(records);
    }

    public void printFiltered(String columnName, Object value) {
        System.out.println("Table: " + name + " | WHERE " + columnName + " = " + value);
        renderTable(findByColumnValue(columnName, value));
    }

    private void renderTable(List<Row> rows) {
        // Calculate column widths (header or value, whichever is wider)
        int[] widths = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).getName().length();
        }
        for (Row r : rows) {
            for (int i = 0; i < columns.size(); i++) {
                Object val = r.get(columns.get(i).getName());
                int len = (val == null) ? 4 : val.toString().length(); // "null" = 4 chars
                widths[i] = Math.max(widths[i], len);
            }
        }

        String separator = buildSeparator(widths);
        System.out.println(separator);
        System.out.println(buildRow(
                columns.stream().map(ColumnDefinition::getName).toArray(String[]::new), widths));
        System.out.println(separator);
        for (Row r : rows) {
            String[] vals = new String[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                Object val = r.get(columns.get(i).getName());
                vals[i] = (val == null) ? "null" : val.toString();
            }
            System.out.println(buildRow(vals, widths));
        }
        System.out.println(separator);
        System.out.println(rows.size() + " record(s)\n");
    }

    private String buildSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) sb.append("-".repeat(w + 2)).append("+");
        return sb.toString();
    }

    private String buildRow(String[] values, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < values.length; i++) {
            sb.append(String.format(" %-" + widths[i] + "s |", values[i]));
        }
        return sb.toString();
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getName() { return name; }
    public List<ColumnDefinition> getColumns() { return Collections.unmodifiableList(columns); }
    public boolean hasIndex(String columnName) { return indexes.containsKey(columnName); }
    public int size() { return records.size(); }
}
