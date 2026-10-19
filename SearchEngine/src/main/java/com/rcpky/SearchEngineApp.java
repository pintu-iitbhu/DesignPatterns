package com.rcpky;

import com.rcpky.core.ISearchEngine;
import com.rcpky.core.SearchEngine;
import com.rcpky.model.Document;
import com.rcpky.query.OrQueryStrategy;
import com.rcpky.rank.RecencyRanker;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Demonstrates all SearchEngine capabilities across 9 scenarios.
 *
 * The app is written against ISearchEngine (not SearchEngine) so that the
 * concrete backend can be swapped without touching this class — Dependency
 * Inversion in action.
 */
public class SearchEngineApp {

    public static void main(String[] args) {

        // ── Default engine: AND query + NoOpRanker ──────────────────────────
        // Builder.build() wires in SimpleTokenizer, AndQueryStrategy, NoOpRanker.
        // No args needed for the common case; Builder lets us override selectively.
        ISearchEngine engine = new SearchEngine.Builder().build();

        System.out.println("=== Search Engine Demo ===\n");

        // ── Scenario 1: Add documents ───────────────────────────────────────
        System.out.println("--- Scenario 1: Add Documents ---");
        long d1 = engine.addDocument(
                "Java is a high-level object-oriented programming language",
                tags("java", "programming", "oop"));
        long d2 = engine.addDocument(
                "Python is a versatile programming language for data science",
                tags("python", "programming", "data-science"));
        long d3 = engine.addDocument(
                "Machine learning is a subset of artificial intelligence",
                tags("ai", "machine-learning", "data-science"));
        long d4 = engine.addDocument(
                "Java and Python are both popular programming languages",
                tags("java", "python", "programming"));
        long d5 = engine.addDocument(
                "Deep learning uses neural networks for artificial intelligence",
                tags("ai", "deep-learning", "machine-learning"));
        System.out.println("Added 5 documents. Total: " + engine.documentCount());
        engine.getAllDocuments().forEach(System.out::println);

        // ── Scenario 2: Single-word AND search ─────────────────────────────
        System.out.println("\n--- Scenario 2: Single-word search: 'java' ---");
        printResults(engine.search("java"));

        // ── Scenario 3: Multi-word AND search ──────────────────────────────
        // AND: document must contain ALL tokens — "java" AND "programming"
        System.out.println("\n--- Scenario 3: AND search: 'java programming' ---");
        printResults(engine.search("java programming"));

        // ── Scenario 4: Search by tag ───────────────────────────────────────
        System.out.println("\n--- Scenario 4: Search by tag: 'data-science' ---");
        printResults(engine.searchByTag("data-science"));

        // ── Scenario 5: Combined tag + AND query search ─────────────────────
        System.out.println("\n--- Scenario 5: Tag='ai' AND query='learning' ---");
        printResults(engine.searchByTagAndQuery("ai", "learning"));

        // ── Scenario 6: Remove a document and verify ───────────────────────
        System.out.println("\n--- Scenario 6: Remove doc " + d1 + " then search 'java' ---");
        engine.removeDocument(d1);
        System.out.println("Documents after removal: " + engine.documentCount());
        printResults(engine.search("java"));

        // ── Scenario 7: OR query strategy (plugged via Builder) ─────────────
        // A new engine instance with OrQueryStrategy: a document qualifies if
        // it contains ANY of the query tokens (union of posting lists).
        // This is useful for broad/suggestion-style searches.
        System.out.println("\n--- Scenario 7: OR search: 'java python' (any token matches) ---");
        ISearchEngine orEngine = new SearchEngine.Builder()
                .queryStrategy(new OrQueryStrategy())
                .build();
        orEngine.addDocument("Java programming guide",    tags("java"));
        orEngine.addDocument("Python data science book",  tags("python"));
        orEngine.addDocument("Ruby on Rails tutorial",    tags("ruby"));
        // "java OR python" should return the first two but not "ruby"
        printResults(orEngine.search("java python"));

        // ── Scenario 8: RecencyRanker (plugged via Builder) ─────────────────
        // Same AND semantics, but results are sorted newest-first.
        // Useful for news feeds or event logs where freshness matters.
        System.out.println("\n--- Scenario 8: RecencyRanker — search 'programming' (newest first) ---");
        ISearchEngine recencyEngine = new SearchEngine.Builder()
                .ranker(new RecencyRanker())
                .build();
        recencyEngine.addDocument("Old Java intro",            tags("java"));
        recencyEngine.addDocument("Newer Python cookbook",     tags("python"));
        recencyEngine.addDocument("Newest programming trends", tags("tech"));
        // All three contain "programming" — RecencyRanker returns newest first
        List<Document> recencyResults = recencyEngine.search("programming");
        System.out.println("  Results (newest first):");
        recencyResults.forEach(d -> System.out.println("  " + d));

        // ── Scenario 9: Edge cases ──────────────────────────────────────────
        System.out.println("\n--- Scenario 9: Edge cases ---");
        List<Document> emptyQuery = engine.search("");
        System.out.println("Empty query result size: " + emptyQuery.size() + " (expected 0)");

        List<Document> noMatch = engine.search("blockchain");
        System.out.println("Unknown word result size: " + noMatch.size() + " (expected 0)");

        List<Document> tagInsensitive = engine.searchByTag("PROGRAMMING");
        System.out.println("Tag search case-insensitive ('PROGRAMMING') size: "
                + tagInsensitive.size() + " (expected 2)");

        try {
            engine.removeDocument(9999L);
        } catch (SearchException e) {
            System.out.println("Caught expected error: " + e.getMessage());
        }

        System.out.println("\n=== Demo complete ===");
    }

    private static Set<String> tags(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static void printResults(List<Document> docs) {
        if (docs.isEmpty()) {
            System.out.println("  (no results)");
        } else {
            docs.forEach(d -> System.out.println("  " + d));
        }
    }
}
