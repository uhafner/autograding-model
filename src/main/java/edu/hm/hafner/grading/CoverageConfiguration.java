package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    /**
     * Converts the specified JSON object to a list of {@link CoverageConfiguration} instances.
     *
     * @param json
     *         the json object to convert
     *
     * @return the corresponding {@link CoverageConfiguration} instances
     */
    public static List<CoverageConfiguration> from(final String json) {
        return extractConfigurations(json, COVERAGE_ID, CoverageConfiguration.class);
    }

    @SuppressWarnings("unused") // Json property
    private int coveredPercentageImpact;

    @SuppressWarnings("unused") // Json property
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
        if (StringUtils.containsAnyIgnoreCase(getId(), MUTATION_IDS)) {
            return "Mutation Coverage";
        }
        return "Code Coverage";
    }

    @Override
    @JsonIgnore
    public boolean isPositive() {
        return coveredPercentageImpact >= 0 && missedPercentageImpact >= 0;
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

        CoverageConfiguration that = (CoverageConfiguration) o;

        if (coveredPercentageImpact != that.coveredPercentageImpact) {
            return false;
        }
        return missedPercentageImpact == that.missedPercentageImpact;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + coveredPercentageImpact;
        result = 31 * result + missedPercentageImpact;
        return result;
    }
}
