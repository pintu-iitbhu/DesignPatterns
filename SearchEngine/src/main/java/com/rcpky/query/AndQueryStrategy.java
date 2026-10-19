package com.rcpky.query;

import com.rcpky.index.InvertedIndex;
import com.rcpky.model.Document;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AND (conjunction) query — a document must contain ALL tokens in the query.
 *
 * This is the most precise search mode; it is the default strategy because it
 * tends to return the most relevant results (no noise from partial matches).
 *
 * Algorithm — optimised posting-list intersection:
 *
 *   Naïve approach: pick any posting list as the seed, intersect with all others.
 *   Optimised approach (used here):
 *     1. Sort posting lists by size, ascending.
 *     2. Start the working set from the SMALLEST posting list.
 *     3. Call retainAll() against each subsequent (larger) list.
 *
 *   Why smallest first?
 *     retainAll() is O(|working_set|) per call. Initialising from the smallest
 *     list gives us the tightest possible starting candidate set, so each
 *     subsequent retainAll() touches fewer elements.
 *
 *   Early exit: if the working set ever becomes empty we know no document can
 *   satisfy the AND predicate — return immediately without examining remaining lists.
 *
 * Complexity: O(T × |min_list|) where T = number of query tokens.
 */
public class AndQueryStrategy implements QueryStrategy {

    @Override
    public List<Document> execute(Set<String> queryTokens,
                                  InvertedIndex invertedIndex,
                                  Map<Long, Document> store) {

        // Fetch posting lists for each token; sort smallest first for the optimisation above.
        List<Set<Long>> postingLists = queryTokens.stream()
                .map(invertedIndex::getDocIds)
                .sorted(Comparator.comparingInt(Set::size))
                .collect(Collectors.toList());

        // If the smallest posting list is empty, no document can contain all tokens.
        if (postingLists.get(0).isEmpty()) return Collections.emptyList();

        // Seed the working set with a copy of the smallest list.
        // We copy so that retainAll() does not mutate the index's internal set.
        Set<Long> result = new HashSet<>(postingLists.get(0));

        for (int i = 1; i < postingLists.size(); i++) {
            result.retainAll(postingLists.get(i)); // intersect in place
            if (result.isEmpty()) return Collections.emptyList(); // early exit
        }

        // Map surviving IDs back to Document objects.
        // Objects::nonNull guards against a race where a document was removed
        // between the index lookup and the store::get call.
        return result.stream()
                .map(store::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
