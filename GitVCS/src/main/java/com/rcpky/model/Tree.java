package com.rcpky.model;

import com.rcpky.util.VcsUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Tree {
    private final String hash;
    private final Map<String, String> entries; // filename -> blobHash

    public Tree(Map<String, String> entries) {
        this.entries = new LinkedHashMap<>(entries);
        this.hash = computeHash();
    }

    private String computeHash() {
        StringBuilder sb = new StringBuilder("tree:");
        entries.forEach((file, blobHash) -> sb.append(file).append(":").append(blobHash).append(";"));
        return VcsUtils.sha256(sb.toString());
    }

    public String getHash() { return hash; }
    public Map<String, String> getEntries() { return Collections.unmodifiableMap(entries); }

    @Override
    public String toString() {
        return "Tree{hash='" + hash.substring(0, 7) + "', files=" + entries.keySet() + "}";
    }
}
