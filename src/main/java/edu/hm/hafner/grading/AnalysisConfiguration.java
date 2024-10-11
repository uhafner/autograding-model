package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.hm.hafner.util.Generated;

/**
 * Configuration to grade static analysis results. The configuration specifies the impact of the static analysis results
 * on the score. This class is intended to be deserialized from JSON, there is no public constructor available.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("unused")
public final class AnalysisConfiguration extends Configuration {
    @Serial
    private static final long serialVersionUID = 3L;

    private static final String ANALYSIS_ID = "analysis";

    /**
     * Converts the specified JSON object to a list of {@link AnalysisConfiguration} instances.
     *
     * @param json
     *         the JSON object to convert
     *
     * @return the corresponding {@link AnalysisConfiguration} instances
     */
    public static List<AnalysisConfiguration> from(final String json) {
        return extractConfigurations(json, ANALYSIS_ID, AnalysisConfiguration.class);
    }

    private int errorImpact;
    private int highImpact;
    private int normalImpact;
    private int lowImpact;

    private AnalysisConfiguration() {
        super(); // Instances are created via JSON deserialization
    }

    @Override
    protected String getDefaultId() {
        return ANALYSIS_ID;
    }

    @Override
    protected String getDefaultName() {
        return "Static Analysis Warnings";
    }

    @Override
    @JsonIgnore
    public boolean isPositive() {
        return errorImpact >= 0 && highImpact >= 0 && normalImpact >= 0 && lowImpact >= 0;
    }

    @Override
    @JsonIgnore
    public boolean hasImpact() {
        return errorImpact != 0 || highImpact != 0 || normalImpact != 0 || lowImpact != 0;
    }

    public int getErrorImpact() {
        return errorImpact;
    }

    public int getHighImpact() {
        return highImpact;
    }

    public int getNormalImpact() {
        return normalImpact;
    }

    public int getLowImpact() {
        return lowImpact;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        var that = (AnalysisConfiguration) o;
        return errorImpact == that.errorImpact
                && highImpact == that.highImpact
                && normalImpact == that.normalImpact
                && lowImpact == that.lowImpact;
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), errorImpact, highImpact, normalImpact, lowImpact);
    }
}
