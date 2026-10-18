package com.rcpky.command;

import com.rcpky.core.ObjectStore;
import com.rcpky.core.Repository;
import com.rcpky.core.StagingArea;
import com.rcpky.core.WorkingDirectory;
import com.rcpky.exception.VcsException;
import com.rcpky.model.Blob;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AddCommand implements VcsCommand {

    @Override
    public void execute(Repository repo, String[] args) {
        if (args.length < 2) throw new VcsException("Usage: " + getUsage());

        String patternStr = args[1];
        Pattern regex;
        try {
            regex = Pattern.compile(patternStr);
        } catch (PatternSyntaxException e) {
            throw new VcsException("Invalid regex pattern: " + patternStr);
        }

        WorkingDirectory wd = repo.getWorkingDirectory();
        StagingArea staging = repo.getStagingArea();
        ObjectStore store = repo.getObjectStore();

        int count = 0;
        for (java.util.Map.Entry<String, String> entry : wd.getFiles().entrySet()) {
            if (regex.matcher(entry.getKey()).matches()) {
                Blob blob = new Blob(entry.getValue());
                store.storeBlob(blob);
                staging.stage(entry.getKey(), blob.getHash());
                count++;
            }
        }

        if (count == 0) {
            System.out.println("No files matched pattern '" + patternStr + "'.");
        } else {
            System.out.println("Staged " + count + " file(s) matching '" + patternStr + "'.");
        }
    }

    @Override public String getName() { return "add"; }
    @Override public String getUsage() { return "add <regex>"; }
    @Override public String getDescription() { return "Stage files in the working directory matching the given regex."; }
}
