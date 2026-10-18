package com.rcpky.command;

import com.rcpky.core.Repository;

public interface VcsCommand {
    void execute(Repository repo, String[] args);
    String getName();
    String getUsage();
    String getDescription();
}
