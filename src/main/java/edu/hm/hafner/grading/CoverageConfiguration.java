package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.hm.hafner.util.Generated;

/**
 * Configuration to grade code coverage results. The configuration specifies the impact of the coverage results on the
 * score. This class is intended to be deserialized from JSON, there is no public constructor available.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"PMD.DataClass", "HashCodeToString"})
public final class CoverageConfiguration extends Configuration {
    @Serial
    private static final long serialVersionUID = 3L;

    private static final String COVERAGE_ID = "coverage";
    private static final String[] MUTATION_IDS = {"pitest", "mutation", "pit"};
    static final String CODE_COVERAGE = "Code Coverage";

    /**
     * Converts the specified JSON object to a list of {@link CoverageConfiguration} instances.
     *
     * @param json
     *         the JSON object to convert
     *
     * @return the corresponding {@link CoverageConfiguration} instances
     */
    public static List<CoverageConfiguration> from(final String json) {
        return extractConfigurations(json, COVERAGE_ID, CoverageConfiguration.class);
    }

    @SuppressWarnings("unused") // JSON property
    private int coveredPercentageImpact;

    @SuppressWarnings("unused") // JSON property
    private int missedPercentageImpact;

    private CoverageConfiguration() {
        super(); // Instances are created via JSON deserialization
    }

    @Override
    protected String getDefaultId() {
        return COVERAGE_ID;
    }

    @Override
    protected String getDefaultName() {
        return CODE_COVERAGE;
    }

    @Override
    @JsonIgnore
    public boolean isPositive() {
        return coveredPercentageImpact >= 0 && missedPercentageImpact >= 0;
    }

    @Override
    @JsonIgnore
    public boolean hasImpact() {
        return coveredPercentageImpact != 0 || missedPercentageImpact != 0;
    }

    public int getCoveredPercentageImpact() {
        return coveredPercentageImpact;
    }

    public int getMissedPercentageImpact() {
        return missedPercentageImpact;
    }

    /**
     * Determines whether the specified ID or name are related to mutation coverage or to code coverage.
     *
     * @return {@code true} if this configuration is for mutation coverage, {@code false} if this configuration is for
     *         code coverage
     */
    public boolean isMutationCoverage() {
        return StringUtils.containsAnyIgnoreCase(getId() + getName(), MUTATION_IDS);
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
        var that = (CoverageConfiguration) o;
        return coveredPercentageImpact == that.coveredPercentageImpact
                && missedPercentageImpact == that.missedPercentageImpact;
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), coveredPercentageImpact, missedPercentageImpact);
    }
}
