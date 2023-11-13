package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.grading.AnalysisScore.AnalysisScoreBuilder;
import edu.hm.hafner.grading.CoverageScore.CoverageScoreBuilder;
import edu.hm.hafner.grading.TestScore.TestScoreBuilder;
import edu.hm.hafner.util.FilteredLog;

/**
 * Stores the scores of an autograding run. Persists the configuration and the scores for each metric.
 *
 * @author Eva-Maria Zeintl
 * @author Ullrich Hafner
 */
public final class AggregatedScore implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;

    private final FilteredLog log;

    private final List<TestScore> testScores = new ArrayList<>();
    private final List<CoverageScore> coverageScores = new ArrayList<>();
    private final List<AnalysisScore> analysisScores = new ArrayList<>();

    private final List<TestConfiguration> testConfigurations;
    private final List<CoverageConfiguration> coverageConfigurations;
    private final List<AnalysisConfiguration> analysisConfigurations;

    private static FilteredLog createNullLogger() {
        return new FilteredLog("Autograding");
    }

    AggregatedScore() {
        this("{}", createNullLogger());
    }

    /**
     * Creates a new {@link AggregatedScore} with the specified configuration.
     *
     * @param configuration
     *         the auto grading configuration
     * @param log
     *         logger that is used to report the progress
     */
    public AggregatedScore(final String configuration, final FilteredLog log) {
        analysisConfigurations = AnalysisConfiguration.from(configuration);
        coverageConfigurations = CoverageConfiguration.from(configuration);
        testConfigurations = TestConfiguration.from(configuration);

        this.log = log;
    }

    public List<String> getInfoMessages() {
        return log.getInfoMessages();
    }

    public List<String> getErrorMessages() {
        return log.getErrorMessages();
    }

    /**
     * Returns the aggregated score, i.e., the number of achieved points.
     *
     * @return the number of achieved points
     */
    public int getAchievedScore() {
        return getTestAchievedScore() + getCoverageAchievedScore() + getAnalysisAchievedScore();
    }

    public int getTestAchievedScore() {
        return getAchievedScore(testScores);
    }

    public int getCoverageAchievedScore() {
        return getAchievedScore(coverageScores);
    }

    public int getCodeCoverageAchievedScore() {
        return getAchievedScore(getCodeCoverageScores());
    }

    public int getMutationCoverageAchievedScore() {
        return getAchievedScore(getMutationCoverageScores());
    }

    public int getAnalysisAchievedScore() {
        return getAchievedScore(analysisScores);
    }

    private int getAchievedScore(final List<? extends Score<?, ?>> scores) {
        return scores.stream()
                .map(Score::getValue)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Returns the total number of points, i.e., the maximum score.
     *
     * @return the total number of points that could be achieved
     */
    public int getMaxScore() {
        return getTestMaxScore() + getCoverageMaxScore() + getAnalysisMaxScore();
    }

    public int getTestMaxScore() {
        return getMaxScore(testConfigurations);
    }

    public boolean hasTests() {
        return !testConfigurations.isEmpty();
    }

    public int getCoverageMaxScore() {
        return getMaxScore(coverageConfigurations);
    }

    public boolean hasCoverage() {
        return !coverageConfigurations.isEmpty();
    }

    public int getCodeCoverageMaxScore() {
        return getMaxScore(getCodeCoverageConfigurations());
    }

    public boolean hasCodeCoverage() {
        return !getCodeCoverageConfigurations().isEmpty();
    }

    public int getMutationCoverageMaxScore() {
        return getMaxScore(getMutationCoverageConfigurations());
    }

    public boolean hasMutationCoverage() {
        return !getMutationCoverageConfigurations().isEmpty();
    }

    private List<CoverageConfiguration> getMutationCoverageConfigurations() {
        return coverageConfigurations.stream()
                .filter(configuration -> isMutation(configuration.getId(), configuration.getName()))
                .toList();
    }

    private boolean isMutation(final String id, final String name) {
        return StringUtils.containsAnyIgnoreCase(id + name, CoverageConfiguration.MUTATION_IDS);
    }

    private List<CoverageConfiguration> getCodeCoverageConfigurations() {
        List<CoverageConfiguration> configurations = new ArrayList<>(coverageConfigurations);
        configurations.removeAll(getMutationCoverageConfigurations());
        return configurations;
    }

    public int getAnalysisMaxScore() {
        return getMaxScore(analysisConfigurations);
    }

    public boolean hasAnalysis() {
        return !analysisConfigurations.isEmpty();
    }

    private int getMaxScore(final List<? extends Configuration> configurations) {
        return configurations.stream()
                .map(Configuration::getMaxScore)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Returns the success ratio, i.e., the number of achieved points divided by total points.
     *
     * @return the success ratio
     */
    public int getRatio() {
        return getRatio(getAchievedScore(), getMaxScore());
    }

    public int getTestRatio() {
        return getRatio(getTestAchievedScore(), getTestMaxScore());
    }

    public int getCoverageRatio() {
        return getRatio(getCoverageAchievedScore(), getCoverageMaxScore());
    }

    public int getCodeCoverageRatio() {
        return getRatio(getCodeCoverageAchievedScore(), getCodeCoverageMaxScore());
    }

    public int getMutationCoverageRatio() {
        return getRatio(getMutationCoverageAchievedScore(), getMutationCoverageMaxScore());
    }

    public int getAnalysisRatio() {
        return getRatio(getAnalysisAchievedScore(), getAnalysisMaxScore());
    }

    private int getRatio(final int achieved, final int total) {
        if (total == 0) {
            return 100;
        }
        return achieved * 100 / total;
    }

    /**
     * Returns whether at least one unit test failure has been recorded. In such a case, mutation results will not be
     * available.
     *
     * @return {@code true} if there are unit test failures, {@code false} otherwise
     */
    public boolean hasTestFailures() {
        return hasPositiveCount(getTestScores().stream().map(TestScore::getFailedSize));
    }

    /**
     * Returns whether at least one static analysis warning has been recorded.
     *
     * @return {@code true} if there are static analysis warnings, {@code false} otherwise
     */
    public boolean hasWarnings() {
        return hasPositiveCount(getAnalysisScores().stream().map(AnalysisScore::getTotalSize));
    }

    private boolean hasPositiveCount(final Stream<Integer> integerStream) {
        return integerStream.mapToInt(Integer::intValue).sum() > 0;
    }

    public List<TestScore> getTestScores() {
        return List.copyOf(testScores);
    }

    public List<CoverageScore> getCoverageScores() {
        return List.copyOf(coverageScores);
    }

    public List<CoverageScore> getMutationCoverageScores() {
        return coverageScores.stream()
                .filter(score -> isMutation(score.getId(), score.getName()))
                .toList();
    }

    public List<CoverageScore> getCodeCoverageScores() {
        List<CoverageScore> scores = new ArrayList<>(coverageScores);
        scores.removeAll(getMutationCoverageScores());
        return scores;
    }

    public List<AnalysisScore> getAnalysisScores() {
        return List.copyOf(analysisScores);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AggregatedScore that = (AggregatedScore) o;

        if (!Objects.equals(log, that.log)) {
            return false;
        }
        if (!testScores.equals(that.testScores)) {
            return false;
        }
        if (!coverageScores.equals(that.coverageScores)) {
            return false;
        }
        if (!analysisScores.equals(that.analysisScores)) {
            return false;
        }
        if (!Objects.equals(testConfigurations, that.testConfigurations)) {
            return false;
        }
        if (!Objects.equals(coverageConfigurations, that.coverageConfigurations)) {
            return false;
        }
        return Objects.equals(analysisConfigurations, that.analysisConfigurations);
    }

    @Override
    public int hashCode() {
        int result = log != null ? log.hashCode() : 0;
        result = 31 * result + testScores.hashCode();
        result = 31 * result + coverageScores.hashCode();
        result = 31 * result + analysisScores.hashCode();
        result = 31 * result + (testConfigurations != null ? testConfigurations.hashCode() : 0);
        result = 31 * result + (coverageConfigurations != null ? coverageConfigurations.hashCode() : 0);
        result = 31 * result + (analysisConfigurations != null ? analysisConfigurations.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("Score: %d / %d", getAchievedScore(), getMaxScore());
    }

    /**
     * Grades the reports given by the report factory and creates corresponding scores for the static analysis.
     *
     * @param factory
     *         the factory to create the reports
     */
    public void gradeAnalysis(final AnalysisReportFactory factory) {
        log.logInfo("Processing %d static analysis configuration(s)", analysisConfigurations.size());
        for (AnalysisConfiguration analysisConfiguration : analysisConfigurations) {
            log.logInfo("%s Configuration:%n%s", analysisConfiguration.getName(), analysisConfiguration);

            List<AnalysisScore> scores = new ArrayList<>();
            for (ToolConfiguration tool : analysisConfiguration.getTools()) {
                var report = factory.create(tool, log);
                var score = new AnalysisScoreBuilder()
                        .withConfiguration(analysisConfiguration)
                        .withName(StringUtils.defaultIfBlank(tool.getName(), report.getName()))
                        .withId(tool.getId())
                        .withReport(report)
                        .build();
                scores.add(score);
            }

            var aggregation = new AnalysisScoreBuilder()
                    .withConfiguration(analysisConfiguration)
                    .withScores(scores)
                    .build();

            analysisScores.add(aggregation);

            log.logInfo("=> %s Score: %d of %d", analysisConfiguration.getName(), aggregation.getValue(),
                    aggregation.getMaxScore());
        }
    }

    /**
     * Grades the reports given by the report factory and creates corresponding scores for the coverage.
     *
     * @param factory the factory to create the reports
     */
    public void gradeCoverage(final CoverageReportFactory factory) {
        log.logInfo("Processing %d coverage configuration(s)", coverageConfigurations.size());
        for (CoverageConfiguration coverageConfiguration : coverageConfigurations) {
            log.logInfo("%s Configuration:%n%s", coverageConfiguration.getName(), coverageConfiguration);

            List<CoverageScore> scores = new ArrayList<>();
            for (ToolConfiguration tool : coverageConfiguration.getTools()) {
                var report = factory.create(tool, log);
                var score = new CoverageScoreBuilder()
                        .withConfiguration(coverageConfiguration)
                        .withName(StringUtils.defaultIfBlank(tool.getName(), report.getName()))
                        .withId(tool.getId())
                        .withReport(report, Metric.fromTag(tool.getMetric()))
                        .build();
                scores.add(score);
            }

            var aggregation = new CoverageScoreBuilder()
                    .withConfiguration(coverageConfiguration)
                    .withScores(scores)
                    .build();

            coverageScores.add(aggregation);

            log.logInfo("=> %s Score: %d of %d", coverageConfiguration.getName(), aggregation.getValue(),
                    aggregation.getMaxScore());
        }
    }

    /**
     * Grades the reports given by the report factory and creates corresponding scores for the tests.
     *
     * @param factory the factory to create the reports
     */
    public void gradeTests(final TestReportFactory factory) {
        log.logInfo("Processing %d test configuration(s)", testConfigurations.size());
        for (TestConfiguration testConfiguration : testConfigurations) {
            log.logInfo("%s Configuration:%n%s", testConfiguration.getName(), testConfiguration);

            List<TestScore> scores = new ArrayList<>();
            for (ToolConfiguration tool : testConfiguration.getTools()) {
                var report = factory.create(tool, log);
                var score = new TestScoreBuilder()
                        .withConfiguration(testConfiguration)
                        .withId(tool.getId())
                        .withName(tool.getName())
                        .withTotalSize(report.getFailedSize() + report.getPassedSize() + report.getSkippedSize())
                        .withFailedSize(report.getFailedSize())
                        .withSkippedSize(report.getSkippedSize())
                        .build();
                scores.add(score);
            }

            var aggregation = new TestScoreBuilder()
                    .withConfiguration(testConfiguration)
                    .withScores(scores)
                    .build();

            testScores.add(aggregation);

            log.logInfo("=> %s Score: %d of %d", testConfiguration.getName(), aggregation.getValue(),
                    aggregation.getMaxScore());
        }
    }

    /**
     * Factory to create the static analysis reports.
     */
    public interface AnalysisReportFactory {
        /**
         * Creates a static analysis report for the specified tool.
         *
         * @param tool
         *         the tool to create the report for
         * @param log
         *         the logger to report the progress
         *
         * @return the created report
         * @throws NoSuchElementException if there is no analysis report for the specified tool
         */
        Report create(ToolConfiguration tool, FilteredLog log);
    }

    /**
     * Factory to create the coverage reports.
     */
    public interface CoverageReportFactory {
        /**
         * Creates a coverage report for the specified tool.
         *
         * @param tool
         *         the tool to create the report for
         * @param log
         *         the logger to report the progress
         *
         * @return the created report
         * @throws NoSuchElementException if there is no coverage report for the specified tool
         */
        Node create(ToolConfiguration tool, FilteredLog log);
    }

    /**
     * Factory to create the test reports.
     */
    public interface TestReportFactory {
        /**
         * Creates a test report for the specified tool.
         *
         * @param tool
         *         the tool to create the report for
         * @param log
         *         the logger to report the progress
         *
         * @return the created report
         * @throws NoSuchElementException if there is no test report for the specified tool
         */
        TestResult create(ToolConfiguration tool, FilteredLog log);
    }

    /**
     * Simple data object to store test results.
     */
    public static class TestResult {
        private final int passedSize;
        private final int failedSize;
        private final int skippedSize;
        private final List<String> messages = new ArrayList<>();

        /**
         * Creates a new {@link TestResult} with the specified results.
         *
         * @param passedSize
         *         the number of passed tests
         * @param failedSize
         *         the number of failed tests
         * @param skippedSize
         *         the number of skipped tests
         */
        public TestResult(final int passedSize, final int failedSize, final int skippedSize) {
            this.passedSize = passedSize;
            this.failedSize = failedSize;
            this.skippedSize = skippedSize;
        }

        public int getPassedSize() {
            return passedSize;
        }

        public int getFailedSize() {
            return failedSize;
        }

        public int getSkippedSize() {
            return skippedSize;
        }

        public int getTotal() {
            return getPassedSize() + getFailedSize() + getSkippedSize();
        }

        public List<String> getMessages() {
            return messages;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TestResult that = (TestResult) o;

            if (passedSize != that.passedSize) {
                return false;
            }
            if (failedSize != that.failedSize) {
                return false;
            }
            if (skippedSize != that.skippedSize) {
                return false;
            }
            return messages.equals(that.messages);
        }

        @Override
        public int hashCode() {
            int result = passedSize;
            result = 31 * result + failedSize;
            result = 31 * result + skippedSize;
            result = 31 * result + messages.hashCode();
            return result;
        }
    }
}
