package com.rcpky.rank;

import com.rcpky.model.Document;

import java.util.List;

/**
 * Strategy interface for ranking (ordering) search results.
 *
 * Why a strategy?
 *   Ranking is orthogonal to matching: a document either matches a query or it
 *   doesn't, but how to order the matches is a separate concern that changes
 *   frequently based on product requirements:
 *     - NoOpRanker      : insertion order (simplest, zero cost)
 *     - RecencyRanker   : newest documents first (news, logs, events)
 *     - TfIdfRanker     : term-frequency × inverse-document-frequency relevance
 *     - PopularityRanker: click-count or view-count descending
 *
 *   By keeping ranking behind this interface, the QueryStrategy and SearchEngine
 *   never need to change when scoring rules evolve.
 *
 * Contract:
 *   - MUST NOT filter or add documents — only reorder the input list.
 *   - The original list passed in may or may not be mutated; callers should
 *     use the returned list, not the input.
 */
public interface Ranker {

    /**
     * Reorders the result list according to this ranking strategy.
     *
     * @param results unordered list of matched documents
     * @param query   original query string (useful for relevance scoring)
     * @return ranked list (same documents, different order)
     */
    List<Document> rank(List<Document> results, String query);
}
