package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.Generated;

/**
 * Configuration to grade test results. The configuration specifies the impact of the test results on the score. This
 * class is intended to be deserialized from JSON, there is no public constructor available.
 *
 * @author Ullrich Hafner
 */
public final class TestConfiguration extends Configuration {
    @Serial
    private static final long serialVersionUID = 3L;

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

    private int failureImpact;
    private int passedImpact;
    private int skippedImpact;

    private int successRateImpact;
    private int failureRateImpact;

    private TestConfiguration() {
        super(); // Instances are created via JSON deserialization
    }

    @Override
    protected String getDefaultName() {
        return "Tests";
    }

    /**
     * Returns whether this configuration defines relative impacts.
     *
     * @return {@code true} if this configuration defines relative impacts, {@code false} if it defines absolute impacts
     */
    @JsonIgnore
    public boolean isRelative() {
        return getFailureRateImpact() != 0 || getSuccessRateImpact() != 0;
    }

    /**
     * Returns whether this configuration defines absolute impacts.
     *
     * @return {@code true} if this configuration defines absolute impacts, {@code false} if it defines relative impacts
     */
    @JsonIgnore
    public boolean isAbsolute() {
        return getPassedImpact() != 0 || getFailureImpact() != 0 || getSkippedImpact() != 0;
    }

    @Override
    @JsonIgnore
    public boolean isPositive() {
        return getPassedImpact() >= 0 && getFailureImpact() >= 0 && getSkippedImpact() >= 0;
    }

    @Override
    @JsonIgnore
    protected boolean hasImpact() {
        return isRelative() || isAbsolute();
    }

    public int getSkippedImpact() {
        return skippedImpact;
    }

    public int getFailureImpact() {
        return failureImpact;
    }

    public int getPassedImpact() {
        return passedImpact;
    }

    public int getSuccessRateImpact() {
        return successRateImpact;
    }

    public int getFailureRateImpact() {
        return failureRateImpact;
    }

    @Override
    protected void validate() {
        Ensure.that(isRelative() && isAbsolute()).isFalse(
                "Test configuration must either define an impact for absolute or relative metrics only.");
    }

    @Override
    protected void validate(final ToolConfiguration tool) {
        Ensure.that(tool.getId()).isNotEmpty(
                createError("No tool ID specified: the IDid of a tool is used to identify the parser and must not be empty.",
                        tool));
        Ensure.that(tool.getPattern()).isNotEmpty(
                createError("No pattern specified: the pattern is used to select the report files to parse and must not be empty.",
                        tool));
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
        var that = (TestConfiguration) o;
        return failureImpact == that.failureImpact
                && passedImpact == that.passedImpact
                && skippedImpact == that.skippedImpact
                && successRateImpact == that.successRateImpact
                && failureRateImpact == that.failureRateImpact;
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), failureImpact, passedImpact, skippedImpact, successRateImpact,
                failureRateImpact);
    }
}
