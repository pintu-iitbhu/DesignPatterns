package com.rcpky.util;

import com.rcpky.core.ObjectStore;
import com.rcpky.exception.VcsException;
import com.rcpky.model.Blob;
import com.rcpky.model.Tree;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

public class VcsUtils {

    private VcsUtils() {}

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new VcsException("SHA-256 algorithm not available", e);
        }
    }

    public static Map<String, String> reconstructFiles(Tree tree, ObjectStore store) {
        Map<String, String> files = new LinkedHashMap<>();
        if (tree == null) return files;
        for (Map.Entry<String, String> entry : tree.getEntries().entrySet()) {
            Blob blob = store.getBlob(entry.getValue());
            if (blob != null) {
                files.put(entry.getKey(), blob.getContent());
            }
        }
        return files;
    }
}
