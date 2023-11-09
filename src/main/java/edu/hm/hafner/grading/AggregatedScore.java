package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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

    private final List<CoverageScore> codeCoverageScores = new ArrayList<>();

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
        return getAchievedScore(codeCoverageScores);
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

    public int getCoverageMaxScore() {
        return getMaxScore(coverageConfigurations);
    }

    public int getAnalysisMaxScore() {
        return getMaxScore(analysisConfigurations);
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
        return getRatio(getAchievedScore(), getAchievedScore());
    }

    public int getTestRatio() {
        return getRatio(getTestAchievedScore(), getTestMaxScore());
    }

    public int getCoverageRatio() {
        return getRatio(getCoverageAchievedScore(), getCoverageMaxScore());
    }

    public int getAnalysisRatio() {
        return getRatio(getAnalysisAchievedScore(), getAnalysisMaxScore());
    }

    private int getRatio(final int total, final int achieved) {
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
        return List.copyOf(codeCoverageScores);
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
        if (!codeCoverageScores.equals(that.codeCoverageScores)) {
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
        result = 31 * result + codeCoverageScores.hashCode();
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

    public void gradeAnalysis(final AnalysisReportFactory factory) {
        log.logInfo("-> Processing %d static analysis configuration(s)", analysisConfigurations.size());
        for (AnalysisConfiguration analysisConfiguration : analysisConfigurations) {
            log.logInfo("-> %s Configuration: %s", analysisConfiguration.getName(), analysisConfiguration);

            List<AnalysisScore> scores = new ArrayList<>();
            for (ToolConfiguration tool : analysisConfiguration.getTools()) {
                var report = factory.create(tool, log);
                var score = createAnalysisScore(analysisConfiguration, tool.getId(), report.getName(), report);
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

    private static AnalysisScore createAnalysisScore(final AnalysisConfiguration configuration,
            final String id, final String name, final Report report) {
        return new AnalysisScoreBuilder()
                .withConfiguration(configuration)
                .withName(name)
                .withId(id)
                .withReport(report)
                .build();
    }

    public interface AnalysisReportFactory {
        Report create(ToolConfiguration tool, FilteredLog log);
    }

    public void gradeCoverage(final CoverageReportFactory factory) {
        log.logInfo("-> Processing %d coverage configuration(s)", coverageConfigurations.size());
        for (CoverageConfiguration coverageConfiguration : coverageConfigurations) {
            log.logInfo("-> %s Configuration: %s", coverageConfiguration.getName(), coverageConfiguration);

            List<CoverageScore> scores = new ArrayList<>();
            for (ToolConfiguration tool : coverageConfiguration.getTools()) {
                var report = factory.create(tool, log);
                var score = createCoverageScore(coverageConfiguration, tool.getId(), report.getName(), report, coverageConfiguration.getMetric());
                scores.add(score);
            }

            var aggregation = new CoverageScoreBuilder()
                    .withConfiguration(coverageConfiguration)
                    .withScores(scores)
                    .build();

            codeCoverageScores.add(aggregation);

            log.logInfo("=> %s Score: %d of %d", coverageConfiguration.getName(), aggregation.getValue(),
                    aggregation.getMaxScore());
        }
    }

    private static CoverageScore createCoverageScore(final CoverageConfiguration configuration,
            final String id, final String name, final Node rootNode, final Metric metric) {
        return new CoverageScoreBuilder()
                .withConfiguration(configuration)
                .withName(name)
                .withId(id)
                .withReport(rootNode, metric)
                .build();
    }


    public interface CoverageReportFactory {
        Node create(ToolConfiguration tool, FilteredLog log);
    }

    public void gradeTests(final TestReportFactory factory) {
        log.logInfo("-> Processing %d test configuration(s)", testConfigurations.size());
        for (TestConfiguration testConfiguration : testConfigurations) {
            log.logInfo("-> %s Configuration: %s", testConfiguration.getName(), testConfiguration);

            List<TestScore> scores = new ArrayList<>();
            for (ToolConfiguration tool : testConfiguration.getTools()) {
                var report = factory.create(tool, log);
                var score = createTestScore(testConfiguration, tool.getId(), tool.getName(), report);
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

    private static TestScore createTestScore(final TestConfiguration configuration,
            final String id, final String name, final TestResult result) {
        return new TestScore.TestScoreBuilder()
                .withConfiguration(configuration)
                .withId(id)
                .withName(name)
                .withTotalSize(result.getFailedSize() + result.getPassedSize() + result.getSkippedSize())
                .withFailedSize(result.getFailedSize())
                .withSkippedSize(result.getSkippedSize())
                .build();
    }

    public interface TestReportFactory {
        TestResult create(ToolConfiguration tool, FilteredLog log);
    }

    public static class TestResult {
        private final int passedSize;
        private final int failedSize;
        private final int skippedSize;

        private final List<String> messages = new ArrayList<>();

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
