package edu.hm.hafner.grading;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Percentage;
import edu.hm.hafner.coverage.Rate;
import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.coverage.TestCase.TestResult;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.Generated;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Computes the {@link Score} impact of test results. These results are obtained by evaluating the
 * number of passed, failed, or skipped tests.
 *
 * @author Eva-Maria Zeintl
 * @author Jannik Ohme
 * @author Ullrich Hafner
 */
public final class TestScore extends Score<TestScore, TestConfiguration> {
    @Serial
    private static final long serialVersionUID = 3L;
    private static final int CAPACITY = 1024;
    private static final double ALMOST_PERFECT = 99.99;

    private final int passedSize;
    private final int failedSize;
    private final int skippedSize;

    private int passedSizeDelta;
    private int failedSizeDelta;
    private int skippedSizeDelta;

    private transient Node report; // do not persist the tree of nodes

    private TestScore(final String name, final String icon, final Scope scope, final TestConfiguration configuration,
            final List<TestScore> scores) {
        super(name, icon, scope, configuration, scores);

        this.passedSize = sum(scores, TestScore::getPassedSize);
        this.failedSize = sum(scores, TestScore::getFailedSize);
        this.skippedSize = sum(scores, TestScore::getSkippedSize);

        this.passedSizeDelta = sum(scores, TestScore::getPassedSizeDelta);
        this.failedSizeDelta = sum(scores, TestScore::getFailedSizeDelta);
        this.skippedSizeDelta = sum(scores, TestScore::getSkippedSizeDelta);

        this.report = new ContainerNode(name);

        scores.stream().map(TestScore::getReport).forEach(report::addChild);
    }

    private TestScore(final String name, final String icon, final Scope scope, final TestConfiguration configuration, final Node report, final boolean hasDelta) {
        super(name, icon, scope, configuration, hasDelta);

        passedSize = sum(report, TestResult.PASSED);
        failedSize = sum(report, TestResult.FAILED);
        skippedSize = sum(report, TestResult.SKIPPED);

        this.report = report;
    }

    private TestScore(final String name, final String icon, final Scope scope, final TestConfiguration configuration, final Node report) {
        this(name, icon, scope, configuration, report, false);
    }

    private TestScore(final String name, final String icon, final Scope scope, final TestConfiguration configuration, final Node report, final Node deltaReport) {
        this(name, icon, scope, configuration, report, true);

        passedSizeDelta = passedSize - sum(deltaReport, TestResult.PASSED);
        failedSizeDelta = failedSize - sum(deltaReport, TestResult.FAILED);
        skippedSizeDelta = skippedSize - sum(deltaReport, TestResult.SKIPPED);
    }

    /**
     * Restore an empty report after deserialization.
     *
     * @return this
     */
    @Serial @CanIgnoreReturnValue
    private Object readResolve() {
        report = new ModuleNode("empty");

        return this;
    }

    private int sum(final List<TestScore> scores, final Function<TestScore, Integer> property) {
        return scores.stream().map(property).reduce(Integer::sum).orElse(0);
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

        change = change + scale(configuration.getSuccessRateImpact(), getSuccessRate());
        change = change + scale(configuration.getFailureRateImpact(), getFailureRate());

        return change;
    }

    /**
     * Returns the success rate of the tests.
     *
     * @return the success rate, i.e., the number of passed tests in percent with respect to the total number of tests
     */
    public double getSuccessRate() {
        if (getTotalSize() - getSkippedSize() == 0) {
            return 100.00; // if there are no executed tests, then the success rate is 100% by definition (since there are no failed tests)
        }
        var rate = Percentage.valueOf(getPassedSize(), getTotalSize() - getSkippedSize()).toRounded();
        if (rate == 100.00 && getFailedSize() > 0) {
            return ALMOST_PERFECT; // 100% success rate is only possible if there are no failed tests
        }
        return rate;
    }

