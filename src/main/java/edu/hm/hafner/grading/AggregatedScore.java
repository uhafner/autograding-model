package edu.hm.hafner.grading;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private static final long serialVersionUID = 1L;

    private final FilteredLog log;

    private final AnalysisConfiguration analysisConfiguration;
    private List<AnalysisScore> analysisScores = new ArrayList<>();
    private int analysisAchieved;

    private final TestConfiguration testsConfiguration;
    private List<TestScore> testScores = new ArrayList<>();
    private int testAchieved;

    private final CoverageConfiguration coverageConfiguration;
    private List<CoverageScore> coverageScores = new ArrayList<>();
    private int coverageAchieved;

    private final PitConfiguration pitConfiguration;
    private List<PitScore> pitScores = new ArrayList<>();
    private int pitAchieved;

    private static FilteredLog createNullLogger() {
        return new FilteredLog("Autograding");
    }

    /**
     * Creates a new {@link AggregatedScore} that does not grade anything ({@code null} object pattern).
     */
    public AggregatedScore() {
        this("{}", createNullLogger());
    }

    /**
     * Creates a new {@link AggregatedScore} with the specified configuration. Uses a {@code null} logger.
     *
     * @param configuration
     *         the grading configuration to use, must be a valid JSON object
     */
    public AggregatedScore(final String configuration) {
        this(configuration, createNullLogger());
    }

    /**
     * Creates a new {@link AggregatedScore} with the specified configuration.
     *
     * @param configuration
     *         the grading configuration to use, must be a valid JSON object
     * @param log
     *         logger that is used to report the progress
     */
    public AggregatedScore(final String configuration, final FilteredLog log) {
        this.log = log;

        JsonNode jsonNode = parseConfiguration(configuration);
        analysisConfiguration = AnalysisConfiguration.from(jsonNode);
        testsConfiguration = TestConfiguration.from(jsonNode);
        coverageConfiguration = CoverageConfiguration.from(jsonNode);
        pitConfiguration = PitConfiguration.from(jsonNode);
    }

    private JsonNode parseConfiguration(final String configuration) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(configuration);
        }
        catch (JsonProcessingException exception) {
            log.logError("Invalid JSON configuration: " + configuration);

            return objectMapper.createObjectNode();
        }
    }

    public boolean isEnabled() {
        return analysisConfiguration.isEnabled()
                && testsConfiguration.isEnabled()
                && coverageConfiguration.isEnabled()
                && pitConfiguration.isEnabled();
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
        return analysisAchieved + testAchieved + coverageAchieved + pitAchieved;
    }

    /**
     * Returns the total number of points that could be achieved.
     *
     * @return the total number of points that could be achieved
     */
    public int getTotal() {
        return analysisConfiguration.getMaxScore() + testsConfiguration.getMaxScore()
                + coverageConfiguration.getMaxScore() + pitConfiguration.getMaxScore();
    }

    public int getAnalysisAchieved() {
        return analysisAchieved;
    }

    public int getAnalysisRatio() {
        return getRatio(analysisConfiguration.getMaxScore(), getAnalysisAchieved());
    }

    public int getTestAchieved() {
        return testAchieved;
    }

    public int getTestRatio() {
        return getRatio(testsConfiguration.getMaxScore(), getTestAchieved());
    }

    public int getCoverageAchieved() {
        return coverageAchieved;
    }

    public int getCoverageRatio() {
        return getRatio(coverageConfiguration.getMaxScore(), getCoverageAchieved());
    }

    public int getPitAchieved() {
        return pitAchieved;
    }

    public int getPitRatio() {
        return getRatio(pitConfiguration.getMaxScore(), getPitAchieved());
    }

    /**
     * Returns the success ratio, i.e. number of achieved points divided by total points.
     *
     * @return the success ration
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
     * Returns whether at least one unit test failure has been recorded. In such a case PIT mutation results will not
     * be available. 
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

    public AnalysisConfiguration getAnalysisConfiguration() {
        return analysisConfiguration;
    }

    public List<AnalysisScore> getAnalysisScores() {
        return analysisScores;
    }

    public TestConfiguration getTestConfiguration() {
        return testsConfiguration;
    }

    public List<TestScore> getTestScores() {
        return testScores;
    }

    public CoverageConfiguration getCoverageConfiguration() {
        return coverageConfiguration;
    }

    public List<CoverageScore> getCoverageScores() {
        return coverageScores;
    }

    public PitConfiguration getPitConfiguration() {
        return pitConfiguration;
    }

    public List<PitScore> getPitScores() {
        return pitScores;
    }

    /**
     * Adds the specified static analysis grading scores.
     *
     * @param supplier
     *         the scores to take into account
     *
     * @return the total score impact (limited by the {@code maxScore} parameter of the configuration)
     */
    public int addAnalysisScores(final AnalysisSupplier supplier) {
        TypeScore<AnalysisScore> score = addScores(supplier, analysisConfiguration, "static analysis results");
        analysisAchieved = score.getTotal();
        analysisScores = score.getScores();
        return analysisAchieved;
    }

    /**
     * Adds the specified code coverage grading scores.
     *
     * @param supplier
     *         the scores to take into account
     *
     * @return the total score impact (limited by the {@code maxScore} parameter of the configuration)
     */
    public int addCoverageScores(final CoverageSupplier supplier) {
        TypeScore<CoverageScore> score = addScores(supplier, coverageConfiguration, "code coverage results");
        coverageAchieved = score.getTotal();
        coverageScores = score.getScores();
        return coverageAchieved;
    }

    /**
     * Adds the specified PIT mutation coverage grading scores.
     *
     * @param supplier
     *         the scores to take into account
     *
     * @return the total score impact (limited by the {@code maxScore} parameter of the configuration)
     */
    public int addPitScores(final PitSupplier supplier) {
        TypeScore<PitScore> score = addScores(supplier, pitConfiguration, "mutation coverage results");
        pitAchieved = score.getTotal();
        pitScores = score.getScores();
        return pitAchieved;
    }

    /**
     * Adds the specified test grading scores.
     *
     * @param supplier
     *         the scores to take into account
     *
     * @return the total score impact (limited by the {@code maxScore} parameter of the configuration)
     */
    public int addTestScores(final TestSupplier supplier) {
        TypeScore<TestScore> score = addScores(supplier, testsConfiguration, "test results");
        testAchieved = score.getTotal();
        testScores = score.getScores();
        return testAchieved;
    }

    private <C extends Configuration, S extends Score> TypeScore<S> addScores(
            final Supplier<C, S> supplier, final C configuration, final String displayName) {
        if (configuration.isDisabled()) {
            log.logInfo("Skipping " + displayName);
            return new TypeScore<>(0, Collections.emptyList());
        }
        else {
            log.logInfo("Grading " + displayName);

            List<S> scores = supplier.createScores(configuration);
            if (scores.isEmpty()) {
                log.logError("-> Scoring of %s has been enabled, but no results have been found.", displayName);
                return new TypeScore<>(0, scores);
            }
            else {
                int total = computeScore(configuration.getMaxScore(), aggregateDelta(scores));
                supplier.log(scores, log);
                log.logInfo("Total score for %s: %d of %d", displayName, total, configuration.getMaxScore());
                return new TypeScore<>(total, scores);
            }
        }
    }

    private int computeScore(final int maxScore, final int totalImpact) {
        if (totalImpact <= 0) {
            return Math.max(0, maxScore + totalImpact);
        }
        else {
            return Math.min(maxScore, totalImpact);
        }
    }

    private int aggregateDelta(final List<? extends Score> scores) {
        int delta = 0;
        for (Score score : scores) {
            delta = delta + score.getTotalImpact();
        }
        return delta;
    }

    @Override @Generated
    public String toString() {
        return String.format("Score: %d / %d", getAchieved(), getTotal());
    }

    @Override @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AggregatedScore that = (AggregatedScore) o;
        return analysisAchieved == that.analysisAchieved
                && testAchieved == that.testAchieved
                && coverageAchieved == that.coverageAchieved
                && pitAchieved == that.pitAchieved
                && analysisConfiguration.equals(that.analysisConfiguration)
                && analysisScores.equals(that.analysisScores)
                && testsConfiguration.equals(that.testsConfiguration)
                && testScores.equals(that.testScores)
                && coverageConfiguration.equals(that.coverageConfiguration)
                && coverageScores.equals(that.coverageScores)
                && pitConfiguration.equals(that.pitConfiguration)
                && pitScores.equals(that.pitScores);
    }

    @Override @Generated
    public int hashCode() {
        return Objects.hash(analysisConfiguration, analysisScores, analysisAchieved, testsConfiguration, testScores,
                testAchieved, coverageConfiguration, coverageScores, coverageAchieved, pitConfiguration, pitScores,
                pitAchieved);
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
