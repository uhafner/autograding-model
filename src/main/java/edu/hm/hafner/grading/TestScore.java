package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.Generated;

/**
 * Computes the {@link Score} impact of test results. These results are obtained by evaluating the
 * number of passed, failed or skipped tests.
 *
 * @author Eva-Maria Zeintl
 */
@SuppressWarnings("PMD.DataClass")
public class TestScore extends Score<TestScore, TestConfiguration> {
    @Serial
    private static final long serialVersionUID = 3L;

    private final int passedSize;
    private final int failedSize;
    private final int skippedSize;

    private TestScore(final String id, final String name, final TestConfiguration configuration,
            final List<TestScore> scores) {
        super(id, name, configuration, scores.toArray(new TestScore[0]));

        this.failedSize = scores.stream().reduce(0, (sum, score) -> sum + score.getFailedSize(), Integer::sum);
        this.skippedSize = scores.stream().reduce(0, (sum, score) -> sum + score.getSkippedSize(), Integer::sum);
        this.passedSize = scores.stream().reduce(0, (sum, score) -> sum + score.getPassedSize(), Integer::sum);
    }

    private TestScore(final String id, final String name, final TestConfiguration configuration,
                     final int totalSize, final int failedSize, final int skippedSize) {
        super(id, name, configuration);

        this.failedSize = failedSize;
        this.skippedSize = skippedSize;
        this.passedSize = totalSize - this.failedSize - this.skippedSize;
    }

    @Override
    public int getImpact() {
        TestConfiguration configuration = getConfiguration();

        int change = 0;

        change = change + configuration.getPassedImpact() * getPassedSize();
        change = change + configuration.getFailureImpact() * getFailedSize();
        change = change + configuration.getSkippedImpact() * getSkippedSize();

        return change;
    }

    public final int getPassedSize() {
        return passedSize;
    }

    public final int getTotalSize() {
        return passedSize + failedSize + skippedSize;
    }

    public final int getFailedSize() {
        return failedSize;
    }

    public final int getSkippedSize() {
        return skippedSize;
    }

    @Override @Generated
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
        TestScore testScore = (TestScore) o;
        return passedSize == testScore.passedSize
                && failedSize == testScore.failedSize
                && skippedSize == testScore.skippedSize;
    }

    @Override @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), passedSize, failedSize, skippedSize);
    }

    /**
     * A builder for {@link TestScore} instances.
     */
    @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    public static class TestScoreBuilder {
        private String id;
        private String name;
        private TestConfiguration configuration;

        private final List<TestScore> scores = new ArrayList<>();
        private int totalSize;
        private int failedSize;
        private int skippedSize;
        private boolean hasTotals;

        /**
         * Sets the ID of the analysis score.
         *
         * @param id
         *         the ID
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public TestScoreBuilder withId(final String id) {
            this.id = id;
            return this;
        }

        private String getId() {
            return StringUtils.defaultIfBlank(id, configuration.getId());
        }

        /**
         * Sets the human-readable name of the analysis score.
         *
         * @param name
         *         the name to show
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public TestScoreBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        private String getName() {
            return StringUtils.defaultIfBlank(name, configuration.getName());
        }

        /**
         * Sets the grading configuration.
         *
         * @param configuration
         *         the grading configuration
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public TestScoreBuilder withConfiguration(final TestConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Sets the total number of tests.
         *
         * @param totalSize total number of tests
         * @return this
         */
        @CanIgnoreReturnValue
        public TestScoreBuilder withTotalSize(final int totalSize) {
            hasTotals = true;
            this.totalSize = totalSize;
            return this;
        }

        /**
         * Sets the total number of failed tests.
         *
         * @param failedSize total number of failed tests
         * @return this
         */

        @CanIgnoreReturnValue
        public TestScoreBuilder withFailedSize(final int failedSize) {
            hasTotals = true;
            this.failedSize = failedSize;
            return this;
        }

        /**
         * Sets the total number of skipped tests.
         *
         * @param skippedSize total number of skipped tests
         * @return this
         */
        @CanIgnoreReturnValue
        public TestScoreBuilder withSkippedSize(final int skippedSize) {
            hasTotals = true;
            this.skippedSize = skippedSize;
            return this;
        }

        /**
         * Sets the scores that should be aggregated by this score.
         *
         * @param scores
         *         the scores to aggregate
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public TestScoreBuilder withScores(final List<TestScore> scores) {
            Ensure.that(scores).isNotEmpty("You cannot add an empty list of scores.");
            this.scores.clear();
            this.scores.addAll(scores);
            return this;
        }

        /**
         * Builds the {@link TestScore} instance with the configured values.
         *
         * @return the new instance
         */
        public TestScore build() {
            Ensure.that(hasTotals ^ !scores.isEmpty()).isTrue(
                    "You must either specify test results or provide a list of sub-scores.");

            if (scores.isEmpty()) {
                return new TestScore(getId(), getName(), configuration, totalSize, failedSize, skippedSize);
            }
            else {
                return new TestScore(getId(), getName(), configuration, scores);
            }

        }
    }
}
