package com.rcpky.index;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Inverted index — the core data structure behind full-text search.
 *
 * What is an inverted index?
 *   A forward index maps document → list of words it contains.
 *   An INVERTED index flips that: it maps word → list of documents that contain it.
 *   This "inversion" is what makes text search fast: instead of scanning every
 *   document for a query term, we do a single O(1) HashMap lookup.
 *
 * Internal structure:
 *   HashMap<String, HashSet<Long>>
 *     key   : normalized token (lowercase, split by SimpleTokenizer)
 *     value : HashSet of document IDs (the "posting list")
 *
 *   HashMap over TreeMap: O(1) average get/put vs O(log n). We don't need
 *   sorted token iteration so TreeMap's overhead is unjustified.
 *
 *   HashSet over ArrayList for posting lists: O(1) add and O(1) remove vs O(n).
 *   Also eliminates duplicate IDs automatically, which matters when a token
 *   appears multiple times in the same document.
 *
 * Thread safety: NOT thread-safe. Wrap with synchronization or use
 * ConcurrentHashMap + CopyOnWriteArraySet if concurrent access is needed.
 */
public class InvertedIndex {

    // token → set of document IDs containing that token
    private final Map<String, Set<Long>> index = new HashMap<>();

    /**
     * Registers a document's tokens in the index.
     *
     * computeIfAbsent: creates the posting-list HashSet only on first encounter
     * of a token, avoiding an explicit null-check on every insert. This is idiomatic
     * and avoids a get-then-put race in single-threaded code.
     *
     * @param docId  unique identifier of the document being indexed
     * @param tokens set of normalized tokens extracted from the document's content
     */
    public void addDocument(long docId, Set<String> tokens) {
        for (String token : tokens) {
            index.computeIfAbsent(token, k -> new HashSet<>()).add(docId);
        }
    }

    /**
     * Removes a document's token entries from the index.
     *
     * Why remove the posting-list entry entirely when it becomes empty?
     *   An empty HashSet still occupies memory and adds noise to uniqueTokenCount().
     *   Pruning eagerly keeps the index compact.
     *
     * @param docId  ID of the document being removed
     * @param tokens the exact token set that was used when the document was indexed
     *               (retrieved from Document.getTokens() — never re-tokenize here
     *               because any tokenizer state change would produce the wrong keys)
     */
    public void removeDocument(long docId, Set<String> tokens) {
        for (String token : tokens) {
            Set<Long> ids = index.get(token);
            if (ids != null) {
                ids.remove(docId);          // O(1) HashSet remove
                if (ids.isEmpty()) {
                    index.remove(token);    // prune the now-empty posting list
                }
            }
        }
    }

    /**
     * Returns the posting list for a token — the set of IDs of documents
     * that contain this token.
     *
     * Returns Collections.emptySet() (not null) for unknown tokens so callers
     * can safely call Set operations (size, retainAll, addAll) without null checks.
     *
     * @param token normalized query token (must use the same normalization as indexing)
     * @return unmodifiable posting list; empty set if the token is not in the index
     */
    public Set<Long> getDocIds(String token) {
        return index.getOrDefault(token, Collections.emptySet());
    }

    /**
     * @return the number of distinct tokens currently in the index (useful for metrics)
     */
    public int uniqueTokenCount() {
        return index.size();
    }
}
