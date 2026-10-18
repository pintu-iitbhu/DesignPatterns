package com.rcpky.command;

import com.rcpky.core.Repository;

public class InitCommand implements VcsCommand {

    @Override
    public void execute(Repository repo, String[] args) {
        String branch = args.length > 1 ? args[1] : "main";
        repo.init(branch);
        System.out.println("Initialized empty Git VCS repository (default branch: " + branch + ")");
    }

    @Override public String getName() { return "init"; }
    @Override public String getUsage() { return "init [branch-name]"; }
    @Override public String getDescription() { return "Initialize a new repository (default branch: main)."; }
}
