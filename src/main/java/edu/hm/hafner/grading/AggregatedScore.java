package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.grading.AnalysisScore.AnalysisScoreBuilder;
import edu.hm.hafner.grading.CoverageScore.CoverageScoreBuilder;
import edu.hm.hafner.grading.TestScore.TestScoreBuilder;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.Generated;

/**
 * Stores the scores of an autograding run. Persists the configuration and the scores for each metric.
 *
 * @author Eva-Maria Zeintl
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.GodClass")
public class AggregatedScore implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;

    private final String configuration;
    private final FilteredLog log;

    private final List<TestScore> testScores = new ArrayList<>();
    private int testAchieved;

    private final List<CoverageScore> codeCoverageScores = new ArrayList<>();
    private int codeCoverageAchieved;

    private final List<CoverageScore> mutationCoverageScores = new ArrayList<>();
    private int mutationCoverageAchieved;

    private final List<AnalysisScore> analysisScores = new ArrayList<>();
    private int analysisAchieved;

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
        this.configuration = configuration;
        this.log = log;
    }

    public List<String> getInfoMessages() {
        return log.getInfoMessages();
    }

    public List<String> getErrorMessages() {
        return log.getErrorMessages();
    }

    /**
     * Returns the number of achieved points.
     *
     * @return the number of achieved points
     */
    public int getAchieved() {
        return testAchieved + codeCoverageAchieved + mutationCoverageAchieved + analysisAchieved;
    }

    /**
     * Returns the total number of points that could be achieved.
     *
     * @return the total number of points that could be achieved
     */
    public int getTotal() {
        return getAnalysisTotal()
                + getTotal(testScores)
                + getTotal(codeCoverageScores)
                + getTotal(mutationCoverageScores);
    }

    public int getTestTotal() {
        return getTotal(testScores);
    }

    public int getCodeCoverageTotal() {
        return getTotal(codeCoverageScores);
    }

    public int getMutationCoverageTotal() {
        return getTotal(mutationCoverageScores);
    }

    public int getAnalysisTotal() {
        return getTotal(analysisScores);
    }

    public int getTestMax() {
        return getMax(testScores);
    }

    public int getCodeCoverageMax() {
        return getMax(codeCoverageScores);
    }

    public int getMutationCoverageMax() {
        return getMax(mutationCoverageScores);
    }

    public int getAnalysisMax() {
        return getMax(analysisScores);
    }

    public int getAnalysisScore() {
        return getScore(analysisScores);
    }

    private int getScore(final List<? extends Score<?, ?>> scores) {
        return scores.stream()
                .map(Score::getImpact)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private int getTotal(final List<? extends Score<?, ?>> scores) {
        return scores.stream()
                .map(Score::getValue)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private int getMax(final List<? extends Score<?, ?>> scores) {
        return scores.stream()
                .map(Score::getConfiguration)
                .map(Configuration::getMaxScore)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public int getTestAchieved() {
        return testAchieved;
    }

    public int getTestRatio() {
        return getRatio(getTestTotal(), getTestAchieved());
    }

    public int getCodeCoverageAchieved() {
        return codeCoverageAchieved;
    }

    public int getCoverageRatio() {
        return getRatio(getCodeCoverageTotal(), getCodeCoverageAchieved());
    }

    public int getMutationCoverageAchieved() {
        return mutationCoverageAchieved;
    }

    public int getPitRatio() {
        return getRatio(getMutationCoverageTotal(), getMutationCoverageAchieved());
    }

    public int getAnalysisAchieved() {
        return analysisAchieved;
    }

    public int getAnalysisRatio() {
        return getRatio(getAnalysisTotal(), getAnalysisAchieved());
    }

    /**
     * Returns the success ratio, i.e., the number of achieved points divided by total points.
     *
     * @return the success ratio
     */
    public int getRatio() {
        return getRatio(getTotal(), getAchieved());
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

    public List<AnalysisScore> getAnalysisScores() {
        return analysisScores;
    }

    public List<TestScore> getTestScores() {
        return testScores;
    }

    public List<CoverageScore> getCoverageScores() {
        return codeCoverageScores;
    }

    public List<CoverageScore> getMutationCoverageScores() {
        return mutationCoverageScores;
    }

    private int computeScore(final int maxScore, final int totalImpact, final boolean positive) {
        if (totalImpact < 0) {
            return Math.max(0, maxScore + totalImpact);
        }
        else if (totalImpact > 0) {
            return Math.min(maxScore, totalImpact);
        }
        if (positive) {
            return 0;
        }
        else {
            return maxScore;
        }
    }

    private int aggregateDelta(final List<? extends Score> scores) {
        var delta = 0;
        for (Score score : scores) {
            delta = delta + score.getImpact();
        }
        return delta;
    }

    @Override
    @Generated
    public String toString() {
        return String.format("Score: %d / %d", getAchieved(), getTotal());
    }

    public void gradeAnalysis(final AnalysisReportFactory factory) {
        var analysisConfigurations = AnalysisConfiguration.from(configuration);

        log.logInfo("Processing %d static analysis configuration(s)", analysisConfigurations.size());
        for (AnalysisConfiguration analysisConfiguration : analysisConfigurations) {
            log.logInfo("%s Configuration: %s", analysisConfiguration.getName(), analysisConfiguration);

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
        var coverageConfigurations = CoverageConfiguration.from(configuration);

        log.logInfo("Processing %d coverage configuration(s)", coverageConfigurations.size());
        for (CoverageConfiguration coverageConfiguration : coverageConfigurations) {
            log.logInfo("%s Configuration: %s", coverageConfiguration.getName(), coverageConfiguration);

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
        var testConfigurations = TestConfiguration.from(configuration);

        log.logInfo("Processing %d test configuration(s)", testConfigurations.size());
        for (TestConfiguration testConfiguration : testConfigurations) {
            log.logInfo("%s Configuration: %s", testConfiguration.getName(), testConfiguration);

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
    }

    private static class TypeScore<S extends Score> {
        private final int total;
        private final List<S> scores;

        TypeScore(final int total, final List<S> scores) {
            this.total = total;
            this.scores = scores;
        }

        public int getTotal() {
            return total;
        }

        public List<S> getScores() {
            return scores;
        }
    }
}
