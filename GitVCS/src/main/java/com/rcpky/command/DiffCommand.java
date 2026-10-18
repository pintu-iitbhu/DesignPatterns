package com.rcpky.command;

import com.rcpky.core.ObjectStore;
import com.rcpky.core.Repository;
import com.rcpky.diff.DiffEngine;
import com.rcpky.model.Commit;
import com.rcpky.model.Tree;
import com.rcpky.util.VcsUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class DiffCommand implements VcsCommand {
    private final DiffEngine diffEngine;

    public DiffCommand(DiffEngine diffEngine) {
        this.diffEngine = diffEngine;
    }

    @Override
    public void execute(Repository repo, String[] args) {
        String mode = args.length > 1 ? args[1] : "";

        ObjectStore store = repo.getObjectStore();
        Commit headCommit = repo.getHeadCommit();

        Map<String, String> committedFiles = new LinkedHashMap<>();
        if (headCommit != null) {
            committedFiles = VcsUtils.reconstructFiles(store.getTree(headCommit.getTreeHash()), store);
        }

        Map<String, String> before;
        Map<String, String> after;

        switch (mode) {
            case "--cached":
                // Staged (index) vs HEAD
                before = committedFiles;
                after = buildIndexView(committedFiles, repo);
                System.out.println("diff: staged changes vs HEAD");
                break;
            case "HEAD":
                // Working directory vs HEAD
                before = committedFiles;
                after = new LinkedHashMap<>(repo.getWorkingDirectory().getFiles());
                System.out.println("diff: working directory vs HEAD");
                break;
            default:
                // Working directory vs staging area (index)
                before = buildIndexView(committedFiles, repo);
                after = new LinkedHashMap<>(repo.getWorkingDirectory().getFiles());
                System.out.println("diff: working directory vs index (staged)");
                break;
        }

        diffEngine.diff(before, after);
    }

    private Map<String, String> buildIndexView(Map<String, String> committed, Repository repo) {
        ObjectStore store = repo.getObjectStore();
        Map<String, String> index = new LinkedHashMap<>(committed);
        repo.getStagingArea().getStagedFiles().forEach((file, hash) -> {
            com.rcpky.model.Blob blob = store.getBlob(hash);
            if (blob != null) index.put(file, blob.getContent());
        });
        repo.getStagingArea().getStagedForDelete().forEach(index::remove);
        return index;
    }

    @Override public String getName() { return "diff"; }
    @Override public String getUsage() { return "diff [--cached | HEAD]"; }
    @Override public String getDescription() {
        return "Show differences. Default: working dir vs index. --cached: index vs HEAD. HEAD: working dir vs HEAD.";
    }
}
