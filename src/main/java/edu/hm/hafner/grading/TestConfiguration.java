package edu.hm.hafner.grading;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.Generated;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

/**
 * Configuration to grade test results. The configuration specifies the impact of the test results on the score. This
 * class is intended to be deserialized from JSON, there is no public constructor available.
 *
 * @author Ullrich Hafner
 */
public final class TestConfiguration extends Configuration {
    @Serial
    private static final long serialVersionUID = 15L;

    private static final String TEST_ID = "tests";

    /**
     * Converts the specified JSON object to a list of {@link TestConfiguration} instances.
     *
     * @param json
     *         the JSON object to convert
     *
     * @return the corresponding {@link TestConfiguration} instances
     */
    public static List<TestConfiguration> from(final String json) {
        return extractConfigurations(json, TEST_ID, TestConfiguration.class);
    }

    private int successRateImpact;
    private int failureRateImpact;

    private TestConfiguration() {
        super(); // Instances are created via JSON deserialization
    }

    @Override
    protected String getDefaultName() {
        return "Tests";
    }

    @Override
    public String getDefaultMetric() {
        return "TESTS";
    }

    @Override
    @JsonIgnore
    public boolean isPositive() {
        return getSuccessRateImpact() >= 0 && getFailureRateImpact() >= 0;
    }

    @Override
    @JsonIgnore
    protected boolean hasImpact() {
        return getFailureRateImpact() != 0 || getSuccessRateImpact() != 0;
    }

    public int getSuccessRateImpact() {
        return successRateImpact;
    }

    public int getFailureRateImpact() {
        return failureRateImpact;
    }

    @Override
    protected void validate(final ToolConfiguration tool) {
        Ensure.that(tool.getId()).isNotEmpty("%s: %s%n%s", tool.getName(),
                "No tool ID specified: the IDid of a tool is used to identify the parser and must not be empty.",
                tool);
        Ensure.that(tool.getPattern()).isNotEmpty("%s: %s%n%s", tool.getName(),
                "No pattern specified: the pattern is used to select the report files to parse and must not be empty.",
                tool);
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        var that = (TestConfiguration) o;
        return successRateImpact == that.successRateImpact
                && failureRateImpact == that.failureRateImpact;
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), successRateImpact, failureRateImpact);
    }
}
