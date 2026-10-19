package com.rcpky.core;

import com.rcpky.model.Document;

import java.util.List;
import java.util.Set;

/**
 * Contract for any document search engine backend.
 *
 * Why an interface?
 *   - Callers depend on this abstraction, not on the concrete SearchEngine class.
 *     This is the Dependency Inversion Principle: high-level modules (SearchEngineApp,
 *     service layer, tests) should not depend on low-level details (how the index is
 *     stored, which thread model is used, etc.).
 *   - Alternative implementations can be plugged in without modifying call-sites:
 *       ReadOnlySearchEngine    — wraps a pre-built persistent index
 *       CachingSearchEngine     — wraps another ISearchEngine with a result cache
 *       DistributedSearchEngine — delegates to remote shards
 *   - Test doubles (mocks, stubs, spies) become trivial to write against this interface.
 */
public interface ISearchEngine {

    /**
     * Indexes a new document with the given content and tags.
     *
     * @param content raw text of the document; must not be null or blank
     * @param tags    optional set of labels for tag-based retrieval
     * @return auto-generated document ID (unique, monotonically increasing)
     * @throws com.rcpky.SearchException if content is null or blank
     */
    long addDocument(String content, Set<String> tags);

    /**
     * Convenience overload that indexes a tag-free document.
     *
     * @param content raw text; must not be null or blank
     * @return auto-generated document ID
     */
    long addDocument(String content);

    /**
     * Removes a document from the store and all indexes.
     *
     * @param docId ID of the document to remove
     * @throws com.rcpky.SearchException if no document with this ID exists
     */
    void removeDocument(long docId);

    /**
     * Full-text search using the engine's configured QueryStrategy.
     *
     * @param query raw query string; tokenized with the engine's Tokenizer
     * @return ranked list of matching documents; empty list if nothing matches
     */
    List<Document> search(String query);

    /**
     * Retrieves all documents carrying the given tag (case-insensitive).
     *
     * @param tag the tag to look up
     * @return list of matching documents; empty list if the tag is unknown
     */
    List<Document> searchByTag(String tag);

    /**
     * Intersection: documents that carry {@code tag} AND match {@code query}.
     *
     * @param tag   required tag label
     * @param query full-text query evaluated by the engine's QueryStrategy
     * @return intersection of tag results and full-text results
     */
    List<Document> searchByTagAndQuery(String tag, String query);

    /** @return total number of indexed documents */
    int documentCount();

    /** @return snapshot of all documents in insertion order */
    List<Document> getAllDocuments();
}
