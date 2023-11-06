package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.hm.hafner.coverage.Metric;

/**
 * Configuration to grade code coverage results.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"PMD.DataClass", "HashCodeToString"})
public final class CoverageConfiguration extends Configuration {
    @Serial
    private static final long serialVersionUID = 3L;
    private static final String COVERAGE_ID = "coverage";

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

    @SuppressWarnings("unused") // Json property
    private Metric metric;

    private CoverageConfiguration() {
        super(); // Instances are created via JSON deserialization
    }

    @Override
    protected String getDefaultId() {
        return COVERAGE_ID;
    }

    @Override
    protected String getDefaultName() {
        return "Code Coverage";
    }

    @Override @JsonIgnore
    public boolean isPositive() {
        return coveredPercentageImpact >= 0 && missedPercentageImpact >= 0;
    }

    public int getCoveredPercentageImpact() {
        return coveredPercentageImpact;
    }

    public int getMissedPercentageImpact() {
        return missedPercentageImpact;
    }

    public Metric getMetric() {
        return metric;
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
        if (missedPercentageImpact != that.missedPercentageImpact) {
            return false;
        }
        return metric.equals(that.metric);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + coveredPercentageImpact;
        result = 31 * result + missedPercentageImpact;
        result = 31 * result + metric.hashCode();
        return result;
    }
}
