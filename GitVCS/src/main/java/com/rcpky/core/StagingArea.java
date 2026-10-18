package com.rcpky.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class StagingArea {
    private final Map<String, String> stagedFiles = new LinkedHashMap<>(); // filename -> blobHash
    private final Set<String> stagedForDelete = new HashSet<>();

    public void stage(String filename, String blobHash) {
        stagedFiles.put(filename, blobHash);
        stagedForDelete.remove(filename);
    }

    public void stageForDelete(String filename) {
        stagedForDelete.add(filename);
        stagedFiles.remove(filename);
    }

    public void unstage(String filename) {
        stagedFiles.remove(filename);
        stagedForDelete.remove(filename);
    }

    public boolean isEmpty() {
        return stagedFiles.isEmpty() && stagedForDelete.isEmpty();
    }

    public void clear() {
        stagedFiles.clear();
        stagedForDelete.clear();
    }

    public Map<String, String> getStagedFiles() { return Collections.unmodifiableMap(stagedFiles); }
    public Set<String> getStagedForDelete() { return Collections.unmodifiableSet(stagedForDelete); }

    public boolean isStaged(String filename) {
        return stagedFiles.containsKey(filename) || stagedForDelete.contains(filename);
    }
}
