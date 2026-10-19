package com.rcpky.rank;

import com.rcpky.model.Document;

import java.util.List;

/**
 * Identity / no-operation ranker.
 *
 * Returns results in the order produced by the QueryStrategy (which reflects
 * the document store's LinkedHashMap insertion order). This is the default
 * ranker because it has zero computational cost and is predictable in tests.
 *
 * Use a different Ranker via SearchEngine.Builder when ordering matters.
 */
public class NoOpRanker implements Ranker {

    @Override
    public List<Document> rank(List<Document> results, String query) {
        // No reordering — return the list as-is.
        // QueryStrategy already returns a mutable list, so no copy needed.
        return results;
    }
}
