package com.rcpky.index;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tag index — an exact-match index mapping tag labels to the documents that carry them.
 *
 * Why a dedicated TagIndex instead of reusing InvertedIndex?
 *   Tags are opaque labels ("data-science", "java"), not free text. Routing them
 *   through the Tokenizer would destructively split hyphenated tags:
 *     "data-science" → {"data", "science"}
 *   That would make searchByTag("data-science") fail to find the document.
 *   TagIndex bypasses tokenization entirely; it stores tags as atomic strings.
 *
 * Normalization:
 *   Tags are lowercased on BOTH insert and lookup so that "Java", "JAVA", and "java"
 *   all resolve to the same posting list. This is the only normalization applied.
 *
 * Internal structure:
 *   Same shape as InvertedIndex: HashMap<String, HashSet<Long>>
 *   — O(1) tag lookup, O(1) add/remove per ID.
 */
public class TagIndex {

    // tag (lowercased) → set of document IDs tagged with it
    private final Map<String, Set<Long>> index = new HashMap<>();

    /**
     * Registers a document's tags.
     *
     * tag.toLowerCase() normalizes case at insert time so getDocIds() can
     * always lowercase the query tag and get a consistent match.
     *
     * @param docId unique document identifier
     * @param tags  set of raw tag strings (case is preserved on the Document
     *              object but normalized here for indexing purposes)
     */
    public void addDocument(long docId, Set<String> tags) {
        for (String tag : tags) {
            index.computeIfAbsent(tag.toLowerCase(), k -> new HashSet<>()).add(docId);
        }
    }

    /**
     * Removes a document's tag entries from the index.
     *
     * The same tag.toLowerCase() normalization must be applied here as in addDocument
     * so that the key lookup resolves to the same entry that was inserted.
     *
     * @param docId ID of the document being removed
     * @param tags  original tag set from Document.getTags() — NOT re-fetched from index
     */
    public void removeDocument(long docId, Set<String> tags) {
        for (String tag : tags) {
            String key = tag.toLowerCase();
            Set<Long> ids = index.get(key);
            if (ids != null) {
                ids.remove(docId);       // O(1)
                if (ids.isEmpty()) {
                    index.remove(key);   // prune empty posting list to save memory
                }
            }
        }
    }

    /**
     * Returns all document IDs that carry the given tag.
     *
     * tag.toLowerCase() ensures callers can pass "JAVA" or "Java" and still
     * get results, matching the case-insensitive contract of searchByTag().
     *
     * @param tag case-insensitive tag label
     * @return set of matching document IDs; empty set if the tag is unknown
     */
    public Set<Long> getDocIds(String tag) {
        return index.getOrDefault(tag.toLowerCase(), Collections.emptySet());
    }

    /**
     * @return number of distinct tags currently in the index (useful for metrics)
     */
    public int uniqueTagCount() {
        return index.size();
    }
}
