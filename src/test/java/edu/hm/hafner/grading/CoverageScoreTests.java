package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.grading.CoverageConfiguration.CoverageConfigurationBuilder;

import static edu.hm.hafner.grading.assertions.Assertions.*;

/**
 * Tests the class {@link CoverageScore}.
 *
 * @author Eva-Maria Zeintl
 * @author Ullrich Hafner
 * @author Patrick Rogg
 * @author Johannes Hintermaier
 */
class CoverageScoreTests {
    private static final int PERCENTAGE = 99;

    @Test
    void shouldCalculateTotalImpactWithZeroCoveredImpact() {
        var coverageConfiguration = createCoverageConfiguration(-2, 0);
        var coverageScore = new CoverageScore.CoverageScoreBuilder().withId(StringUtils.lowerCase("Line"))
                .withDisplayName("Line")
                .withConfiguration(coverageConfiguration)
                .withCoveredPercentage(PERCENTAGE)
                .build();

        assertThat(coverageScore).hasTotalImpact(-2);
    }

    @Test
    void shouldCalculateTotalImpactWithZeroMissedImpact() {
        var coverageConfiguration = createCoverageConfiguration(0, 5);
        var coverageScore = new CoverageScore.CoverageScoreBuilder().withId(StringUtils.lowerCase("Line"))
                .withDisplayName("Line")
                .withConfiguration(coverageConfiguration)
                .withCoveredPercentage(PERCENTAGE)
                .build();

        assertThat(coverageScore).hasTotalImpact(495);
    }

    @Test
    void shouldCalculateTotalImpact() {
        var coverageConfiguration = createCoverageConfiguration(-1, 3);
        var coverageScore = new CoverageScore.CoverageScoreBuilder().withId(StringUtils.lowerCase("Line"))
                .withDisplayName("Line")
                .withConfiguration(coverageConfiguration)
                .withCoveredPercentage(PERCENTAGE)
                .build();

        assertThat(coverageScore).hasTotalImpact(296);
    }

    @Test
    void shouldGetProperties() {
        var coverageConfiguration = createCoverageConfiguration(1, 1);
        var coverageScore = new CoverageScore.CoverageScoreBuilder().withId(StringUtils.lowerCase("Line"))
                .withDisplayName("Line")
                .withConfiguration(coverageConfiguration)
                .withCoveredPercentage(PERCENTAGE)
                .build();

        assertThat(coverageScore).hasName("Line");
        assertThat(coverageScore).hasCoveredPercentage(PERCENTAGE);
        assertThat(coverageScore).hasMissedPercentage(100 - PERCENTAGE);
    }

    private CoverageConfiguration createCoverageConfiguration(final int missedImpact, final int coveredImpact) {
        return new CoverageConfigurationBuilder()
                .setMissedPercentageImpact(missedImpact)
                .setCoveredPercentageImpact(coveredImpact)
                .build();
    }

    @Test
    void shouldConvertFromJson() {
        var configuration = CoverageConfiguration.from(
                        "{\"enabled\": true, \"maxScore\": 4, \"coveredPercentageImpact\":5, \"missedPercentageImpact\":3}");
        assertThat(configuration).hasMaxScore(4);
        assertThat(configuration).hasCoveredPercentageImpact(5);
        assertThat(configuration).hasMissedPercentageImpact(3);
        assertThat(configuration).isEnabled();
    }

    @Test
    void shouldInitializeWithDefault() {
        var configurationEmpty = CoverageConfiguration.from("{}");
        assertThat(configurationEmpty).hasMaxScore(0);
        assertThat(configurationEmpty).hasCoveredPercentageImpact(0);
        assertThat(configurationEmpty).hasMissedPercentageImpact(0);

        var configurationOneValue = CoverageConfiguration.from(
                "{\"maxScore\": 4}");
        assertThat(configurationOneValue).hasMaxScore(4);
        assertThat(configurationOneValue).hasCoveredPercentageImpact(0);
        assertThat(configurationOneValue).hasMissedPercentageImpact(0);
    }

    @Test
    void shouldNotReadAdditionalAttributes() {
        var configuration = CoverageConfiguration.from(
                "{\"maxScore\": 2, \"coveredPercentageImpact\":3, \"missedPercentageImpact\":4, \"notRead\":5}");
        assertThat(configuration).hasMaxScore(2);
        assertThat(configuration).hasCoveredPercentageImpact(3);
        assertThat(configuration).hasMissedPercentageImpact(4);
    }
}
