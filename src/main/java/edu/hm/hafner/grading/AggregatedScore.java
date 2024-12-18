package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.grading.AnalysisScore.AnalysisScoreBuilder;
import edu.hm.hafner.grading.CoverageScore.CoverageScoreBuilder;
import edu.hm.hafner.grading.MetricScore.MetricScoreBuilder;
import edu.hm.hafner.grading.TestScore.TestScoreBuilder;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.Generated;

/**
 * Stores the scores of an autograding run. Persists the configuration and the scores for each metric.
 *
 * @author Eva-Maria Zeintl
 * @author Ullrich Hafner
 */
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity", "PMD.ExcessivePublicCount", "PMD.CouplingBetweenObjects"})
public final class AggregatedScore implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;
    private static final int MAX_PERCENTAGE = 100;

    private final FilteredLog log;

    private final List<TestScore> testScores = new ArrayList<>();
    private final List<CoverageScore> coverageScores = new ArrayList<>();
    private final List<AnalysisScore> analysisScores = new ArrayList<>();
    private final List<MetricScore> metricScores = new ArrayList<>();

    private final List<TestConfiguration> testConfigurations;
    private final List<CoverageConfiguration> coverageConfigurations;
    private final List<AnalysisConfiguration> analysisConfigurations;
    private final List<MetricConfiguration> metricConfigurations;

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
        metricConfigurations = MetricConfiguration.from(configuration);

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
        return getTestAchievedScore() + getCoverageAchievedScore()
                + getAnalysisAchievedScore() + getMetricAchievedScore();
    }

    private int getAchievedScore(final List<? extends Score<?, ?>> scores) {
        return scores.stream()
                .map(Score::getValue)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Returns the percentage of the achieved score.
     *
     * @return the percentage
     */
    public int getAchievedPercentage() {
        if (getMaxScore() == 0) {
            return MAX_PERCENTAGE;
        }
        return getAchievedScore() * MAX_PERCENTAGE / getMaxScore();
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

    public int getMetricAchievedScore() {
        return getAchievedScore(metricScores);
    }

    /**
     * Returns the total number of points, i.e., the maximum score.
     *
     * @return the total number of points that could be achieved
     */
    public int getMaxScore() {
        return getTestMaxScore() + getCoverageMaxScore() + getAnalysisMaxScore();
    }

    private int getMaxScore(final List<? extends Configuration> configurations) {
        return configurations.stream()
                .map(Configuration::getMaxScore)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public int getTestMaxScore() {
        return getMaxScore(testConfigurations);
    }

    /**
     * Returns whether at least one test configuration has been defined.
     *
     * @return {@code true} if there are test configurations, {@code false} otherwise
     */
    public boolean hasTests() {
        return !testConfigurations.isEmpty();
    }

    public int getCoverageMaxScore() {
        return getMaxScore(coverageConfigurations);
    }

    /**
     * Returns whether at least one coverage configuration has been defined.
     *
     * @return {@code true} if there are coverage configurations, {@code false} otherwise
     */
    public boolean hasCoverage() {
        return !coverageConfigurations.isEmpty();
    }

    public int getCodeCoverageMaxScore() {
        return getMaxScore(getCodeCoverageConfigurations());
    }

    /**
     * Returns whether at least one code coverage configuration has been defined.
     *
     * @return {@code true} if there are code coverage configurations, {@code false} otherwise
     */
    public boolean hasCodeCoverage() {
        return !getCodeCoverageConfigurations().isEmpty();
    }

    public int getMutationCoverageMaxScore() {
        return getMaxScore(getMutationCoverageConfigurations());
    }

    /**
     * Returns whether at least one mutation coverage configuration has been defined.
     *
     * @return {@code true} if there are mutation coverage configurations, {@code false} otherwise
     */
    public boolean hasMutationCoverage() {
        return !getMutationCoverageConfigurations().isEmpty();
    }

    private List<CoverageConfiguration> getMutationCoverageConfigurations() {
        return coverageConfigurations.stream()
                .filter(CoverageConfiguration::isMutationCoverage)
                .toList();
    }

    private List<CoverageConfiguration> getCodeCoverageConfigurations() {
        List<CoverageConfiguration> configurations = new ArrayList<>(coverageConfigurations);
        configurations.removeAll(getMutationCoverageConfigurations());
        return configurations;
    }

    public int getAnalysisMaxScore() {
        return getMaxScore(analysisConfigurations);
    }

    /**
     * Returns whether at least one static analysis configuration has been defined.
     *
     * @return {@code true} if there are static analysis configurations, {@code false} otherwise
     */
    public boolean hasAnalysis() {
        return !analysisConfigurations.isEmpty();
    }

    public int getMetricsMaxScore() {
        return getMaxScore(metricConfigurations);
    }

    /**
     * Returns whether at least one metric configuration has been defined.
     *
     * @return {@code true} if there are metric configurations, {@code false} otherwise
     */
    public boolean hasMetrics() {
        return !metricConfigurations.isEmpty();
    }

    /**
     * Returns the success ratio, i.e., the number of achieved points divided by total points.
     *
     * @return the success ratio
     */
    public int getRatio() {
        return getRatio(getAchievedScore(), getMaxScore());
    }

    private int getRatio(final int achieved, final int total) {
        if (total == 0) {
            return MAX_PERCENTAGE;
        }
        return achieved * MAX_PERCENTAGE / total;
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

    public int getMetricRatio() {
        return getRatio(getMetricAchievedScore(), getMetricsMaxScore());
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

    /**
     * Filters the coverage scores and returns only the mutation coverage scores.
     *
     * @return the mutation coverage scores
     */
    public List<CoverageScore> getMutationCoverageScores() {
        return coverageScores.stream()
                .filter(score -> score.getConfiguration().isMutationCoverage())
                .toList();
    }

    /**
     * Filters the coverage scores and returns only the code coverage scores.
     *
     * @return the code coverage scores
     */
    public List<CoverageScore> getCodeCoverageScores() {
        List<CoverageScore> scores = new ArrayList<>(coverageScores);
        scores.removeAll(getMutationCoverageScores());
        return scores;
    }

    /**
     * Returns all issues that have been reported by the static analysis tools.
     *
     * @return the issues
     */
    public List<Issue> getIssues() {
        return getAnalysisScores().stream()
                .map(AnalysisScore::getReport)
                .flatMap(Report::stream)
                .toList();
    }

    /**
     * Returns the covered files for the specified metric.
     *
     * @param metric
     *         the metric to get the covered files for
     *
     * @return the covered files
     */
    public List<FileNode> getCoveredFiles(final Metric metric) {
        return getCoverageScores().stream()
                .map(CoverageScore::getSubScores)
                .flatMap(Collection::stream)
                .filter(score -> score.getMetric() == metric)
                .map(CoverageScore::getReport)
                .map(Node::getAllFileNodes)
                .flatMap(Collection::stream)
                .toList();
    }

    public List<AnalysisScore> getAnalysisScores() {
        return List.copyOf(analysisScores);
    }

    public List<MetricScore> getMetricScores() {
        return List.copyOf(metricScores);
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
        var that = (AggregatedScore) o;
        return Objects.equals(log, that.log)
                && Objects.equals(testScores, that.testScores)
                && Objects.equals(coverageScores, that.coverageScores)
                && Objects.equals(analysisScores, that.analysisScores)
                && Objects.equals(metricScores, that.metricScores)
                && Objects.equals(testConfigurations, that.testConfigurations)
                && Objects.equals(coverageConfigurations, that.coverageConfigurations)
                && Objects.equals(analysisConfigurations, that.analysisConfigurations)
                && Objects.equals(metricConfigurations, that.metricConfigurations);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(log, testScores, coverageScores, analysisScores, metricScores, testConfigurations,
                coverageConfigurations, analysisConfigurations, metricConfigurations);
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Score: %d / %d", getAchievedScore(), getMaxScore());
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
                        .withIcon(tool.getIcon())
                        .withReport(report)
                        .build();
                scores.add(score);
                logSubResult(score);
            }

            var aggregation = new AnalysisScoreBuilder()
                    .withConfiguration(analysisConfiguration)
                    .withScores(scores)
                    .build();

            analysisScores.add(aggregation);

            logResult(analysisConfiguration, aggregation);
        }
    }

    /**
     * Grades the reports given by the report factory and creates corresponding scores for the coverage.
     *
     * @param factory
     *         the factory to create the reports
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
                        .withIcon(tool.getIcon())
                        .withReport(report, Metric.fromTag(tool.getMetric()))
                        .build();
                scores.add(score);
                logSubResult(score);
            }

            var aggregation = new CoverageScoreBuilder()
                    .withConfiguration(coverageConfiguration)
                    .withScores(scores)
                    .build();

            coverageScores.add(aggregation);

            logResult(coverageConfiguration, aggregation);
        }
    }

    /**
     * Grades the reports given by the report factory and creates corresponding scores for the tests.
     *
     * @param factory
     *         the factory to create the reports
     */
    public void gradeTests(final CoverageReportFactory factory) {
        log.logInfo("Processing %d test configuration(s)", testConfigurations.size());
        for (TestConfiguration testConfiguration : testConfigurations) {
            log.logInfo("%s Configuration:%n%s", testConfiguration.getName(), testConfiguration);

            List<TestScore> scores = new ArrayList<>();
            for (ToolConfiguration tool : testConfiguration.getTools()) {
                var report = factory.create(tool, log);
                var score = new TestScoreBuilder()
                        .withConfiguration(testConfiguration)
                        .withId(tool.getId())
                        .withIcon(tool.getIcon())
                        .withName(StringUtils.defaultIfBlank(tool.getName(), report.getName()))
                        .withReport(report)
                        .build();
                scores.add(score);
                logSubResult(score);
            }

            var aggregation = new TestScoreBuilder()
                    .withConfiguration(testConfiguration)
                    .withScores(scores)
                    .build();

            testScores.add(aggregation);

            logResult(testConfiguration, aggregation);
        }
    }

    /**
     * Grades the reports given by the report factory and creates corresponding scores for the metrics.
     *
     * @param factory
     *         the factory to create the reports
     */
    public void gradeMetrics(final CoverageReportFactory factory) {
        log.logInfo("Processing %d metric configuration(s)", metricConfigurations.size());
        for (MetricConfiguration metricConfiguration : metricConfigurations) {
            log.logInfo("%s Configuration:%n%s", metricConfiguration.getName(), metricConfiguration);

            List<MetricScore> scores = new ArrayList<>();
            for (ToolConfiguration tool : metricConfiguration.getTools()) {
                var report = factory.create(tool, log);
                var score = new MetricScoreBuilder()
                        .withConfiguration(metricConfiguration)
                        .withId(tool.getId())
                        .withIcon(tool.getIcon())
                        .withName(StringUtils.defaultIfBlank(tool.getName(), report.getName()))
                        .withReport(report, Metric.fromName(tool.getMetric()))
                        .build();
                scores.add(score);
                logSubResult(score);
            }

            var aggregation = new MetricScoreBuilder()
                    .withConfiguration(metricConfiguration)
                    .withScores(scores)
                    .build();

            metricScores.add(aggregation);

            logResult(metricConfiguration, aggregation);
        }
    }

    private void logSubResult(final Score<?, ?> score) {
        if (!score.hasMaxScore()) {
            log.logInfo("=> %s: %s",
                    score.getName(), score.createSummary());
        }
    }

    private void logResult(final Configuration configuration, final Score<?, ?> score) {
        if (score.hasMaxScore()) {
            log.logInfo("=> %s Score: %d of %d",
                    configuration.getName(), score.getValue(), score.getMaxScore());
        }
        else {
            log.logInfo("=> %s: %s",
                    configuration.getName(), score.createSummary());
        }
    }

    /**
     * Returns statistical metrics for the results aggregated in this score. The key of the returned map is a string
     * that identifies the metric, the value is the integer-based result.
     *
     * @return the metrics
     */
    public Map<String, Integer> getMetrics() {
        var metrics = new HashMap<String, Integer>();
        if (hasTests()) {
            metrics.putAll(getTestMetrics());
        }
        if (hasCodeCoverage()) {
            metrics.putAll(getCoverageMetrics());
        }
        if (hasMutationCoverage()) {
            metrics.putAll(getMutationMetrics());
        }
        if (hasAnalysis()) {
            metrics.putAll(getAnalysisTopLevelMetrics());
            metrics.putAll(getAnalysisMetrics());
        }
        if (hasMetrics()) {
            metrics.putAll(getSoftwareMetrics());
        }
        return metrics;
    }

    private Map<String, Integer> getTestMetrics() {
        var tests = getTestScores().stream()
                .map(TestScore::getTotalSize).reduce(0, Integer::sum);
        return Map.of("tests", tests);
    }

    private Map<String, Integer> getSoftwareMetrics() {
        return getMetricScores().stream()
                .map(Score::getSubScores)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(MetricScore::getMetricTagName, MetricScore::getMetricValue));
    }

    private Map<String, Integer> getCoverageMetrics() {
        return getCodeCoverageScores().stream()
                .map(Score::getSubScores)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(CoverageScore::getMetricTagName, CoverageScore::getCoveredPercentage));
    }

    private Map<String, Integer> getMutationMetrics() {
        return getMutationCoverageScores().stream()
                .map(Score::getSubScores)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(CoverageScore::getMetricTagName, CoverageScore::getCoveredPercentage));
    }

    private Map<String, Integer> getAnalysisMetrics() {
        return getAnalysisScores().stream()
                .map(Score::getSubScores)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Score::getId, AnalysisScore::getTotalSize));
    }

    private Map<String, Integer> getAnalysisTopLevelMetrics() {
        return getAnalysisScores().stream()
                .collect(Collectors.toMap(Score::getId, AnalysisScore::getTotalSize));
    }

    /**
     * Factory to create the static analysis reports based on the analysis-model.
     *
     * @see <a href="https://github.com/jenkinsci/analysis-model">Analysis Model</a>
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
         * @throws NoSuchElementException
         *         if there is no analysis report for the specified tool
         */
        Report create(ToolConfiguration tool, FilteredLog log);
    }

    /**
     * Factory to create the coverage, test and metric reports that are based on the coverage-model.
     *
     * @see <a href="https://github.com/jenkinsci/coverage-model">Coverage Model</a>
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
         * @throws NoSuchElementException
         *         if there is no coverage report for the specified tool
         */
        Node create(ToolConfiguration tool, FilteredLog log);
    }
}
