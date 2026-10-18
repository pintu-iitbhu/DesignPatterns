package com.rcpky.command;

import com.rcpky.core.Repository;
import com.rcpky.model.Branch;
import com.rcpky.model.Commit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogCommand implements VcsCommand {

    @Override
    public void execute(Repository repo, String[] args) {
        Commit current = repo.getHeadCommit();
        if (current == null) {
            System.out.println("No commits yet.");
            return;
        }

        while (current != null) {
            String decoration = buildDecoration(repo, current);
            System.out.println("commit " + current.getHash() + decoration);
            System.out.println("Author: " + current.getAuthor());
            System.out.println("Date:   " + current.getFormattedTimestamp());
            System.out.println();
            System.out.println("    " + current.getMessage());
            System.out.println();

            current = current.getParentHash() != null
                    ? repo.getObjectStore().getCommit(current.getParentHash())
                    : null;
        }
    }

    private String buildDecoration(Repository repo, Commit commit) {
        String headCommitHash = repo.resolveHeadCommitHash();
        String currentBranch = repo.getCurrentBranch();
        List<String> decorations = new ArrayList<>();

        for (Map.Entry<String, Branch> entry : repo.getBranches().entrySet()) {
            if (commit.getHash().equals(entry.getValue().getCommitHash())) {
                String branchName = entry.getKey();
                if (branchName.equals(currentBranch) && commit.getHash().equals(headCommitHash)) {
                    decorations.add(0, "HEAD -> " + branchName);
                } else {
                    decorations.add(branchName);
                }
            }
        }
        return decorations.isEmpty() ? "" : " (" + String.join(", ", decorations) + ")";
    }

    @Override public String getName() { return "log"; }
    @Override public String getUsage() { return "log"; }
    @Override public String getDescription() { return "Show the commit history of the current branch."; }
}
