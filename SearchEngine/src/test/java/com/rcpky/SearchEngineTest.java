package com.rcpky;

import com.rcpky.core.ISearchEngine;
import com.rcpky.core.SearchEngine;
import com.rcpky.model.Document;
import com.rcpky.query.OrQueryStrategy;
import com.rcpky.rank.RecencyRanker;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class SearchEngineTest {

    // Declared as ISearchEngine to verify tests compile against the interface contract.
    private ISearchEngine engine;

    @Before
    public void setUp() {
        // Default: SimpleTokenizer + AndQueryStrategy + NoOpRanker
        engine = new SearchEngine.Builder().build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Set<String> tags(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private boolean containsContent(List<Document> docs, String content) {
        return docs.stream().anyMatch(d -> d.getContent().equals(content));
    }

    // ── addDocument ───────────────────────────────────────────────────────────

    @Test
    public void testAddDocumentReturnsUniqueIds() {
        long id1 = engine.addDocument("hello world");
        long id2 = engine.addDocument("hello world");
        assertNotEquals(id1, id2);
        assertEquals(2, engine.documentCount());
    }

    @Test(expected = SearchException.class)
    public void testAddDocumentBlankContentThrows() {
        engine.addDocument("   ");
    }

    @Test(expected = SearchException.class)
    public void testAddDocumentNullContentThrows() {
        engine.addDocument(null);
    }

    // ── removeDocument ────────────────────────────────────────────────────────

    @Test
    public void testRemoveDocumentDecreasesCount() {
        long id = engine.addDocument("remove me");
        assertEquals(1, engine.documentCount());
        engine.removeDocument(id);
        assertEquals(0, engine.documentCount());
    }

    @Test(expected = SearchException.class)
    public void testRemoveNonExistentDocumentThrows() {
        engine.removeDocument(9999L);
    }

    @Test
    public void testRemovedDocumentNotReturnedBySearch() {
        String content = "unique canary token zxqy";
        long id = engine.addDocument(content);
        assertTrue(containsContent(engine.search("canary"), content));
        engine.removeDocument(id);
        assertFalse(containsContent(engine.search("canary"), content));
    }

    // ── search (AND query) ────────────────────────────────────────────────────

    @Test
    public void testSearchSingleWordReturnsMatchingDocs() {
        engine.addDocument("The quick brown fox");
        engine.addDocument("The lazy dog slept");
        List<Document> results = engine.search("fox");
        assertEquals(1, results.size());
        assertTrue(containsContent(results, "The quick brown fox"));
    }

    @Test
    public void testSearchAndQueryRequiresAllWords() {
        engine.addDocument("Java is a programming language");
        engine.addDocument("Python is a scripting language");
        engine.addDocument("Java and Python are programming languages");

        List<Document> results = engine.search("Java Python");
        assertEquals(1, results.size());
        assertTrue(containsContent(results, "Java and Python are programming languages"));
    }

    @Test
    public void testSearchCaseInsensitive() {
        engine.addDocument("Java Programming");
        List<Document> results = engine.search("JAVA");
        assertEquals(1, results.size());
    }

    @Test
    public void testSearchNoMatchReturnsEmpty() {
        engine.addDocument("hello world");
        assertTrue(engine.search("blockchain").isEmpty());
    }

    @Test
    public void testSearchEmptyQueryReturnsEmpty() {
        engine.addDocument("hello world");
        assertTrue(engine.search("").isEmpty());
        assertTrue(engine.search("   ").isEmpty());
    }

    @Test
    public void testSearchMultipleDocsSameWord() {
        engine.addDocument("apple pie recipe");
        engine.addDocument("apple cider vinegar");
        engine.addDocument("orange juice");
        List<Document> results = engine.search("apple");
        assertEquals(2, results.size());
    }

    @Test
    public void testSearchAndQueryPartialOverlapReturnsEmpty() {
        engine.addDocument("cat dog");
        engine.addDocument("dog fish");
        assertTrue(engine.search("cat fish").isEmpty());
    }

    // ── searchByTag ───────────────────────────────────────────────────────────

    @Test
    public void testSearchByTagReturnsTaggedDocs() {
        engine.addDocument("Java tutorial", tags("java", "programming"));
        engine.addDocument("Python tutorial", tags("python", "programming"));
        engine.addDocument("Cooking recipe", tags("food"));

        List<Document> results = engine.searchByTag("programming");
        assertEquals(2, results.size());
    }

    @Test
    public void testSearchByTagCaseInsensitive() {
        engine.addDocument("Doc A", tags("Science"));
        List<Document> results = engine.searchByTag("SCIENCE");
        assertEquals(1, results.size());
    }

    @Test
    public void testSearchByTagUnknownTagReturnsEmpty() {
        engine.addDocument("Doc A", tags("java"));
        assertTrue(engine.searchByTag("python").isEmpty());
    }

    @Test
    public void testSearchByTagEmptyTagReturnsEmpty() {
        engine.addDocument("Doc A", tags("java"));
        assertTrue(engine.searchByTag("").isEmpty());
    }

    // ── searchByTagAndQuery ───────────────────────────────────────────────────

    @Test
    public void testSearchByTagAndQueryIntersects() {
        engine.addDocument("Machine learning basics", tags("ai", "ml"));
        engine.addDocument("Deep learning networks", tags("ai", "dl"));
        engine.addDocument("Python programming basics", tags("programming"));

        List<Document> results = engine.searchByTagAndQuery("ai", "learning");
        assertEquals(2, results.size());

        results = engine.searchByTagAndQuery("ai", "deep learning");
        assertEquals(1, results.size());
        assertTrue(containsContent(results, "Deep learning networks"));
    }

    @Test
    public void testSearchByTagAndQueryNoTagMatchReturnsEmpty() {
        engine.addDocument("hello world", tags("greetings"));
        assertTrue(engine.searchByTagAndQuery("unknown-tag", "hello").isEmpty());
    }

    @Test
    public void testSearchByTagAndQueryNoWordMatchReturnsEmpty() {
        engine.addDocument("hello world", tags("greetings"));
        assertTrue(engine.searchByTagAndQuery("greetings", "blockchain").isEmpty());
    }

    // ── inverted index cleanup ────────────────────────────────────────────────

    @Test
    public void testRemoveDocumentCleansUpInvertedIndex() {
        long id = engine.addDocument("orphan term xyzzy");
        assertFalse(engine.search("xyzzy").isEmpty());
        engine.removeDocument(id);
        assertTrue(engine.search("xyzzy").isEmpty());
    }

    @Test
    public void testRemoveDocumentCleansUpTagIndex() {
        long id = engine.addDocument("tagged doc", tags("unique-tag-abc"));
        assertFalse(engine.searchByTag("unique-tag-abc").isEmpty());
        engine.removeDocument(id);
        assertTrue(engine.searchByTag("unique-tag-abc").isEmpty());
    }

    // ── OrQueryStrategy ───────────────────────────────────────────────────────

    @Test
    public void testOrQueryReturnsDocsWithAnyToken() {
        ISearchEngine orEngine = new SearchEngine.Builder()
                .queryStrategy(new OrQueryStrategy())
                .build();
        orEngine.addDocument("Java programming guide");
        orEngine.addDocument("Python data science");
        orEngine.addDocument("Ruby on Rails");

        // "java" OR "python" — should match first two, not Ruby
        List<Document> results = orEngine.search("java python");
        assertEquals(2, results.size());
        assertTrue(containsContent(results, "Java programming guide"));
        assertTrue(containsContent(results, "Python data science"));
    }

    @Test
    public void testOrQueryReturnsSingleMatch() {
        ISearchEngine orEngine = new SearchEngine.Builder()
                .queryStrategy(new OrQueryStrategy())
                .build();
        orEngine.addDocument("apple pie recipe");
        orEngine.addDocument("orange juice");

        // "apple" OR "blockchain" — only first doc matches
        List<Document> results = orEngine.search("apple blockchain");
        assertEquals(1, results.size());
        assertTrue(containsContent(results, "apple pie recipe"));
    }

    @Test
    public void testOrQueryNoMatchReturnsEmpty() {
        ISearchEngine orEngine = new SearchEngine.Builder()
                .queryStrategy(new OrQueryStrategy())
                .build();
        orEngine.addDocument("hello world");
        assertTrue(orEngine.search("blockchain nft").isEmpty());
    }

    // ── RecencyRanker ─────────────────────────────────────────────────────────

    @Test
    public void testRecencyRankerOrdersNewestFirst() throws InterruptedException {
        ISearchEngine recencyEngine = new SearchEngine.Builder()
                .ranker(new RecencyRanker())
                .build();

        recencyEngine.addDocument("oldest programming article");
        Thread.sleep(5); // ensure distinct timestamps
        recencyEngine.addDocument("newer programming tutorial");
        Thread.sleep(5);
        recencyEngine.addDocument("newest programming trends");

        List<Document> results = recencyEngine.search("programming");
        assertEquals(3, results.size());
        // Newest must appear first
        assertEquals("newest programming trends", results.get(0).getContent());
        assertEquals("oldest programming article", results.get(2).getContent());
    }

    // ── Builder validation ────────────────────────────────────────────────────

    @Test(expected = NullPointerException.class)
    public void testBuilderRejectsNullTokenizer() {
        new SearchEngine.Builder().tokenizer(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void testBuilderRejectsNullQueryStrategy() {
        new SearchEngine.Builder().queryStrategy(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void testBuilderRejectsNullRanker() {
        new SearchEngine.Builder().ranker(null).build();
    }
}
