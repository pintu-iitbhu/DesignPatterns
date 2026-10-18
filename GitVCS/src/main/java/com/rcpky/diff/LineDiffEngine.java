package com.rcpky.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LineDiffEngine implements DiffEngine {

    @Override
    public void diff(Map<String, String> before, Map<String, String> after) {
        Set<String> allFiles = new LinkedHashSet<>();
        allFiles.addAll(before.keySet());
        allFiles.addAll(after.keySet());

        boolean anyDiff = false;
        for (String file : allFiles) {
            String oldContent = before.get(file);
            String newContent = after.get(file);
            if (Objects.equals(oldContent, newContent)) continue;
            anyDiff = true;
            printFileDiff(file, oldContent, newContent);
        }
        if (!anyDiff) {
            System.out.println("(no differences)");
        }
    }

    private void printFileDiff(String file, String oldContent, String newContent) {
        System.out.println("diff --git a/" + file + " b/" + file);
        System.out.println("--- " + (oldContent == null ? "/dev/null" : "a/" + file));
        System.out.println("+++ " + (newContent == null ? "/dev/null" : "b/" + file));

        List<String> oldLines = splitLines(oldContent == null ? "" : oldContent);
        List<String> newLines = splitLines(newContent == null ? "" : newContent);

        List<DiffLine> diffLines = computeDiff(oldLines, newLines);
        printHunks(diffLines, oldLines.size(), newLines.size());
    }

    private void printHunks(List<DiffLine> diffLines, int oldSize, int newSize) {
        if (diffLines.isEmpty()) return;
        System.out.println("@@ -1," + oldSize + " +1," + newSize + " @@");
        for (DiffLine dl : diffLines) {
            System.out.println(dl.prefix + dl.text);
        }
    }

    private List<DiffLine> computeDiff(List<String> oldLines, List<String> newLines) {
        int m = oldLines.size(), n = newLines.size();
        int[][] lcs = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (oldLines.get(i - 1).equals(newLines.get(j - 1))) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i - 1][j], lcs[i][j - 1]);
                }
            }
        }
        List<DiffLine> result = new ArrayList<>();
        backtrack(result, lcs, oldLines, newLines, m, n);
        return result;
    }

    private void backtrack(List<DiffLine> result, int[][] lcs,
                           List<String> oldLines, List<String> newLines, int i, int j) {
        if (i == 0 && j == 0) return;
        if (i > 0 && j > 0 && oldLines.get(i - 1).equals(newLines.get(j - 1))) {
            backtrack(result, lcs, oldLines, newLines, i - 1, j - 1);
            result.add(new DiffLine(" ", oldLines.get(i - 1)));
        } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
            backtrack(result, lcs, oldLines, newLines, i, j - 1);
            result.add(new DiffLine("+", newLines.get(j - 1)));
        } else {
            backtrack(result, lcs, oldLines, newLines, i - 1, j);
            result.add(new DiffLine("-", oldLines.get(i - 1)));
        }
    }

    private List<String> splitLines(String content) {
        if (content.isEmpty()) return new ArrayList<>();
        return Arrays.asList(content.split("\n", -1));
    }

    private static class DiffLine {
        final String prefix;
        final String text;
        DiffLine(String prefix, String text) { this.prefix = prefix; this.text = text; }
    }
}
