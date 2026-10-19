package com.rcpky;

/**
 * Unchecked exception for search-engine domain errors.
 *
 * Why RuntimeException (unchecked) instead of a checked Exception?
 *   Checked exceptions force every caller to handle or re-declare them, which
 *   pollutes method signatures all the way up the call stack. Search errors
 *   (bad input, missing document) are programming mistakes or data issues —
 *   callers that care can catch SearchException explicitly; those that don't
 *   are not forced to write boilerplate try/catch blocks.
 *
 * The two-argument constructor (message + cause) follows the standard
 * exception-chaining pattern: wrap a lower-level exception without losing
 * the original stack trace.
 */
public class SearchException extends RuntimeException {
    public SearchException(String message)                  { super(message); }
    public SearchException(String message, Throwable cause) { super(message, cause); }
}
