package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.coverage.TestCase.TestResult;
import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.Generated;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Computes the {@link Score} impact of test results. These results are obtained by evaluating the
 * number of passed, failed or skipped tests.
 *
 * @author Eva-Maria Zeintl
 */
@SuppressWarnings("PMD.DataClass")
public final class TestScore extends Score<TestScore, TestConfiguration> {
    @Serial
    private static final long serialVersionUID = 3L;
    private static final int CAPACITY = 1024;

    private final int passedSize;
    private final int failedSize;
    private final int skippedSize;

    private transient Node report; // do not persist the tree of nodes

    private TestScore(final String id, final String name, final String icon, final TestConfiguration configuration,
            final List<TestScore> scores) {
        super(id, name, icon, configuration, scores.toArray(new TestScore[0]));

        this.failedSize = aggregate(scores, TestScore::getFailedSize);
        this.skippedSize = aggregate(scores, TestScore::getSkippedSize);
        this.passedSize = aggregate(scores, TestScore::getPassedSize);

        this.report = new ContainerNode(name);
        scores.stream().map(TestScore::getReport).forEach(report::addChild);
    }

    private TestScore(final String id, final String name, final String icon, final TestConfiguration configuration, final Node report) {
        super(id, name, icon, configuration);

        this.report = report;

        passedSize = sum(report, TestResult.PASSED);
        failedSize = sum(report, TestResult.FAILED);
        skippedSize = sum(report, TestResult.SKIPPED);
    }

    /**
     * Restore an empty report after de-serialization.
     *
     * @return this
     */
    @Serial @CanIgnoreReturnValue
    private Object readResolve() {
        report = new ModuleNode("empty");

        return this;
    }

    private int aggregate(final List<TestScore> scores, final Function<TestScore, Integer> property) {
        return scores.stream().reduce(0, (sum, score) -> sum + property.apply(score), Integer::sum);
    }

    private int sum(final Node testReport, final TestResult testResult) {
        return testReport.getTestCases().stream()
                .map(TestCase::getResult)
                .filter(status -> status == testResult)
                .mapToInt(i -> 1)
                .sum();
    }

    @JsonIgnore
    public Node getReport() {
        return report;
    }

    public int getReportFiles() {
        return report.getAll(Metric.MODULE).size();
    }

    @Override
    public int getImpact() {
        var configuration = getConfiguration();

        int change = 0;

        if (configuration.isAbsolute()) {
            change = change + configuration.getPassedImpact() * getPassedSize();
            change = change + configuration.getFailureImpact() * getFailedSize();
            change = change + configuration.getSkippedImpact() * getSkippedSize();
        }
        else {
            change = change + scale(configuration.getSuccessRateImpact(), getSuccessRate());
            change = change + scale(configuration.getFailureRateImpact(), getFailureRate());
        }
        return change;
    }

    /**
     * Returns the success rate of the tests.
     *
     * @return the success rate, i.e., the number of passed tests in percent with respect to the total number of tests
     */
    public int getSuccessRate() {
        var rate = getRateOf(getPassedSize());
        if (rate == 100 && getFailedSize() > 0) {
            return 99; // 100% success rate is only possible if there are no failed tests
        }
        return rate;
    }

    public int getFailureRate() {
        return getRateOf(getFailedSize());
    }

    private int getRateOf(final int achieved) {
        return Math.toIntExact(Math.round(achieved * 100.0 / getTotalSize()));
    }

    public int getPassedSize() {
        return passedSize;
    }

    /**
     * Returns whether this score has any passed tests.
     *
     * @return {@code true} if this score has passed tests, {@code false} otherwise
     */
    public boolean hasPassedTests() {
        return getPassedSize() > 0;
    }

    public int getTotalSize() {
        return passedSize + failedSize + skippedSize;
    }

    public int getFailedSize() {
        return failedSize;
    }

    /**
     * Returns whether this score has any test failures.
     *
     * @return {@code true} if this score has any test failures, {@code false} otherwise
     */
    public boolean hasFailures() {
        return getFailedSize() > 0;
    }

    public int getSkippedSize() {
        return skippedSize;
    }

    /**
     * Returns whether this score has any skipped tests.
     *
     * @return {@code true} if this score has any skipped tests, {@code false} otherwise
     */
    public boolean hasSkippedTests() {
        return getSkippedSize() > 0;
    }

    /**
     * Returns the list of failed test cases.
     *
     * @return the failed test cases
     */
    public List<TestCase> getFailures() {
        return filterTests(TestResult.FAILED);
    }

    /**
     * Returns the list of skipped test cases.
     *
     * @return the skipped test cases
     */
    public List<TestCase> getSkippedTests() {
        return filterTests(TestResult.SKIPPED);
    }

    /**
     * Returns whether this score has any test results.
     *
     * @return {@code true} if this score has any test results, {@code false} otherwise
     */
    public boolean hasTests() {
        return hasSkippedTests() || hasFailures() || hasPassedTests();
    }

    private List<TestCase> filterTests(final TestResult result) {
        return getReport().getTestCases().stream()
                .filter(testCase -> testCase.getResult() == result).collect(Collectors.toList());
    }

    @Override
    protected String createSummary() {
        var summary = new StringBuilder(CAPACITY);
        if (hasFailures()) {
            summary.append(format("%d tests failed, %d passed", getFailedSize(), getPassedSize()));
        }
        else {
            summary.append(format("%d tests passed", getPassedSize()));
        }
        if (getSkippedSize() > 0) {
            summary.append(format(", %d skipped", getSkippedSize()));
        }
        return summary.toString();
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
        var testScore = (TestScore) o;
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
        @CheckForNull
        private String id;
        @CheckForNull
        private String name;
        private String icon = StringUtils.EMPTY;
        @CheckForNull
        private TestConfiguration configuration;

        private final List<TestScore> scores = new ArrayList<>();
        @CheckForNull
        private Node report;

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
            return StringUtils.defaultIfBlank(id, getConfiguration().getId());
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
            return StringUtils.defaultIfBlank(name, getConfiguration().getName());
        }

        /**
         * Sets the icon of the test score.
         *
         * @param icon
         *         the icon to show
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public TestScoreBuilder withIcon(final String icon) {
            this.icon = icon;
            return this;
        }

        private String getIcon() {
            return StringUtils.defaultString(icon);
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

        private TestConfiguration getConfiguration() {
            return Objects.requireNonNull(configuration);
        }

        /**
         * Sets the test report for this score.
         *
         * @param rootNode
         *         the root of the tree with the test cases
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public TestScoreBuilder withReport(final Node rootNode) {
            this.report = rootNode;
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
            Ensure.that(report != null ^ !scores.isEmpty()).isTrue(
                    "You must either specify test results or provide a list of sub-scores.");

            if (scores.isEmpty() && report != null) {
                return new TestScore(getId(), getName(), getIcon(), getConfiguration(), report);
            }
            else {
                return new TestScore(getId(), getName(), getIcon(), getConfiguration(), scores);
            }
        }
    }
}
