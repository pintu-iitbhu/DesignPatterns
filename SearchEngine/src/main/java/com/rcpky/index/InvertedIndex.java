package com.rcpky.index;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Inverted index: maps each word token → set of document IDs that contain it.
 * Supports O(1) lookup per token; AND queries are resolved by intersecting posting lists.
 */
public class InvertedIndex {

    private final Map<String, Set<Long>> index = new HashMap<>();

    public void addDocument(long docId, Set<String> tokens) {
        for (String token : tokens) {
            index.computeIfAbsent(token, k -> new HashSet<>()).add(docId);
        }
    }

    public void removeDocument(long docId, Set<String> tokens) {
        for (String token : tokens) {
            Set<Long> ids = index.get(token);
            if (ids != null) {
                ids.remove(docId);
                if (ids.isEmpty()) {
                    index.remove(token);
                }
            }
        }
    }

    public Set<Long> getDocIds(String token) {
        return index.getOrDefault(token, Collections.emptySet());
    }

    public int uniqueTokenCount() {
        return index.size();
    }
}