    /**
     * Returns the delta success rate of the tests.
     *
     * @return the success rate, i.e., the number of changed passed tests in percent with respect to the total number of tests
     */
    public double getSuccessRateDelta() {
        var rate = getDeltaRateOf(getPassedSize() - getPassedSizeDelta());
        if (rate == 100 && failedSize - failedSizeDelta > 0) {
            return 99; // 100% success rate is only possible if there are no failed tests
        }
        return getSuccessRate() - rate;
    }

    /**
     * Returns the success rate of the tests.
     *
     * @return the success rate, i.e., the number of passed tests in percent with respect to the total number of executed tests
     */
    public Value getSuccessPercentage() {
        return getReport().getValue(Metric.TEST_SUCCESS_RATE).orElse(Rate.nullObject(Metric.TEST_SUCCESS_RATE));
    }

    public double getFailureRate() {
        return getRateOf(getFailedSize());
    }

    private int getRateOf(final int achieved) {
        return Math.toIntExact(Math.round(achieved * 100.0 / (getTotalSize() - getSkippedSize())));
    }

    private int getDeltaRateOf(final int achieved) {
        return Math.toIntExact(Math.round(achieved * 100.0 / (getTotalSize() - getTotalSizeDelta() - (getSkippedSize() - getSkippedSizeDelta()))));
    }

    public int getPassedSize() {
        return passedSize;
    }

    public int getPassedSizeDelta() {
        return passedSizeDelta;
    }

    /**
     * Returns whether this score has any passed tests.
     *
     * @return {@code true} if this score has passed tests, {@code false} otherwise
     */
    public boolean hasPassedTests() {
        return getPassedSize() > 0;
    }

    public int getExecutedSize() {
        return passedSize + failedSize;
    }

    public int getTotalSize() {
        return passedSize + failedSize + skippedSize;
    }

    public int getTotalSizeDelta() {
        return passedSizeDelta + failedSizeDelta + skippedSizeDelta;
    }

    public int getFailedSize() {
        return failedSize;
    }

    public int getFailedSizeDelta() {
        return failedSizeDelta;
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

    public int getSkippedSizeDelta() {
        return skippedSizeDelta;
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
        if (!hasTests()) {
            return "No test results available";
        }
        var summary = new StringBuilder(CAPACITY);
        summary.append(format("%s successful", getSuccessPercentage().asText(Locale.ENGLISH)));
        var joiner = new StringJoiner(", ", " (", ")");
        if (hasFailures()) {
            joiner.add(format("%s failed", ScoreMarkdown.formatDelta(getFailedSize(), getFailedSizeDelta())));
        }
        if (hasPassedTests()) {
            joiner.add(format("%s passed", ScoreMarkdown.formatDelta(getPassedSize(), getPassedSizeDelta())));
        }
        if (hasSkippedTests()) {
            joiner.add(format("%s skipped", ScoreMarkdown.formatDelta(getSkippedSize(), getSkippedSizeDelta())));
        }
        summary.append(joiner);
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
    static class TestScoreBuilder extends ScoreBuilder<TestScore, TestConfiguration> {
        TestScoreBuilder() {
            this(Optional.empty());
        }

        TestScoreBuilder(final Optional<Path> deltaReports) {
            super(deltaReports);
        }

        @Override
        TestScore aggregate(final List<TestScore> scores) {
            return new TestScore(getTopLevelName(), getIcon(), getScope(), getConfiguration(), scores);
        }

        @Override
        TestScore build() {
            if (hasDelta()) {
                return new TestScore(getName(), getIcon(), getScope(), getConfiguration(), getNode(), getDeltaNode());
            }
            return new TestScore(getName(), getIcon(), getScope(), getConfiguration(), getNode());
        }

        @Override
        void read(final ToolParser factory, final ToolConfiguration tool, final FilteredLog log) {
            readNode(factory, tool, log);
        }

        @Override
        String getType() {
            return "test";
        }

        @Override
        String getDefaultTopLevelName() {
            return "Test Results";
        }
    }
}
