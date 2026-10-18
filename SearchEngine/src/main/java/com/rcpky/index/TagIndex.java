package com.rcpky.index;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tag index: maps each normalized tag → set of document IDs carrying that tag.
 * Tags are stored case-insensitively (lowercased on insert and lookup).
 */
public class TagIndex {

    private final Map<String, Set<Long>> index = new HashMap<>();

    public void addDocument(long docId, Set<String> tags) {
        for (String tag : tags) {
            index.computeIfAbsent(tag.toLowerCase(), k -> new HashSet<>()).add(docId);
        }
    }

    public void removeDocument(long docId, Set<String> tags) {
        for (String tag : tags) {
            String key = tag.toLowerCase();
            Set<Long> ids = index.get(key);
            if (ids != null) {
                ids.remove(docId);
                if (ids.isEmpty()) {
                    index.remove(key);
                }
            }
        }
    }

    public Set<Long> getDocIds(String tag) {
        return index.getOrDefault(tag.toLowerCase(), Collections.emptySet());
    }

    public int uniqueTagCount() {
        return index.size();
    }
}
