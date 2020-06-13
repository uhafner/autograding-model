package edu.hm.hafner.grading;

import java.util.List;

import edu.hm.hafner.util.FilteredLog;

/**
 * A generic supplier for gradding results.
 *
 * @author Ullrich Hafner
 */
public abstract class Supplier<C extends Configuration, S extends Score> {
    /**
     * Returns the autograding scores using the specified configuration.
     *
     * @param configuration
     *         the grading configuration to use
     *
     * @return the created scores
     */
    protected abstract List<S> createScores(C configuration);

    final void log(final List<S> scores, final FilteredLog log) {
        for (S score : scores) {
            logScore(score, log);
        }
    }

    protected abstract void logScore(S score, FilteredLog log);
}
