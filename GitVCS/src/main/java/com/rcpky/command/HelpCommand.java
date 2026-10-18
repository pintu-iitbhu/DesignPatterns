package com.rcpky.command;

import com.rcpky.core.Repository;

public class HelpCommand implements VcsCommand {
    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void execute(Repository repo, String[] args) {
        System.out.println("Git VCS - Available Commands:");
        System.out.println("=".repeat(60));
        registry.getCommands().values().forEach(cmd -> {
            System.out.printf("  %-35s %s%n", cmd.getUsage(), cmd.getDescription());
        });
        System.out.println("=".repeat(60));
    }

    @Override public String getName() { return "help"; }
    @Override public String getUsage() { return "help"; }
    @Override public String getDescription() { return "Display this help message."; }
}
