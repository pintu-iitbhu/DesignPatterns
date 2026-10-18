package com.rcpky.core;

import com.rcpky.model.Blob;
import com.rcpky.model.Commit;
import com.rcpky.model.Tree;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ObjectStore {
    private final Map<String, Blob> blobs = new LinkedHashMap<>();
    private final Map<String, Tree> trees = new LinkedHashMap<>();
    private final Map<String, Commit> commits = new LinkedHashMap<>();

    public void storeBlob(Blob blob) { blobs.put(blob.getHash(), blob); }
    public void storeTree(Tree tree) { trees.put(tree.getHash(), tree); }
    public void storeCommit(Commit commit) { commits.put(commit.getHash(), commit); }

    public Blob getBlob(String hash) { return blobs.get(hash); }
    public Tree getTree(String hash) { return trees.get(hash); }
    public Commit getCommit(String hash) { return commits.get(hash); }

    public Collection<Commit> getAllCommits() { return Collections.unmodifiableCollection(commits.values()); }

    public boolean hasBlob(String hash) { return blobs.containsKey(hash); }
}
