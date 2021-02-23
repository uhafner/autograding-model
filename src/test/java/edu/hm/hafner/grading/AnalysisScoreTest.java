package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.grading.AnalysisConfiguration.AnalysisConfigurationBuilder;

import static edu.hm.hafner.grading.assertions.Assertions.*;

/**
 * Tests the class {@link AnalysisScore}.
 *
 * @author Eva-Maria Zeintl
 * @author Ullrich Hafner
 * @author Andreas Stiglmeier
 * @author Andreas Riepl
 * @author Oliver Scholz
 */
class AnalysisScoreTest {
    private static final String NAME = "Results";
    private static final String ID = "result-id";

    @Test
    void shouldCalculate() {
        AnalysisConfiguration analysisConfiguration = new AnalysisConfigurationBuilder()
                .setErrorImpact(-4)
                .setHighImpact(-3)
                .setNormalImpact(-2)
                .setLowImpact(-1)
                .build();
        AnalysisScore analysisScore = new AnalysisScore.AnalysisScoreBuilder().withId(ID)
                .withDisplayName(NAME)
                .withConfiguration(analysisConfiguration)
                .withTotalErrorsSize(2)
                .withTotalHighSeveritySize(2)
                .withTotalNormalSeveritySize(2)
                .withTotalLowSeveritySize(2)
                .build();
        assertThat(analysisScore).hasTotalImpact(2 * -4 - 2 * 3 - 2 * 2 - 2 * 1);
    }

    @Test
    void shouldConvertFromJson() {
        AnalysisConfiguration configuration = AnalysisConfiguration.from(
                "{\"maxScore\":5,\"errorImpact\":1,\"highImpact\":2,\"normalImpact\":3,\"lowImpact\":4}");
        assertThat(configuration).hasErrorImpact(1);
        assertThat(configuration).hasHighImpact(2);
        assertThat(configuration).hasNormalImpact(3);
        assertThat(configuration).hasLowImpact(4);
        assertThat(configuration).hasMaxScore(5);
    }

    @Test
    void shouldReturnPositiveParams() {
        AnalysisScore analysisScore = new AnalysisScore.AnalysisScoreBuilder().withId(ID)
                .withDisplayName(NAME)
                .withConfiguration(createConfigurationWithOnePointForEachSeverity())
                .withTotalErrorsSize(3)
                .withTotalHighSeveritySize(5)
                .withTotalNormalSeveritySize(2)
                .withTotalLowSeveritySize(4)
                .build();

        assertThat(analysisScore.getErrorsSize()).isEqualTo(3);
        assertThat(analysisScore).hasErrorsSize(3);
        assertThat(analysisScore).hasHighSeveritySize(5);
        assertThat(analysisScore).hasNormalSeveritySize(2);
        assertThat(analysisScore).hasLowSeveritySize(4);
        assertThat(analysisScore).hasTotalSize(14);
        assertThat(analysisScore).hasName(NAME);
        assertThat(analysisScore).hasId(ID);
    }

    @Test
    void shouldReturnNegativeParams() {
        AnalysisScore analysisScore = new AnalysisScore.AnalysisScoreBuilder().withId(ID)
                .withDisplayName(NAME)
                .withConfiguration(createConfigurationWithOnePointForEachSeverity())
                .withTotalErrorsSize(-3)
                .withTotalHighSeveritySize(-5)
                .withTotalNormalSeveritySize(-2)
                .withTotalLowSeveritySize(-4)
                .build();

        assertThat(analysisScore.getErrorsSize()).isEqualTo(-3);
        assertThat(analysisScore).hasErrorsSize(-3);
        assertThat(analysisScore).hasHighSeveritySize(-5);
        assertThat(analysisScore).hasNormalSeveritySize(-2);
        assertThat(analysisScore).hasLowSeveritySize(-4);
        assertThat(analysisScore).hasTotalSize(-14);
        assertThat(analysisScore).hasName(NAME);
        assertThat(analysisScore).hasId(ID);
    }

    private AnalysisConfiguration createConfigurationWithOnePointForEachSeverity() {
        return new AnalysisConfigurationBuilder()
                .setErrorImpact(1)
                .setHighImpact(1)
                .setNormalImpact(1)
                .setLowImpact(1)
                .build();
    }

    @Test
    void shouldComputeImpactBySizeZero() {
        AnalysisConfiguration configuration = new AnalysisConfigurationBuilder()
                .setErrorImpact(100)
                .setHighImpact(100)
                .setNormalImpact(100)
                .setLowImpact(100)
                .build();

        AnalysisScore score = new AnalysisScore.AnalysisScoreBuilder().withId(ID)
                .withDisplayName(NAME)
                .withConfiguration(configuration)
                .withTotalErrorsSize(0)
                .withTotalHighSeveritySize(0)
                .withTotalNormalSeveritySize(0)
                .withTotalLowSeveritySize(0)
                .build();
        assertThat(score).hasTotalImpact(0);
    }
}
