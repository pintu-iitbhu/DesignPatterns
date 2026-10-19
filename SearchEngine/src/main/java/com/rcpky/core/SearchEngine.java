package com.rcpky.core;

import com.rcpky.SearchException;
import com.rcpky.index.InvertedIndex;
import com.rcpky.index.TagIndex;
import com.rcpky.model.Document;
import com.rcpky.query.AndQueryStrategy;
import com.rcpky.query.QueryStrategy;
import com.rcpky.rank.NoOpRanker;
import com.rcpky.rank.Ranker;
import com.rcpky.tokenizer.SimpleTokenizer;
import com.rcpky.tokenizer.Tokenizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core in-memory search engine — the primary implementation of ISearchEngine.
 *
 * Three pluggable strategies are injected via the inner Builder:
 *   Tokenizer     — how raw text is split into searchable tokens
 *   QueryStrategy — how posting lists are combined (AND / OR / phrase / …)
 *   Ranker        — how the matched document list is ordered before returning
 *
 * Internal data structures:
 *   store         — LinkedHashMap<Long, Document>
 *                   Preserves document insertion order (stable getAllDocuments()
 *                   output) while giving O(1) lookup by ID.
 *   invertedIndex — HashMap<String, HashSet<Long>>   O(1) per-token posting lookup
 *   tagIndex      — HashMap<String, HashSet<Long>>   O(1) per-tag doc lookup
 *                   Tags are stored lower-cased so lookup is case-insensitive.
 *
 * Default strategies (used when not overridden in Builder):
 *   SimpleTokenizer, AndQueryStrategy, NoOpRanker
 */
public class SearchEngine implements ISearchEngine {

    // ── document store ────────────────────────────────────────────────────────

    // LinkedHashMap: O(1) get/put by ID AND stable iteration order.
    // A plain HashMap would also give O(1) but would produce non-deterministic
    // iteration order, making getAllDocuments() output unpredictable.
    private final Map<Long, Document> store = new LinkedHashMap<>();

    // Two separate indexes because their normalization rules differ:
    //   invertedIndex lowercases via Tokenizer (content tokens)
    //   tagIndex      always lowercases regardless of Tokenizer (tag labels)
    private final InvertedIndex invertedIndex = new InvertedIndex();
    private final TagIndex tagIndex = new TagIndex();

    // ── pluggable strategies ──────────────────────────────────────────────────

    private final Tokenizer tokenizer;       // splits content/query into tokens
    private final QueryStrategy queryStrategy; // combines posting lists into results
    private final Ranker ranker;              // orders the result list

