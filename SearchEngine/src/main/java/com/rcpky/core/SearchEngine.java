package com.rcpky.core;

import com.rcpky.SearchException;
import com.rcpky.index.InvertedIndex;
import com.rcpky.index.TagIndex;
import com.rcpky.model.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core search engine facade.
 *
 * Data structures:
 *   store         — LinkedHashMap<Long, Document>      O(1) doc retrieval by ID
 *   invertedIndex — HashMap<String, HashSet<Long>>     O(1) posting-list lookup per token
 *   tagIndex      — HashMap<String, HashSet<Long>>     O(1) tag-to-doc lookup
 *
 * AND search complexity: O(T * min_posting_list) where T = number of query tokens.
 * The smallest posting list is chosen first to minimise intersection work.
 */
public class SearchEngine {

    private final Map<Long, Document> store = new LinkedHashMap<>();
    private final InvertedIndex invertedIndex = new InvertedIndex();
    private final TagIndex tagIndex = new TagIndex();

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        return Arrays.stream(text.toLowerCase().split("[^a-zA-Z0-9]+"))
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toSet());
    }

    public long addDocument(String content, Set<String> tags) {
        if (content == null || content.isBlank()) {
            throw new SearchException("Document content cannot be null or blank");
        }
        Set<String> tokens = tokenize(content);
        Set<String> normalizedTags = (tags == null) ? Collections.emptySet() : tags;
        Document doc = new Document(content, normalizedTags, tokens);
        store.put(doc.getId(), doc);
        invertedIndex.addDocument(doc.getId(), tokens);
        tagIndex.addDocument(doc.getId(), normalizedTags);
        return doc.getId();
    }

    public long addDocument(String content) {
        return addDocument(content, Collections.emptySet());
    }

    public void removeDocument(long docId) {
        Document doc = store.remove(docId);
        if (doc == null) {
            throw new SearchException("Document not found: " + docId);
        }
        invertedIndex.removeDocument(docId, doc.getTokens());
        tagIndex.removeDocument(docId, doc.getTags());
    }

    /**
     * AND search: returns every document whose content contains ALL tokens in the query.
     * Strategy: sort posting lists smallest-first, then progressively intersect.
     */
    public List<Document> search(String query) {
        Set<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) return Collections.emptyList();

        List<Set<Long>> postingLists = queryTokens.stream()
                .map(invertedIndex::getDocIds)
                .sorted(Comparator.comparingInt(Set::size))
                .collect(Collectors.toList());

        if (postingLists.get(0).isEmpty()) return Collections.emptyList();

        Set<Long> result = new HashSet<>(postingLists.get(0));
        for (int i = 1; i < postingLists.size(); i++) {
            result.retainAll(postingLists.get(i));
            if (result.isEmpty()) return Collections.emptyList();
        }

        return result.stream()
                .map(store::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns all documents carrying the given tag (case-insensitive).
     */
    public List<Document> searchByTag(String tag) {
        if (tag == null || tag.isBlank()) return Collections.emptyList();
        return tagIndex.getDocIds(tag).stream()
                .map(store::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Intersection of tag search and AND query search.
     * Returns documents that both carry the given tag AND contain all query tokens.
     */
    public List<Document> searchByTagAndQuery(String tag, String query) {
        Set<Long> tagResults = new HashSet<>(tagIndex.getDocIds(tag));
        if (tagResults.isEmpty()) return Collections.emptyList();

        return search(query).stream()
                .filter(d -> tagResults.contains(d.getId()))
                .collect(Collectors.toList());
    }

    public int documentCount()           { return store.size(); }
    public List<Document> getAllDocuments() { return new ArrayList<>(store.values()); }
}
