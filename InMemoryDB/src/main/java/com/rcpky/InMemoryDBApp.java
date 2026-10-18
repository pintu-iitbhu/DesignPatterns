package com.rcpky;

import com.rcpky.core.*;
import com.rcpky.exception.DatabaseException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryDBApp {

    public static void main(String[] args) {

        Database db = new Database("CompanyDB");

        // ── Scenario 1: Create tables ──────────────────────────────────────
        System.out.println("══════════════════════════════════════════");
        System.out.println(" Scenario 1: Create Tables");
        System.out.println("══════════════════════════════════════════");

        db.createTable("employees", Arrays.asList(
                new ColumnDefinition("id",         ColumnType.INT,    Constraint.PRIMARY_KEY),
                new ColumnDefinition("name",       ColumnType.STRING, Constraint.NOT_NULL),
                new ColumnDefinition("department", ColumnType.STRING),
                new ColumnDefinition("salary",     ColumnType.INT),
                new ColumnDefinition("email",      ColumnType.STRING, Constraint.UNIQUE)
        ));

        db.createTable("departments", Arrays.asList(
                new ColumnDefinition("dept_id",   ColumnType.INT,    Constraint.PRIMARY_KEY),
                new ColumnDefinition("dept_name", ColumnType.STRING, Constraint.NOT_NULL, Constraint.UNIQUE)
        ));

        System.out.println("Tables: " + db.listTables());

        // ── Scenario 2: Insert records ─────────────────────────────────────
        System.out.println("\n══════════════════════════════════════════");
        System.out.println(" Scenario 2: Insert Records");
        System.out.println("══════════════════════════════════════════");

        Table emp = db.getTable("employees");

        emp.insert(row("id", 1,  "name", "Alice",   "department", "Engineering", "salary", 900, "email", "alice@corp.com"));
        emp.insert(row("id", 2,  "name", "Bob",     "department", "Marketing",   "salary", 750, "email", "bob@corp.com"));
        emp.insert(row("id", 3,  "name", "Charlie", "department", "Engineering", "salary", 850, "email", "charlie@corp.com"));
        emp.insert(row("id", 4,  "name", "Diana",   "department", "HR",          "salary", 700, "email", "diana@corp.com"));
        emp.insert(row("id", 5,  "name", "Eve",     "department", "Engineering", "salary", 950, "email", "eve@corp.com"));

        System.out.println("Inserted 5 employee records.");

        // ── Scenario 3: Print all records ─────────────────────────────────
        System.out.println("\n══════════════════════════════════════════");
        System.out.println(" Scenario 3: Print All Records");
        System.out.println("══════════════════════════════════════════");
        emp.printAll();

        // ── Scenario 4: Create index + indexed filter ──────────────────────
        System.out.println("══════════════════════════════════════════");
        System.out.println(" Scenario 4: Create Index on 'department' and Filter");
        System.out.println("══════════════════════════════════════════");
        emp.createIndex("department");
        System.out.println("Filter WHERE department = 'Engineering':");
        emp.printFiltered("department", "Engineering");

        // ── Scenario 5: Filter without index (linear scan) ─────────────────
        System.out.println("══════════════════════════════════════════");
        System.out.println(" Scenario 5: Filter Without Index (linear scan)");
        System.out.println("══════════════════════════════════════════");
        System.out.println("Filter WHERE salary = 750:");
        emp.printFiltered("salary", 750);

        // ── Scenario 6: Constraint violations ─────────────────────────────
        System.out.println("══════════════════════════════════════════");
        System.out.println(" Scenario 6: Constraint Violations");
        System.out.println("══════════════════════════════════════════");

        // NOT_NULL
        try {
            emp.insert(row("id", 6, "name", null, "department", "Finance", "salary", 680, "email", "frank@corp.com"));
        } catch (DatabaseException e) {
            System.out.println("[NOT_NULL]   " + e.getMessage());
        }

        // PRIMARY_KEY / UNIQUE duplicate
        try {
            emp.insert(row("id", 1, "name", "Ghost", "department", "IT", "salary", 500, "email", "ghost@corp.com"));
        } catch (DatabaseException e) {
            System.out.println("[UNIQUE-PK]  " + e.getMessage());
        }

        // UNIQUE on email
        try {
            emp.insert(row("id", 7, "name", "Hannah", "department", "IT", "salary", 600, "email", "alice@corp.com"));
        } catch (DatabaseException e) {
            System.out.println("[UNIQUE-col] " + e.getMessage());
        }

        // STRING length > 20
        try {
            emp.insert(row("id", 8, "name", "TooLongNameExceedsTwenty", "department", "IT", "salary", 600, "email", "long@corp.com"));
        } catch (DatabaseException e) {
            System.out.println("[STRING-len] " + e.getMessage());
        }

        // INT out of range
        try {
            emp.insert(row("id", 9999, "name", "Ivan", "department", "IT", "salary", 600, "email", "ivan@corp.com"));
        } catch (DatabaseException e) {
            System.out.println("[INT-range]  " + e.getMessage());
        }

        // ── Scenario 7: Drop a table ───────────────────────────────────────
        System.out.println("\n══════════════════════════════════════════");
        System.out.println(" Scenario 7: Drop Table");
        System.out.println("══════════════════════════════════════════");
        db.dropTable("departments");
        System.out.println("Tables remaining: " + db.listTables());

        try {
            db.getTable("departments");
        } catch (DatabaseException e) {
            System.out.println("Access after drop: " + e.getMessage());
        }
    }

    // Helper to build a Map from alternating key-value pairs
    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> row(Object... kvPairs) {
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < kvPairs.length; i += 2) {
            map.put((K) kvPairs[i], (V) kvPairs[i + 1]);
        }
        return map;
    }
}
