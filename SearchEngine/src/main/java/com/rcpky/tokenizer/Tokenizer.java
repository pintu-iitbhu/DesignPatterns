package com.rcpky.tokenizer;

import java.util.Set;

/**
 * Strategy interface for text tokenization.
 *
 * Why a strategy?
 *   Different use-cases need different tokenization rules:
 *     - SimpleTokenizer  : split on non-alphanumeric characters, lowercase
 *     - StopWordTokenizer: additionally remove common words ("the", "is", …)
 *     - StemmingTokenizer: reduce words to their root form ("running" → "run")
 *     - NGramTokenizer   : emit character n-grams for fuzzy / partial matching
 *
 *   By hiding tokenization behind this interface, SearchEngine never needs to
 *   change when a new tokenization strategy is introduced — only the Builder
 *   call-site changes (Open/Closed Principle).
 *
 *   Both indexing and querying MUST use the same Tokenizer instance so that
 *   token space is consistent (a query token can only match an indexed token
 *   if both went through identical normalization).
 */
public interface Tokenizer {

    /**
     * Converts raw text into a set of normalized, searchable tokens.
     *
     * @param text raw input (document content or query string)
     * @return immutable set of tokens; empty set for null / blank input
     */
    Set<String> tokenize(String text);
}
