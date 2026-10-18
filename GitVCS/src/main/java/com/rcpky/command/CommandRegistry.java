package com.rcpky.command;

import com.rcpky.core.Repository;
import com.rcpky.diff.LineDiffEngine;
import com.rcpky.exception.VcsException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommandRegistry {
    private final Map<String, VcsCommand> commands = new LinkedHashMap<>();

    public CommandRegistry() {
        HelpCommand helpCommand = new HelpCommand(this);
        register(new InitCommand());
        register(new AddCommand());
        register(new CommitCommand());
        register(new StatusCommand());
        register(new LogCommand());
        register(new CheckoutCommand());
        register(new ResetCommand());
        register(new DiffCommand(new LineDiffEngine()));
        register(helpCommand);
    }

    public void register(VcsCommand command) {
        commands.put(command.getName(), command);
    }

    public void execute(Repository repo, String[] args) {
        if (args == null || args.length == 0) {
            throw new VcsException("No command provided. Run 'help' for usage.");
        }
        VcsCommand command = commands.get(args[0]);
        if (command == null) {
            throw new VcsException("Unknown command: '" + args[0] + "'. Run 'help' for usage.");
        }
        command.execute(repo, args);
    }

    public Map<String, VcsCommand> getCommands() {
        return Collections.unmodifiableMap(commands);
    }
}
