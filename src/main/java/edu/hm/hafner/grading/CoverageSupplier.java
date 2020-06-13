package edu.hm.hafner.grading;

import edu.hm.hafner.util.FilteredLog;

/**
 * Provides code coverage grading scores.
 *
 * @author Ullrich Hafner
 */
public abstract class CoverageSupplier extends Supplier<CoverageConfiguration, CoverageScore> {
    @Override
    protected void logScore(final CoverageScore score, final FilteredLog log) {
        log.logInfo("-> %s score: %d (covered: %d%%, missed: %d%%)",
                score.getName(), score.getTotalImpact(), score.getCoveredPercentage(), score.getMissedPercentage());
    }
}
