package com.rcpky.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable value object representing an indexed document.
 *
 * Why immutable?
 *   Indexes (InvertedIndex, TagIndex) store the token/tag sets that were
 *   active at index time. If a Document were mutable, its content could drift
 *   out of sync with the index entries: the index would still point to old tokens
 *   while the document reports new ones. Immutability makes the contract
 *   "index and document always agree" trivially true.
 *
 * Naming:
 *   Named "Document" (not "Record") to avoid clashing with java.lang.Record
 *   introduced in Java 16.
 */
public class Document {

    // Static AtomicLong ensures each Document gets a globally unique, monotonically
    // increasing ID even if multiple threads call addDocument() concurrently.
    // getAndIncrement() is a single CAS (Compare-And-Swap) operation — lock-free.
    private static final AtomicLong ID_GEN = new AtomicLong(1);

    private final long id;
    private final String content;
    private final Set<String> tags;
    private final Set<String> tokens;
    private final long createdAt;

    /**
     * @param content raw document text (stored as-is for display)
     * @param tags    raw tag labels (stored case-normalized by TagIndex, not here)
     * @param tokens  pre-computed normalized tokens (produced by Tokenizer at index time)
     */
    public Document(String content, Set<String> tags, Set<String> tokens) {
        this.id = ID_GEN.getAndIncrement();
        this.content = content;

        // Defensive copy + unmodifiable wrapper:
        //   new HashSet<>(tags)  — copy so the caller's original set mutations don't
        //                          affect this Document
        //   unmodifiableSet(…)   — prevents the caller of getTags() from mutating our
        //                          internal set (which would silently corrupt the index
        //                          since indexes hold references to these same sets)
        this.tags = Collections.unmodifiableSet(new HashSet<>(tags));
        this.tokens = Collections.unmodifiableSet(new HashSet<>(tokens));

        // Snapshot the clock at construction time; used by RecencyRanker.
        // System.currentTimeMillis() is sufficient for relative ordering;
        // use System.nanoTime() if sub-millisecond ordering is needed.
        this.createdAt = System.currentTimeMillis();
    }

    /** @return unique document ID, monotonically increasing from 1 */
    public long getId()            { return id; }

    /** @return the raw document text as supplied by the caller */
    public String getContent()     { return content; }

    /** @return unmodifiable view of the original tags (not normalized) */
    public Set<String> getTags()   { return tags; }

    /**
     * @return unmodifiable set of normalized tokens used to index this document.
     *         Used by removeDocument() to clean up posting-list entries without
     *         re-tokenizing (guarantees the exact same keys that were inserted).
     */
    public Set<String> getTokens() { return tokens; }

    /** @return epoch-millisecond timestamp of when this document was created */
    public long getCreatedAt()     { return createdAt; }

    @Override
    public String toString() {
        return "Document{id=" + id + ", content='" + content + "', tags=" + tags + "}";
    }
}
