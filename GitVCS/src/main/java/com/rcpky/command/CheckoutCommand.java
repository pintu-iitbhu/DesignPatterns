package com.rcpky.command;

import com.rcpky.core.Repository;
import com.rcpky.exception.VcsException;
import com.rcpky.model.Commit;
import com.rcpky.model.Tree;
import com.rcpky.util.VcsUtils;

public class CheckoutCommand implements VcsCommand {

    @Override
    public void execute(Repository repo, String[] args) {
        boolean createNew = false;
        String branchName = null;

        for (int i = 1; i < args.length; i++) {
            if ("-b".equals(args[i])) {
                createNew = true;
            } else {
                branchName = args[i];
            }
        }

        if (branchName == null) throw new VcsException("Usage: " + getUsage());

        if (createNew) {
            repo.createBranch(branchName);
            System.out.println("Created branch '" + branchName + "'.");
        }

        repo.switchToBranch(branchName);

        Commit headCommit = repo.getHeadCommit();
        if (headCommit != null) {
            Tree tree = repo.getObjectStore().getTree(headCommit.getTreeHash());
            repo.getWorkingDirectory().setFiles(VcsUtils.reconstructFiles(tree, repo.getObjectStore()));
        } else {
            repo.getWorkingDirectory().clear();
        }
        repo.getStagingArea().clear();

        System.out.println("Switched to branch '" + branchName + "'");
    }

    @Override public String getName() { return "checkout"; }
    @Override public String getUsage() { return "checkout [-b] <branch>"; }
    @Override public String getDescription() {
        return "Switch to a branch. Use -b to create and switch to a new branch.";
    }
}