    // Private constructor forces callers through Builder, ensuring all
    // strategies are validated (non-null) before the engine is constructed.
    private SearchEngine(Builder builder) {
        this.tokenizer = builder.tokenizer;
        this.queryStrategy = builder.queryStrategy;
        this.ranker = builder.ranker;
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    /**
     * Builder Pattern: assembles a SearchEngine with optional strategy overrides.
     *
     * Why Builder and not a constructor with 3 parameters?
     *   - Constructor parameter order is error-prone and unreadable for 3+ arguments
     *     of the same abstract type (Tokenizer / QueryStrategy / Ranker are unrelated
     *     interfaces, but a future caller could easily swap positions by mistake).
     *   - Builder makes each override named and explicit.
     *   - Defaults are declared once here, not scattered across call-sites.
     *
     * Usage:
     *   ISearchEngine engine = new SearchEngine.Builder()
     *       .queryStrategy(new OrQueryStrategy())
     *       .ranker(new RecencyRanker())
     *       .build();
     */
    public static class Builder {
        // Sensible defaults so new Builder().build() produces a working engine
        private Tokenizer tokenizer         = new SimpleTokenizer();
        private QueryStrategy queryStrategy = new AndQueryStrategy();
        private Ranker ranker               = new NoOpRanker();

        /** Override the tokenization strategy. */
        public Builder tokenizer(Tokenizer tokenizer) {
            this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer must not be null");
            return this;
        }

        /** Override the query execution strategy (AND / OR / phrase / …). */
        public Builder queryStrategy(QueryStrategy queryStrategy) {
            this.queryStrategy = Objects.requireNonNull(queryStrategy, "queryStrategy must not be null");
            return this;
        }

        /** Override the result ranking/ordering strategy. */
        public Builder ranker(Ranker ranker) {
            this.ranker = Objects.requireNonNull(ranker, "ranker must not be null");
            return this;
        }

        /** Constructs and returns the configured SearchEngine. */
        public SearchEngine build() {
            return new SearchEngine(this);
        }
    }

    // ── write operations ──────────────────────────────────────────────────────

    /**
     * Tokenizes and indexes a document, then returns its auto-generated ID.
     *
     * Why reject blank content?
     *   A document with only whitespace would tokenize to an empty token set,
     *   making it permanently unreachable via search(). Failing fast here
     *   prevents silent data loss (data stored but never retrievable).
     *
     * Write order matters:
     *   store → invertedIndex → tagIndex
     *   All three are in-memory and always succeed together. If the constructor
     *   threw (e.g., bad ID), we never reach store.put(), so no partial state.
     */
    @Override
    public long addDocument(String content, Set<String> tags) {
        if (content == null || content.trim().isEmpty()) {
            throw new SearchException("Document content cannot be null or blank");
        }
        // Tokenize using the injected strategy so indexing and querying
        // share an identical token space (same tokenizer = same normalization).
        Set<String> tokens = tokenizer.tokenize(content);

        // null-safe: callers that pass null tags are treated as "no tags"
        Set<String> normalizedTags = (tags == null) ? Collections.emptySet() : tags;

        Document doc = new Document(content, normalizedTags, tokens);
        store.put(doc.getId(), doc);
        invertedIndex.addDocument(doc.getId(), tokens);
        tagIndex.addDocument(doc.getId(), normalizedTags);
        return doc.getId();
    }

    /** Convenience overload: indexes a tag-free document. */
    @Override
    public long addDocument(String content) {
        return addDocument(content, Collections.emptySet());
    }

    /**
     * Removes a document from the store and purges its entries from both indexes.
     *
     * Why clean up both indexes eagerly?
     *   Stale entries (a deleted doc's ID still in a posting list) would cause
     *   phantom results: a search would return IDs that no longer exist in store.
     *   The Objects::nonNull guard in search() would silently swallow them, but
     *   leaving them in wastes memory and pollutes intersection logic in AND queries.
     */
    @Override
    public void removeDocument(long docId) {
        Document doc = store.remove(docId); // returns null if not found
        if (doc == null) {
            throw new SearchException("Document not found: " + docId);
        }
        // Remove token → docId entries; cleans up posting lists for all indexed tokens
        invertedIndex.removeDocument(docId, doc.getTokens());
        // Remove tag → docId entries; cleans up posting lists for all document tags
        tagIndex.removeDocument(docId, doc.getTags());
    }

    // ── read / search operations ──────────────────────────────────────────────

    /**
     * Full-text search, delegating matching to QueryStrategy and ordering to Ranker.
     *
     * Pipeline:
     *   raw query string
     *     → Tokenizer.tokenize()         (normalize, same rules as indexing)
     *     → QueryStrategy.execute()      (resolve posting lists → Document list)
     *     → Ranker.rank()                (reorder by relevance / recency / …)
     *     → caller
     *
     * Why use the same Tokenizer for both indexing and querying?
     *   Token space must be consistent. If indexing lowercases and the query doesn't,
     *   "Java" at query time would never hit the "java" token in the index.
     */
    @Override
    public List<Document> search(String query) {
        Set<String> queryTokens = tokenizer.tokenize(query);
        // Empty query → return nothing; avoids accidentally returning all documents.
        if (queryTokens.isEmpty()) return Collections.emptyList();

        // QueryStrategy resolves posting lists and maps IDs to Document objects.
        List<Document> results = queryStrategy.execute(queryTokens, invertedIndex, store);

        // Ranker reorders the results; it MUST NOT filter or add documents.
        return ranker.rank(results, query);
    }

    /**
     * Tag-only search — O(1) TagIndex lookup followed by an ID → Document mapping.
     *
     * Why separate from search()?
     *   Tags are exact labels (e.g., "java", "ai"), not full-text. Routing tag
     *   lookups through the inverted index would force them through tokenization,
     *   which could split hyphenated tags ("data-science" → {"data","science"}).
     *   A dedicated TagIndex preserves tag integrity.
     */
    @Override
    public List<Document> searchByTag(String tag) {
        if (tag == null || tag.isEmpty()) return Collections.emptyList();
        return tagIndex.getDocIds(tag).stream()  // O(1) tag lookup
                .map(store::get)                  // O(1) doc lookup per ID
                .filter(Objects::nonNull)          // defensive null-guard
                .collect(Collectors.toList());
    }

    /**
     * Intersection of tag-filter and full-text search.
     *
     * Strategy: run the cheaper tag lookup first (O(1)) to get a candidate ID set,
     * then filter the full-text results (which may be costlier) to only those IDs.
     *
     * Why not the other way around?
     *   Tag sets are usually small subsets of the corpus, so they prune the
     *   candidate space cheaply before the potentially larger text-search result
     *   set is even computed.
     *
     * Why not re-implement intersection manually?
     *   Delegating to search() reuses QueryStrategy and Ranker for free.
     */
    @Override
    public List<Document> searchByTagAndQuery(String tag, String query) {
        Set<Long> tagResults = new HashSet<>(tagIndex.getDocIds(tag));
        // Short-circuit: if no docs have this tag, skip the text search entirely.
        if (tagResults.isEmpty()) return Collections.emptyList();

        return search(query).stream()
                .filter(d -> tagResults.contains(d.getId())) // O(1) HashSet.contains
                .collect(Collectors.toList());
    }

    // ── accessors ─────────────────────────────────────────────────────────────

    /** @return total number of indexed documents */
    @Override
    public int documentCount() { return store.size(); }

    /**
     * Returns a snapshot list of all documents in insertion order.
     *
     * new ArrayList wraps the LinkedHashMap's values view to prevent callers
     * from holding a live reference to the internal collection and accidentally
     * observing future adds/removes.
     */
    @Override
    public List<Document> getAllDocuments() { return new ArrayList<>(store.values()); }
}
