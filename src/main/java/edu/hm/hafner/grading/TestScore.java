package edu.hm.hafner.grading;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Rate;
import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.coverage.TestCase.TestResult;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.Generated;

import java.io.Serial;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Computes the {@link Score} impact of test results. These results are obtained by evaluating the
 * number of passed, failed or skipped tests.
 *
 * @author Eva-Maria Zeintl
 * @author Jannik Ohme
 */
public final class TestScore extends Score<TestScore, TestConfiguration> {
    @Serial
    private static final long serialVersionUID = 3L;
    private static final int CAPACITY = 1024;

    private final int passedSize;
    private final int failedSize;
    private final int skippedSize;

    private transient Node report; // do not persist the tree of nodes

    private TestScore(final String name, final String icon, final Scope scope, final TestConfiguration configuration,
            final List<TestScore> scores) {
        super(name, icon, scope, configuration, scores.toArray(new TestScore[0]));

        this.failedSize = aggregate(scores, TestScore::getFailedSize);
        this.skippedSize = aggregate(scores, TestScore::getSkippedSize);
        this.passedSize = aggregate(scores, TestScore::getPassedSize);

        this.report = new ContainerNode(name);
        scores.stream().map(TestScore::getReport).forEach(report::addChild);
    }

    private TestScore(final String name, final String icon, final Scope scope, final TestConfiguration configuration, final Node report) {
        super(name, icon, scope, configuration);

        this.report = report;

        passedSize = sum(report, TestResult.PASSED);
        failedSize = sum(report, TestResult.FAILED);
        skippedSize = sum(report, TestResult.SKIPPED);
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

    /**
     * Returns the success rate of the tests.
     *
     * @return the success rate, i.e., the number of passed tests in percent with respect to the total number of executed tests
     */
    public Value getSuccessPercentage() {
        return getReport().getValue(Metric.TEST_SUCCESS_RATE).orElse(Rate.nullObject(Metric.TEST_SUCCESS_RATE));
    }

    public int getFailureRate() {
        return getRateOf(getFailedSize());
    }

    private int getRateOf(final int achieved) {
        return Math.toIntExact(Math.round(achieved * 100.0 / (getTotalSize() - getSkippedSize())));
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

    public int getExecutedSize() {
        return passedSize + failedSize;
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
        if (!hasTests()) {
            return "No test results available";
        }
        var summary = new StringBuilder(CAPACITY);
        summary.append(format("%s successful", getSuccessPercentage().asText(Locale.ENGLISH)));
        var joiner = new StringJoiner(", ", " (", ")");
        if (hasFailures()) {
            joiner.add(format("%d failed", getFailedSize()));
        }
        if (hasPassedTests()) {
            joiner.add(format("%d passed", getPassedSize()));
        }
        if (hasSkippedTests()) {
            joiner.add(format("%d skipped", getSkippedSize()));
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
        @Override
        public TestScore aggregate(final List<TestScore> scores) {
            return new TestScore(getTopLevelName(), getIcon(), getScope(), getConfiguration(), scores);
        }

        @Override
        public TestScore build() {
            return new TestScore(getName(), getIcon(), getScope(), getConfiguration(), getNode());
        }

        @Override
        public void read(final ToolParser factory, final ToolConfiguration tool, final FilteredLog log) {
            readNode(factory, tool, log);
        }

        @Override
        public String getType() {
            return "test";
        }

        @Override
        String getDefaultTopLevelName() {
            return "Test Results";
        }
    }
}
