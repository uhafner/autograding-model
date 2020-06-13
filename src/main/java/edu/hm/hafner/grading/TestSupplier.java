package edu.hm.hafner.grading;

import edu.hm.hafner.util.FilteredLog;

/**
 * Provides code coverage grading scores.
 *
 * @author Ullrich Hafner
 */
public abstract class TestSupplier extends Supplier<TestConfiguration, TestScore> {
    @Override
    protected void logScore(final TestScore score, final FilteredLog log) {
        log.logInfo("-> %s score: %d (total: %d, passed: %d, failed: %d, skipped: %d)",
                score.getName(), score.getTotalImpact(),
                score.getTotalSize(), score.getPassedSize(),
                score.getFailedSize(), score.getSkippedSize());
    }
}
