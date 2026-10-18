package com.rcpky.command;

import com.rcpky.core.ObjectStore;
import com.rcpky.core.Repository;
import com.rcpky.core.StagingArea;
import com.rcpky.core.WorkingDirectory;
import com.rcpky.exception.VcsException;
import com.rcpky.model.Blob;
import com.rcpky.model.Commit;
import com.rcpky.model.Tree;
import com.rcpky.util.VcsUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class CommitCommand implements VcsCommand {

    @Override
    public void execute(Repository repo, String[] args) {
        boolean addAll = false;
        String message = null;

        for (int i = 1; i < args.length; i++) {
            if ("-am".equals(args[i]) && i + 1 < args.length) {
                addAll = true;
                message = args[++i];
            } else if ("-m".equals(args[i]) && i + 1 < args.length) {
                message = args[++i];
            }
        }

        if (message == null) throw new VcsException("Usage: " + getUsage());

        if (addAll) {
            stageAllTrackedFiles(repo);
        }

        StagingArea staging = repo.getStagingArea();
        if (staging.isEmpty()) {
            throw new VcsException("Nothing to commit (staging area is empty). Use 'add' to stage files.");
        }

        ObjectStore store = repo.getObjectStore();
        Commit parentCommit = repo.getHeadCommit();

        Map<String, String> treeEntries = new LinkedHashMap<>();
        if (parentCommit != null) {
            Tree parentTree = store.getTree(parentCommit.getTreeHash());
            if (parentTree != null) treeEntries.putAll(parentTree.getEntries());
        }
        treeEntries.putAll(staging.getStagedFiles());
        staging.getStagedForDelete().forEach(treeEntries::remove);

        Tree tree = new Tree(treeEntries);
        store.storeTree(tree);

        String parentHash = parentCommit != null ? parentCommit.getHash() : null;
        Commit commit = new Commit(parentHash, tree.getHash(), message, repo.getAuthor());
        store.storeCommit(commit);

        repo.advanceHead(commit.getHash());
        staging.clear();

        System.out.println("[" + repo.getHeadLabel() + " " + commit.getHash().substring(0, 7) + "] " + message);
        System.out.println(" " + treeEntries.size() + " file(s) in tree.");
    }

    private void stageAllTrackedFiles(Repository repo) {
        Commit headCommit = repo.getHeadCommit();
        if (headCommit == null) return;

        Tree headTree = repo.getObjectStore().getTree(headCommit.getTreeHash());
        if (headTree == null) return;

        WorkingDirectory wd = repo.getWorkingDirectory();
        ObjectStore store = repo.getObjectStore();
        StagingArea staging = repo.getStagingArea();

        for (String trackedFile : headTree.getEntries().keySet()) {
            if (wd.hasFile(trackedFile)) {
                Blob blob = new Blob(wd.getContent(trackedFile));
                store.storeBlob(blob);
                staging.stage(trackedFile, blob.getHash());
            } else {
                staging.stageForDelete(trackedFile);
            }
        }
    }

    @Override public String getName() { return "commit"; }
    @Override public String getUsage() { return "commit -m <message>  |  commit -am <message>"; }
    @Override public String getDescription() {
        return "Record staged changes. -am also stages all tracked files before committing.";
    }
}
