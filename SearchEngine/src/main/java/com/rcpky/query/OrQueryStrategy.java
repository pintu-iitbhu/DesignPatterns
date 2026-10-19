package com.rcpky.query;

import com.rcpky.index.InvertedIndex;
import com.rcpky.model.Document;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OR (disjunction) query — a document matches if it contains AT LEAST ONE query token.
 *
 * When to use OR instead of AND:
 *   - Broad searches where the user wants maximum recall (e.g., autocomplete, suggestion).
 *   - Queries with synonyms: "automobile car vehicle" should find any of the three.
 *   - When the AND result set is empty and the engine wants to fall back gracefully.
 *
 * Algorithm — posting-list union:
 *   Iterate over every query token, add all document IDs in its posting list to a
 *   single result HashSet. The HashSet deduplicates naturally, so a document
 *   containing multiple query tokens appears only once.
 *
 * Complexity: O(sum of |posting_list_i|) across all query tokens.
 *   This is typically larger than AND because more documents qualify. Pair with a
 *   relevance Ranker (e.g., one scoring by how many tokens a document contains)
 *   to surface the best matches first.
 */
public class OrQueryStrategy implements QueryStrategy {

    @Override
    public List<Document> execute(Set<String> queryTokens,
                                  InvertedIndex invertedIndex,
                                  Map<Long, Document> store) {

        // Union of all posting lists — a doc qualifies if any token hits
        Set<Long> result = new HashSet<>();
        for (String token : queryTokens) {
            result.addAll(invertedIndex.getDocIds(token)); // addAll = set union
        }

        if (result.isEmpty()) return Collections.emptyList();

        return result.stream()
                .map(store::get)
                .filter(Objects::nonNull) // guard against concurrent removes
                .collect(Collectors.toList());
    }
}
