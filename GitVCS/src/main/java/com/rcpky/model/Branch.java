package com.rcpky.model;

public class Branch {
    private final String name;
    private String commitHash;

    public Branch(String name, String commitHash) {
        this.name = name;
        this.commitHash = commitHash;
    }

    public String getName() { return name; }
    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    @Override
    public String toString() {
        return "Branch{name='" + name + "', commit='" +
                (commitHash != null ? commitHash.substring(0, 7) : "none") + "'}";
    }
}
