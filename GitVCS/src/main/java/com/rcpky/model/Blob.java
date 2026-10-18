package com.rcpky.model;

import com.rcpky.util.VcsUtils;

public class Blob {
    private final String hash;
    private final String content;

    public Blob(String content) {
        this.content = content;
        this.hash = VcsUtils.sha256("blob:" + content);
    }

    public String getHash() { return hash; }
    public String getContent() { return content; }

    @Override
    public String toString() {
        return "Blob{hash='" + hash.substring(0, 7) + "', length=" + content.length() + "}";
    }
}
