package edu.hm.hafner.grading;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.Generated;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

/**
 * Configuration to grade code coverage results. The configuration specifies the impact of the coverage results on the
 * score. This class is intended to be deserialized from JSON, there is no public constructor available.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("HashCodeToString")
public final class CoverageConfiguration extends Configuration {
    @Serial
    private static final long serialVersionUID = 3L;

    private static final String COVERAGE_ID = "coverage";
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

    @SuppressFBWarnings("UWF_UNWRITTEN_FIELD") // Initialized via JSON
    private int coveredPercentageImpact;
    @SuppressFBWarnings("UWF_UNWRITTEN_FIELD") // Initialized via JSON
    private int missedPercentageImpact;

    private CoverageConfiguration() {
        super(); // Instances are created via JSON deserialization
    }

    @Override
    protected void validate(final ToolConfiguration tool) {
        Ensure.that(tool.getId()).isNotEmpty("%s: %s%n%s", tool.getName(),
                "No tool ID specified: the ID of a tool is used to identify the parser and must not be empty.",
                tool);
        Ensure.that(tool.getPattern()).isNotEmpty("%s: %s%n%s", tool.getName(),
                "No pattern specified: the pattern is used to select the report files to parse and must not be empty.",
                tool);
        Ensure.that(tool.getMetric()).isNotEmpty("%s: %s%n%s", tool.getName(),
                "No metric specified: for each tool a specific coverage metric must be specified.",
                tool);
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
