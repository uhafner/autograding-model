package edu.hm.hafner.grading;

import edu.hm.hafner.util.FilteredLog;

/**
 * Provides PIT mutation coverage grading scores.
 *
 * @author Ullrich Hafner
 */
public abstract class PitSupplier extends Supplier<PitConfiguration, PitScore> {
    @Override
    protected void logScore(final PitScore score, final FilteredLog log) {
        log.logInfo("-> PIT mutation score: %d (mutations: %d, detected: %d%%, undetected: %d%%)",
                score.getTotalImpact(), score.getMutationsSize(),
                score.getDetectedPercentage(), score.getUndetectedPercentage());
    }
}
