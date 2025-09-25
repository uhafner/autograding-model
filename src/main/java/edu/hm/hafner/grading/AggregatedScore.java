package edu.hm.hafner.grading;

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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stores the scores of an autograding run. Persists the configuration and the scores for each metric.
 *
 * @author Eva-Maria Zeintl
 * @author Ullrich Hafner
 */
@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public final class AggregatedScore implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;
    private static final int MAX_PERCENTAGE = 100;

    private final FilteredLog log;

    @SuppressWarnings("serial")
    private final List<TestScore> testScores = new ArrayList<>();
    @SuppressWarnings("serial")
    private final List<CoverageScore> coverageScores = new ArrayList<>();
    @SuppressWarnings("serial")
    private final List<AnalysisScore> analysisScores = new ArrayList<>();
    @SuppressWarnings("serial")
    private final List<MetricScore> metricScores = new ArrayList<>();

    private static FilteredLog createNullLogger() {
        return new FilteredLog("Autograding");
    }

    AggregatedScore() {
        this(createNullLogger());
    }

    /**
     * Creates a new {@link AggregatedScore} with the specified configuration.
     *
     * @param log
     *         logger that is used to report the progress
     */
    public AggregatedScore(final FilteredLog log) {
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
        return getTestAchievedScore()
                + getCoverageAchievedScore()
                + getAnalysisAchievedScore()
                + getMetricAchievedScore();
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
        return getTestMaxScore()
                + getCoverageMaxScore()
                + getAnalysisMaxScore();
    }

    private int getMaxScore(final List<? extends Score<?, ?>> configurations) {
        return configurations.stream()
                .map(Score::getMaxScore)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public int getTestMaxScore() {
        return getMaxScore(testScores);
    }

    /**
     * Returns whether at least one test configuration has been defined.
     *
     * @return {@code true} if there are test configurations, {@code false} otherwise
     */
    public boolean hasTests() {
        return !testScores.isEmpty();
    }

    public int getCoverageMaxScore() {
        return getMaxScore(coverageScores);
    }

    /**
     * Returns whether at least one coverage configuration has been defined.
     *
     * @return {@code true} if there are coverage configurations, {@code false} otherwise
     */
    public boolean hasCoverage() {
        return !coverageScores.isEmpty();
    }

    public int getAnalysisMaxScore() {
        return getMaxScore(analysisScores);
    }

    /**
     * Returns whether at least one static analysis configuration has been defined.
     *
     * @return {@code true} if there are static analysis configurations, {@code false} otherwise
     */
    public boolean hasAnalysis() {
        return !analysisScores.isEmpty();
    }

    public int getMetricsMaxScore() {
        return getMaxScore(metricScores);
    }

    /**
     * Returns whether at least one metric configuration has been defined.
     *
     * @return {@code true} if there are metric configurations, {@code false} otherwise
     */
    public boolean hasMetrics() {
        return !metricScores.isEmpty();
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
        var nodeStream = getCoverageScores().stream()
                .map(CoverageScore::getSubScores)
                .flatMap(Collection::stream)
                .filter(score -> score.getMetric() == metric)
                .map(CoverageScore::getReport).toList();
        return nodeStream.stream()
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
                && Objects.equals(metricScores, that.metricScores);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(log, testScores, coverageScores, analysisScores, metricScores);
    }

    @Override
    public String toString() {
        if (getMaxScore() == 0) {
            return "Empty Score";
        }
        return String.format(Locale.ENGLISH, "Score: %d / %d", getAchievedScore(), getMaxScore());
    }

    /**
     * Grades the reports given by the report factory and creates corresponding scores for the static analysis.
     *
     * @param factory
     *         the factory to create the reports
     * @param analysisConfigurations
     *         the configurations to grade
     */
    public void gradeAnalysis(final ToolParser factory,
            final List<AnalysisConfiguration> analysisConfigurations) {
        grade(factory, analysisConfigurations, new AnalysisScoreBuilder(), analysisScores::add);
    }

    /**
     * Grades the reports given by the report factory and creates corresponding scores for the coverage.
     *
     * @param factory
     *         the factory to create the reports
     * @param coverageConfigurations
     *         the coverage configurations to grade     */
    public void gradeCoverage(final ToolParser factory,
            final List<CoverageConfiguration> coverageConfigurations) {
        grade(factory, coverageConfigurations, new CoverageScoreBuilder(), coverageScores::add);
    }

    /**
     * Grades the reports given by the report factory and creates corresponding scores for the tests.
     *
     * @param factory
     *         the factory to create the reports
     * @param testConfigurations
     *        the test configurations to grade
     */
    public void gradeTests(final ToolParser factory, final List<TestConfiguration> testConfigurations) {
        grade(factory, testConfigurations, new TestScoreBuilder(), testScores::add);
    }

    /**
     * Grades the reports given by the report factory and creates corresponding scores for the metrics.
     *
     * @param factory
     *         the factory to create the reports
     * @param metricConfigurations
     *        the metric configurations to grade
     */
    public void gradeMetrics(final ToolParser factory, final List<MetricConfiguration> metricConfigurations) {
        grade(factory, metricConfigurations, new MetricScoreBuilder(), metricScores::add);
    }

    private <S extends Score<S, C>, C extends Configuration> void grade(final ToolParser factory,
            final List<C> configurations, final ScoreBuilder<S, C> builder, final Consumer<S> setter) {
        log.logInfo("Processing %d %s configuration(s)", configurations.size(), builder.getType());

        for (var configuration : configurations) {
            log.logInfo("%s Configuration:%n%s", configuration.getName(), configuration);

            List<S> scores = new ArrayList<>();
            for (var tool : configuration.getTools()) {
                builder.setConfiguration(configuration);
                builder.read(factory, tool, log);
                builder.setName(tool.getName());

                var score = builder.build();
                scores.add(score);
                logSubResult(score);
            }

            builder.setConfiguration(configuration);
            builder.setName(configuration.getName());
            builder.setIcon(configuration.getIcon());

            var aggregation = builder.aggregate(scores);

            setter.accept(aggregation);

            logResult(configuration, aggregation);
        }
    }

    private void logSubResult(final Score<?, ?> score) {
        if (!score.hasMaxScore()) {
            log.logInfo("=> %s: %s", score.getName(), score.createSummary());
        }
    }

    private void logResult(final Configuration subConfiguration, final Score<?, ?> score) {
        if (score.hasMaxScore()) {
            log.logInfo("=> %s Score: %d of %d",
                    subConfiguration.getName(), score.getValue(), score.getMaxScore());
        }
        else {
            log.logInfo("=> %s: %s",
                    subConfiguration.getName(), score.createSummary());
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
        if (hasCoverage()) {
            metrics.putAll(getCoverageMetrics());
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
        return Map.of(
                "tests", getTestMetric(TestScore::getTotalSize),
                "tests-success-rate", getTestMetric(TestScore::getSuccessRate),
                "tests-failure-rate", getTestMetric(TestScore::getFailureRate));
    }

    private Integer getTestMetric(final Function<TestScore, Integer> metric) {
        return getTestScores().stream()
                .map(metric).reduce(0, Integer::sum);
    }

    private Map<String, Integer> getSoftwareMetrics() {
        return getMetricScores().stream()
                .map(Score::getSubScores)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(MetricScore::getMetricTagName, MetricScore::getMetricValue));
    }

    private Map<String, Integer> getCoverageMetrics() {
        return getCoverageScores().stream()
                .map(Score::getSubScores)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(CoverageScore::getMetricTagName, CoverageScore::getCoveredPercentage));
    }

    private Map<String, Integer> getAnalysisMetrics() {
        return getAnalysisScores().stream()
                .map(Score::getSubScores)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(s -> s.getReport().getId(), AnalysisScore::getTotalSize));
    }

    private Map<String, Integer> getAnalysisTopLevelMetrics() {
        return getAnalysisScores().stream()
                .collect(Collectors.toMap(
                        s -> StringUtils.lowerCase(s.getName()),
                        AnalysisScore::getTotalSize));
    }
}
