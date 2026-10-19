package com.rcpky.tokenizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default Tokenizer implementation.
 *
 * Algorithm:
 *   1. Guard: null or blank input → empty set.
 *   2. Lowercase the entire string so that "Java" and "java" map to the same token.
 *   3. Split on the regex "[^a-zA-Z0-9]+" — any run of non-alphanumeric characters
 *      (spaces, hyphens, punctuation, …) acts as a delimiter.
 *   4. Filter empty strings that arise from leading/trailing delimiters.
 *   5. Collect into a HashSet — duplicate tokens are collapsed, giving O(1) lookup
 *      later when building posting lists.
 *
 * Complexity: O(n) in the length of the input string.
 *
 * Limitations (extension points):
 *   - No stop-word removal ("a", "the", "is" are indexed unnecessarily).
 *   - No stemming (searching "run" won't find "running").
 *   Implement a new Tokenizer and pass it to SearchEngine.Builder to address these.
 */
public class SimpleTokenizer implements Tokenizer {

    /**
     * Splits on any non-alphanumeric sequence.
     * Treats hyphens the same as spaces so "data-science" yields tokens {"data","science"}.
     */
    private static final String DELIMITER_REGEX = "[^a-zA-Z0-9]+";

    @Override
    public Set<String> tokenize(String text) {
        // Guard: blank content produces zero tokens; callers should reject it before indexing.
        if (text == null || text.trim().isEmpty()) return Collections.emptySet();

        return Arrays.stream(text.toLowerCase().split(DELIMITER_REGEX))
                .filter(t -> !t.isEmpty())   // remove the empty string from leading delimiters
                .collect(Collectors.toSet()); // HashSet: O(1) contains, collapses duplicates
    }
}
