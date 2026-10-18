package com.rcpky.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class Document {

    private static final AtomicLong ID_GEN = new AtomicLong(1);

    private final long id;
    private final String content;
    private final Set<String> tags;
    private final Set<String> tokens;
    private final long createdAt;

    public Document(String content, Set<String> tags, Set<String> tokens) {
        this.id = ID_GEN.getAndIncrement();
        this.content = content;
        this.tags = Collections.unmodifiableSet(new HashSet<>(tags));
        this.tokens = Collections.unmodifiableSet(new HashSet<>(tokens));
        this.createdAt = System.currentTimeMillis();
    }

    public long getId()            { return id; }
    public String getContent()     { return content; }
    public Set<String> getTags()   { return tags; }
    public Set<String> getTokens() { return tokens; }
    public long getCreatedAt()     { return createdAt; }

    @Override
    public String toString() {
        return "Document{id=" + id + ", content='" + content + "', tags=" + tags + "}";
    }
}
