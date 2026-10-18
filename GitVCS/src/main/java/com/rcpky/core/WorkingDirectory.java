package com.rcpky.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorkingDirectory {
    private final Map<String, String> files = new LinkedHashMap<>(); // filename -> content

    public void addFile(String filename, String content) {
        files.put(filename, content);
    }

    public void removeFile(String filename) {
        files.remove(filename);
    }

    public boolean hasFile(String filename) {
        return files.containsKey(filename);
    }

    public String getContent(String filename) {
        return files.get(filename);
    }

    public Map<String, String> getFiles() {
        return Collections.unmodifiableMap(files);
    }

    public void setFiles(Map<String, String> snapshot) {
        files.clear();
        files.putAll(snapshot);
    }

    public void clear() {
        files.clear();
    }
}
