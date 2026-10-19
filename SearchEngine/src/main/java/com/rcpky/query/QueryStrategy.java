package com.rcpky.query;

import com.rcpky.index.InvertedIndex;
import com.rcpky.model.Document;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strategy interface for query execution over an inverted index.
 *
 * Why a strategy?
 *   The only difference between AND, OR, phrase, and proximity queries is how
 *   posting lists are combined. Extracting that single decision into a strategy
 *   keeps SearchEngine free from a growing if/else chain and lets callers
 *   choose (or mix) query semantics at construction time without subclassing.
 *
 * Contract:
 *   - Receives pre-tokenized query tokens (same tokenizer used during indexing).
 *   - Has read-only access to InvertedIndex and the document store.
 *   - Returns a mutable list — the Ranker downstream may reorder it.
 *   - MUST NOT modify the index or the store.
 */
public interface QueryStrategy {

    /**
     * Executes a query against the inverted index and resolves document objects.
     *
     * @param queryTokens  set of normalized tokens extracted from the query string
     * @param invertedIndex posting-list lookup structure
     * @param store         document store for ID → Document resolution
     * @return list of matching documents (order is implementation-defined)
     */
    List<Document> execute(Set<String> queryTokens,
                           InvertedIndex invertedIndex,
                           Map<Long, Document> store);
}
