package com.rcpky.model;

import com.rcpky.util.VcsUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Commit {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy");

    private final String hash;
    private final String parentHash;
    private final String treeHash;
    private final String message;
    private final String author;
    private final LocalDateTime timestamp;

    public Commit(String parentHash, String treeHash, String message, String author) {
        this.parentHash = parentHash;
        this.treeHash = treeHash;
        this.message = message;
        this.author = author;
        this.timestamp = LocalDateTime.now();
        this.hash = computeHash();
    }

    private String computeHash() {
        String content = "commit:" + parentHash + ":" + treeHash + ":" + message + ":" + author + ":" + timestamp;
        return VcsUtils.sha256(content);
    }

    public String getHash() { return hash; }
    public String getParentHash() { return parentHash; }
    public String getTreeHash() { return treeHash; }
    public String getMessage() { return message; }
    public String getAuthor() { return author; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    @Override
    public String toString() {
        return "Commit{hash='" + hash.substring(0, 7) + "', message='" + message + "'}";
    }
}
