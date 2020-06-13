package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.grading.assertions.Assertions.*;

/**
 * Tests the class {@link PitScore}.
 *
 * @author Eva-Maria Zeintl
 * @author Ullrich Hafner
 * @author Kevin Richter
 * @author Thomas Großbeck
 */
class PitScoreTest {
    private static final String NAME = "pit";

    @Test
    void shouldInitialiseConfigurationWithJson() {
        PitConfiguration pitConfiguration = PitConfiguration.from(
                "{\"maxScore\": 50, \"undetectedImpact\":-2, \"detectedImpact\":1, \"undetectedPercentageImpact\":-1, \"detectedPercentageImpact\":-3}");

        assertThat(pitConfiguration).hasMaxScore(50);
        assertThat(pitConfiguration).hasUndetectedImpact(-2);
        assertThat(pitConfiguration).hasDetectedImpact(1);
        assertThat(pitConfiguration).hasUndetectedPercentageImpact(-1);
        assertThat(pitConfiguration).hasDetectedPercentageImpact(-3);
    }

    @Test
    void shouldInitialiseConfigurationWithDefaultValues() {
        PitConfiguration pitConfiguration = PitConfiguration.from("{}");

        assertThat(pitConfiguration).hasMaxScore(0);
        assertThat(pitConfiguration).hasUndetectedImpact(0);
        assertThat(pitConfiguration).hasDetectedImpact(0);
        assertThat(pitConfiguration).hasUndetectedPercentageImpact(0);
        assertThat(pitConfiguration).hasDetectedPercentageImpact(0);
    }

    @Test
    void shouldInitialiseConfigurationWithJsonIgnoresAdditionalAttributes() {
        PitConfiguration pitConfiguration = PitConfiguration.from(
                "{\"maxScore\": 50, \"undetectedImpact\":-2, \"detectedImpact\":1, \"undetectedPercentageImpact\":-1, \"detectedPercentageImpact\":2, \"additionalAttribute\":10}");

        assertThat(pitConfiguration).hasMaxScore(50);
        assertThat(pitConfiguration).hasUndetectedImpact(-2);
        assertThat(pitConfiguration).hasDetectedImpact(1);
        assertThat(pitConfiguration).hasUndetectedPercentageImpact(-1);
        assertThat(pitConfiguration).hasDetectedPercentageImpact(2);
    }

    @Test
    void shouldCalculateSizeImpacts() {
        PitConfiguration pitConfiguration = new PitConfiguration.PitConfigurationBuilder().setMaxScore(25)
                .setUndetectedImpact(-2)
                .setDetectedImpact(1)
                .build();

        PitScore pits = new PitScore.PitScoreBuilder().withDisplayName(NAME)
                .withConfiguration(pitConfiguration)
                .withTotalMutations(30)
                .withUndetectedMutations(5)
                .build();

        assertThat(pits).hasTotalImpact(15);
    }

    @Test
    void shouldCalculateRatioImpacts() {
        PitConfiguration pitConfiguration = new PitConfiguration.PitConfigurationBuilder().setMaxScore(25)
                .setUndetectedPercentageImpact(-2)
                .build();

        PitScore pits = new PitScore.PitScoreBuilder().withDisplayName(NAME)
                .withConfiguration(pitConfiguration)
                .withTotalMutations(30)
                .withUndetectedMutations(3)
                .build();

        assertThat(pits).hasTotalImpact(-20);
    }

    @Test
    void shouldCalculateNegativeResult() {
        PitConfiguration pitConfiguration = new PitConfiguration.PitConfigurationBuilder().setMaxScore(25)
                .setUndetectedImpact(-2)
                .setDetectedImpact(1)
                .build();

        PitScore pits = new PitScore.PitScoreBuilder().withDisplayName(NAME)
                .withConfiguration(pitConfiguration)
                .withTotalMutations(30)
                .withUndetectedMutations(20)
                .build();

        assertThat(pits).hasTotalImpact(-30);
    }

    @Test
    void shouldCalculateZeroTotalImpact() {
        PitConfiguration pitConfiguration = new PitConfiguration.PitConfigurationBuilder().setMaxScore(25).build();

        PitScore pits = new PitScore.PitScoreBuilder().withDisplayName(NAME)
                .withConfiguration(pitConfiguration)
                .withTotalMutations(30)
                .withUndetectedMutations(20)
                .build();

        assertThat(pits).hasTotalImpact(0);
    }

    @Test
    void shouldGetProperties() {
        PitConfiguration pitConfiguration = new PitConfiguration.PitConfigurationBuilder().setMaxScore(100)
                .setUndetectedImpact(-1)
                .setDetectedImpact(1)
                .build();
        PitScore pits = new PitScore.PitScoreBuilder().withDisplayName(NAME)
                .withConfiguration(pitConfiguration)
                .withTotalMutations(100)
                .withUndetectedMutations(25)
                .build();

        assertThat(pits).hasId(PitScore.ID);
        assertThat(pits).hasName(NAME);
        assertThat(pits).hasTotalImpact(50);
        assertThat(pits).hasMutationsSize(100);
        assertThat(pits).hasDetectedSize(75);
        assertThat(pits).hasUndetectedSize(25);
        assertThat(pits).hasUndetectedPercentage(25);
        assertThat(pits).hasDetectedPercentage(75);
    }
}
