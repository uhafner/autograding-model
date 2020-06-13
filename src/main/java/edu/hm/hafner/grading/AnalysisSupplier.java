package edu.hm.hafner.grading;

import edu.hm.hafner.util.FilteredLog;

/**
 * Provides static analysis grading scores.
 *
 * @author Ullrich Hafner
 */
public abstract class AnalysisSupplier extends Supplier<AnalysisConfiguration, AnalysisScore> {
    @Override
    protected void logScore(final AnalysisScore score, final FilteredLog log) {
        log.logInfo("-> %s score: %d (errors:%d, high:%d, normal:%d, low:%d)",
                score.getName(), score.getTotalImpact(),
                score.getErrorsSize(), score.getHighSeveritySize(),
                score.getNormalSeveritySize(), score.getLowSeveritySize());
    }
}
