package com.rcpky;

import com.rcpky.core.*;
import com.rcpky.exception.DatabaseException;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AppTest {

    private Database db;

    @Before
    public void setUp() {
        db = new Database("TestDB");
        db.createTable("users", Arrays.asList(
                new ColumnDefinition("id",    ColumnType.INT,    Constraint.PRIMARY_KEY),
                new ColumnDefinition("name",  ColumnType.STRING, Constraint.NOT_NULL),
                new ColumnDefinition("age",   ColumnType.INT),
                new ColumnDefinition("email", ColumnType.STRING, Constraint.UNIQUE)
        ));
    }

    // ── DDL ──────────────────────────────────────────────────────────────────

    @Test
    public void testCreateTableSuccess() {
        assertNotNull(db.getTable("users"));
    }

    @Test(expected = DatabaseException.class)
    public void testDuplicateTableThrows() {
        db.createTable("users", Arrays.asList(
                new ColumnDefinition("col", ColumnType.STRING)
        ));
    }

    @Test
    public void testDropTable() {
        db.createTable("tmp", Arrays.asList(
                new ColumnDefinition("x", ColumnType.INT)
        ));
        db.dropTable("tmp");
        assertFalse(db.listTables().contains("tmp"));
    }

    @Test(expected = DatabaseException.class)
    public void testDropNonExistentTableThrows() {
        db.dropTable("nonexistent");
    }

    @Test(expected = DatabaseException.class)
    public void testAccessDroppedTableThrows() {
        db.createTable("tmp", Arrays.asList(
                new ColumnDefinition("x", ColumnType.INT)
        ));
        db.dropTable("tmp");
        db.getTable("tmp");
    }

    @Test(expected = DatabaseException.class)
    public void testDuplicateColumnNamesThrow() {
        db.createTable("bad", Arrays.asList(
                new ColumnDefinition("col", ColumnType.INT),
                new ColumnDefinition("col", ColumnType.STRING)
        ));
    }

    // ── Insert & retrieve ─────────────────────────────────────────────────────

    @Test
    public void testInsertAndFindAll() {
        Table users = db.getTable("users");
        users.insert(row("id", 1, "name", "Alice", "age", 30, "email", "alice@x.com"));
        users.insert(row("id", 2, "name", "Bob",   "age", 25, "email", "bob@x.com"));

        List<Row> all = users.findAll();
        assertEquals(2, all.size());
        assertEquals("Alice", all.get(0).get("name"));
        assertEquals(2,       all.get(1).get("id"));
    }

    @Test(expected = DatabaseException.class)
    public void testUnknownColumnThrows() {
        db.getTable("users").insert(row("id", 1, "name", "Alice", "unknown_col", "x"));
    }

    // ── NOT_NULL constraint ───────────────────────────────────────────────────

    @Test(expected = DatabaseException.class)
    public void testNotNullViolationThrows() {
        db.getTable("users").insert(row("id", 1, "name", null, "age", 30, "email", "a@x.com"));
    }

    // ── UNIQUE constraint ─────────────────────────────────────────────────────

    @Test(expected = DatabaseException.class)
    public void testPrimaryKeyUniqueViolationThrows() {
        Table users = db.getTable("users");
        users.insert(row("id", 1, "name", "Alice", "age", 30, "email", "a@x.com"));
        users.insert(row("id", 1, "name", "Bob",   "age", 25, "email", "b@x.com"));
    }

    @Test(expected = DatabaseException.class)
    public void testUniqueColumnViolationThrows() {
        Table users = db.getTable("users");
        users.insert(row("id", 1, "name", "Alice", "age", 30, "email", "same@x.com"));
        users.insert(row("id", 2, "name", "Bob",   "age", 25, "email", "same@x.com"));
    }

    // ── Type & range validation ───────────────────────────────────────────────

    @Test(expected = DatabaseException.class)
    public void testStringMaxLengthViolationThrows() {
        db.getTable("users").insert(
                row("id", 1, "name", "TooLongNameExceedsTwenty", "age", 30, "email", "a@x.com"));
    }

    @Test(expected = DatabaseException.class)
    public void testIntAboveRangeThrows() {
        db.getTable("users").insert(
                row("id", 9999, "name", "Alice", "age", 30, "email", "a@x.com"));
    }

    @Test(expected = DatabaseException.class)
    public void testIntBelowRangeThrows() {
        db.getTable("users").insert(
                row("id", 1, "name", "Alice", "age", -2000, "email", "a@x.com"));
    }

    @Test
    public void testBoundaryIntValuesAccepted() {
        Table users = db.getTable("users");
        users.insert(row("id", -1024, "name", "Min", "age", 1024,  "email", "min@x.com"));
        users.insert(row("id",  1024, "name", "Max", "age", -1024, "email", "max@x.com"));
        assertEquals(2, users.size());
    }

    @Test
    public void testNullableColumnAcceptsNull() {
        Table users = db.getTable("users");
        users.insert(row("id", 1, "name", "Alice", "age", null, "email", "a@x.com"));
        assertNull(users.findAll().get(0).get("age"));
    }

    // ── Index ─────────────────────────────────────────────────────────────────

    @Test
    public void testPrimaryKeyAutoIndexed() {
        assertTrue(db.getTable("users").hasIndex("id"));
    }

    @Test
    public void testCreateIndex() {
        Table users = db.getTable("users");
        assertFalse(users.hasIndex("age"));
        users.createIndex("age");
        assertTrue(users.hasIndex("age"));
    }

    @Test(expected = DatabaseException.class)
    public void testDuplicateIndexThrows() {
        Table users = db.getTable("users");
        users.createIndex("age");
        users.createIndex("age");
    }

    @Test(expected = DatabaseException.class)
    public void testIndexOnNonExistentColumnThrows() {
        db.getTable("users").createIndex("nonexistent");
    }

    @Test
    public void testIndexBuiltFromExistingRecords() {
        Table users = db.getTable("users");
        users.insert(row("id", 1, "name", "Alice", "age", 30, "email", "a@x.com"));
        users.insert(row("id", 2, "name", "Bob",   "age", 25, "email", "b@x.com"));
        users.insert(row("id", 3, "name", "Carol", "age", 30, "email", "c@x.com"));

        users.createIndex("age");                         // built after inserts
        List<Row> result = users.findByColumnValue("age", 30);
        assertEquals(2, result.size());
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    @Test
    public void testFilterWithIndex() {
        Table users = db.getTable("users");
        users.insert(row("id", 1, "name", "Alice", "age", 30, "email", "a@x.com"));
        users.insert(row("id", 2, "name", "Bob",   "age", 25, "email", "b@x.com"));
        users.insert(row("id", 3, "name", "Carol", "age", 30, "email", "c@x.com"));
        users.createIndex("age");

        List<Row> result = users.findByColumnValue("age", 30);
        assertEquals(2, result.size());
    }

    @Test
    public void testFilterWithoutIndex() {
        Table users = db.getTable("users");
        users.insert(row("id", 1, "name", "Alice", "age", 30, "email", "a@x.com"));
        users.insert(row("id", 2, "name", "Bob",   "age", 25, "email", "b@x.com"));

        List<Row> result = users.findByColumnValue("name", "Alice");
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("id"));
    }

    @Test
    public void testFilterReturnsEmptyWhenNoMatch() {
        Table users = db.getTable("users");
        users.insert(row("id", 1, "name", "Alice", "age", 30, "email", "a@x.com"));

        List<Row> result = users.findByColumnValue("age", 99);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testPrimaryKeyIndexFilter() {
        Table users = db.getTable("users");
        users.insert(row("id", 42, "name", "Alice", "age", 30, "email", "a@x.com"));
        users.insert(row("id", 7,  "name", "Bob",   "age", 25, "email", "b@x.com"));

        List<Row> result = users.findByColumnValue("id", 42);
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).get("name"));
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> row(Object... kvPairs) {
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < kvPairs.length; i += 2) {
            map.put((K) kvPairs[i], (V) kvPairs[i + 1]);
        }
        return map;
    }
}
