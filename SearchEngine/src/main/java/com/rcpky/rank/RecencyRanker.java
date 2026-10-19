package com.rcpky.rank;

import com.rcpky.model.Document;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Recency ranker — sorts results by creation timestamp, newest first.
 *
 * When to use:
 *   - News feeds, event logs, or any domain where freshness is the primary
 *     quality signal (e.g., "latest articles about machine learning").
 *
 * Why descending order?
 *   Users almost always want the most recent item first; scanning from oldest
 *   to newest requires scrolling to the bottom, which is poor UX for large
 *   result sets.
 *
 * Complexity: O(n log n) — a standard comparison sort over the result set.
 *   For very large result sets, consider a partial sort (e.g., top-K heap)
 *   if only the first page of results is needed.
 */
public class RecencyRanker implements Ranker {

    @Override
    public List<Document> rank(List<Document> results, String query) {
        // Comparator.comparingLong builds a Comparator on the createdAt long field.
        // .reversed() flips it from ascending (oldest first) to descending (newest first).
        return results.stream()
                .sorted(Comparator.comparingLong(Document::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
}
