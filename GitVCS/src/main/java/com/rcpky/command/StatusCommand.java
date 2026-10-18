package com.rcpky.command;

import com.rcpky.core.ObjectStore;
import com.rcpky.core.Repository;
import com.rcpky.core.StagingArea;
import com.rcpky.core.WorkingDirectory;
import com.rcpky.model.Blob;
import com.rcpky.model.Commit;
import com.rcpky.model.Tree;
import com.rcpky.util.VcsUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatusCommand implements VcsCommand {

    @Override
    public void execute(Repository repo, String[] args) {
        System.out.println("On branch " + repo.getHeadLabel());

        ObjectStore store = repo.getObjectStore();
        Commit headCommit = repo.getHeadCommit();
        Map<String, String> committedFiles = new LinkedHashMap<>();
        if (headCommit != null) {
            committedFiles = VcsUtils.reconstructFiles(store.getTree(headCommit.getTreeHash()), store);
        }

        StagingArea staging = repo.getStagingArea();
        WorkingDirectory wd = repo.getWorkingDirectory();

        List<String> stagedNew = new ArrayList<>();
        List<String> stagedModified = new ArrayList<>();
        List<String> stagedDeleted = new ArrayList<>();

        for (Map.Entry<String, String> e : staging.getStagedFiles().entrySet()) {
            String file = e.getKey();
            String stagedHash = e.getValue();
            if (!committedFiles.containsKey(file)) {
                stagedNew.add(file);
            } else if (!committedFiles.get(file).equals(stagedHash)) {
                stagedModified.add(file);
            }
        }
        staging.getStagedForDelete().forEach(stagedDeleted::add);

        Map<String, String> indexView = new LinkedHashMap<>(committedFiles);
        staging.getStagedFiles().forEach(indexView::put);
        staging.getStagedForDelete().forEach(indexView::remove);

        List<String> modifiedUnstaged = new ArrayList<>();
        List<String> deletedUnstaged = new ArrayList<>();

        for (Map.Entry<String, String> e : indexView.entrySet()) {
            String file = e.getKey();
            String indexedHash = e.getValue();
            if (!wd.hasFile(file)) {
                deletedUnstaged.add(file);
            } else {
                Blob wdBlob = new Blob(wd.getContent(file));
                if (!wdBlob.getHash().equals(indexedHash)) {
                    modifiedUnstaged.add(file);
                }
            }
        }

        List<String> untracked = new ArrayList<>();
        for (String file : wd.getFiles().keySet()) {
            if (!indexView.containsKey(file) && !staging.getStagedFiles().containsKey(file)) {
                untracked.add(file);
            }
        }

        boolean clean = stagedNew.isEmpty() && stagedModified.isEmpty() && stagedDeleted.isEmpty()
                && modifiedUnstaged.isEmpty() && deletedUnstaged.isEmpty() && untracked.isEmpty();

        if (clean) {
            System.out.println("nothing to commit, working tree clean");
            return;
        }

        if (!stagedNew.isEmpty() || !stagedModified.isEmpty() || !stagedDeleted.isEmpty()) {
            System.out.println("\nChanges to be committed:");
            stagedNew.forEach(f -> System.out.println("        new file:   " + f));
            stagedModified.forEach(f -> System.out.println("        modified:   " + f));
            stagedDeleted.forEach(f -> System.out.println("        deleted:    " + f));
        }

        if (!modifiedUnstaged.isEmpty() || !deletedUnstaged.isEmpty()) {
            System.out.println("\nChanges not staged for commit:");
            modifiedUnstaged.forEach(f -> System.out.println("        modified:   " + f));
            deletedUnstaged.forEach(f -> System.out.println("        deleted:    " + f));
        }

        if (!untracked.isEmpty()) {
            System.out.println("\nUntracked files:");
            untracked.forEach(f -> System.out.println("        " + f));
        }
    }

    @Override public String getName() { return "status"; }
    @Override public String getUsage() { return "status"; }
    @Override public String getDescription() { return "Show the working tree status."; }
}
