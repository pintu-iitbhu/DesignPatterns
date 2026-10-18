package com.rcpky.command;

import com.rcpky.core.Repository;
import com.rcpky.exception.VcsException;
import com.rcpky.model.Commit;
import com.rcpky.model.Tree;
import com.rcpky.util.VcsUtils;

public class ResetCommand implements VcsCommand {

    @Override
    public void execute(Repository repo, String[] args) {
        if (args.length < 2) throw new VcsException("Usage: " + getUsage());

        String target = args[1];
        if (!target.startsWith("HEAD~")) {
            throw new VcsException("Expected format: HEAD~N (e.g. HEAD~1). Got: " + target);
        }

        int steps;
        try {
            steps = Integer.parseInt(target.substring(5));
        } catch (NumberFormatException e) {
            throw new VcsException("Invalid step count in: " + target);
        }
        if (steps < 0) throw new VcsException("Step count must be non-negative.");

        Commit current = repo.getHeadCommit();
        if (current == null) throw new VcsException("No commits to reset.");

        for (int i = 0; i < steps; i++) {
            if (current.getParentHash() == null) {
                throw new VcsException("Cannot reset " + steps + " steps; only " + i + " parent(s) exist.");
            }
            current = repo.getObjectStore().getCommit(current.getParentHash());
        }

        repo.advanceHead(current.getHash());
        repo.getStagingArea().clear();

        Tree tree = repo.getObjectStore().getTree(current.getTreeHash());
        repo.getWorkingDirectory().setFiles(VcsUtils.reconstructFiles(tree, repo.getObjectStore()));

        System.out.println("HEAD is now at " + current.getHash().substring(0, 7) + " " + current.getMessage());
    }

    @Override public String getName() { return "reset"; }
    @Override public String getUsage() { return "reset HEAD~<N>"; }
    @Override public String getDescription() {
        return "Reset HEAD to N commits back, discarding staged changes and updating the working directory.";
    }
}
