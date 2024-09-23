package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

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
     *         the json object to convert
     *
     * @return the corresponding {@link TestConfiguration} instances
     */
    public static List<TestConfiguration> from(final String json) {
        return extractConfigurations(json, TEST_ID, TestConfiguration.class);
    }

    private int failureImpact;
    private int passedImpact;
    private int skippedImpact;

    private TestConfiguration() {
        super(); // Instances are created via JSON deserialization
    }

    @Override
    protected String getDefaultId() {
        return TEST_ID;
    }

    @Override
    protected String getDefaultName() {
        return "Tests";
    }

    @Override
    public boolean isPositive() {
        return passedImpact >= 0 && failureImpact >= 0 && skippedImpact >= 0;
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
                && skippedImpact == that.skippedImpact;
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), failureImpact, passedImpact, skippedImpact);
    }
}
