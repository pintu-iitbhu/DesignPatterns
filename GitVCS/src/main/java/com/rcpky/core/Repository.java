package com.rcpky.core;

import com.rcpky.exception.VcsException;
import com.rcpky.model.Branch;
import com.rcpky.model.Commit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Repository {
    private boolean initialized = false;
    private final ObjectStore objectStore = new ObjectStore();
    private final StagingArea stagingArea = new StagingArea();
    private final WorkingDirectory workingDirectory = new WorkingDirectory();
    private final Map<String, Branch> branches = new LinkedHashMap<>();
    private String currentBranch;
    private String detachedHead;
    private String author;

    public Repository(String author) {
        this.author = author;
    }

    public void init(String defaultBranch) {
        if (initialized) throw new VcsException("Repository is already initialized.");
        branches.put(defaultBranch, new Branch(defaultBranch, null));
        currentBranch = defaultBranch;
        initialized = true;
    }

    public boolean isInitialized() { return initialized; }

    public Commit getHeadCommit() {
        requireInitialized();
        String commitHash = resolveHeadCommitHash();
        return commitHash != null ? objectStore.getCommit(commitHash) : null;
    }

    public String resolveHeadCommitHash() {
        if (currentBranch != null) {
            Branch branch = branches.get(currentBranch);
            return branch != null ? branch.getCommitHash() : null;
        }
        return detachedHead;
    }

    public void advanceHead(String commitHash) {
        requireInitialized();
        if (currentBranch != null) {
            branches.get(currentBranch).setCommitHash(commitHash);
        } else {
            detachedHead = commitHash;
        }
    }

    public void switchToBranch(String branchName) {
        requireInitialized();
        if (!branches.containsKey(branchName)) {
            throw new VcsException("Branch '" + branchName + "' does not exist.");
        }
        currentBranch = branchName;
        detachedHead = null;
    }

    public void createBranch(String branchName) {
        requireInitialized();
        if (branches.containsKey(branchName)) {
            throw new VcsException("Branch '" + branchName + "' already exists.");
        }
        String headHash = resolveHeadCommitHash();
        branches.put(branchName, new Branch(branchName, headHash));
    }

    private void requireInitialized() {
        if (!initialized) throw new VcsException("Repository not initialized. Run 'init' first.");
    }

    public ObjectStore getObjectStore() { return objectStore; }
    public StagingArea getStagingArea() { return stagingArea; }
    public WorkingDirectory getWorkingDirectory() { return workingDirectory; }
    public Map<String, Branch> getBranches() { return Collections.unmodifiableMap(branches); }
    public String getCurrentBranch() { return currentBranch; }
    public String getAuthor() { return author; }

    public String getHeadLabel() {
        requireInitialized();
        return currentBranch != null ? currentBranch : ("detached:" + detachedHead.substring(0, 7));
    }
}
