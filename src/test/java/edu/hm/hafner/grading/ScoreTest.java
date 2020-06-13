package edu.hm.hafner.grading;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.grading.assertions.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link AggregatedScore}.
 *
 * @author Ullrich Hafner
 * @author Oliver Scholz
 */
class ScoreTest {
    @Test
    void shouldInitializeToZero() {
        AggregatedScore score = new AggregatedScore();

        assertThat(score).hasAchieved(0).hasTotal(0).hasRatio(100).isNotEnabled()
                .hasAnalysisAchieved(0).hasNoAnalysisScores()
                .hasTestAchieved(0).hasNoTestScores()
                .hasCoverageAchieved(0).hasNoTestScores()
                .hasPitAchieved(0).hasNoPitScores();

        score.addAnalysisScores(mock(AnalysisSupplier.class));
        assertThat(score)
                .hasInfoMessages("Skipping static analysis results")
                .hasTotal(0).hasAchieved(0).hasRatio(100).hasAnalysisAchieved(0).hasNoAnalysisScores();

        score.addTestScores(mock(TestSupplier.class));
        assertThat(score)
                .hasInfoMessages("Skipping test results")
                .hasTotal(0).hasAchieved(0).hasRatio(100).hasTestAchieved(0).hasNoTestScores();

        score.addCoverageScores(mock(CoverageSupplier.class));
        assertThat(score)
                .hasInfoMessages("Skipping code coverage results")
                .hasTotal(0).hasAchieved(0).hasRatio(100).hasCoverageAchieved(0).hasNoTestScores();

        score.addPitScores(mock(PitSupplier.class));
        assertThat(score)
                .hasInfoMessages("Skipping mutation coverage results")
                .hasTotal(0).hasAchieved(0).hasRatio(100).hasPitAchieved(0).hasNoPitScores();

    }

    @Test
    void shouldFindErrorWhenThereIsNoAnalysisResult() {
        AnalysisSupplier supplier = mock(AnalysisSupplier.class);
        AggregatedScore noActionScore = new AggregatedScore(
                "{\"analysis\": {\"maxScore\":5,\"errorImpact\":1,\"highImpact\":2,\"normalImpact\":3,\"lowImpact\":4}}");

        assertThat(noActionScore.addAnalysisScores(supplier)).isZero();
        assertThat(noActionScore)
                .hasInfoMessages("Grading static analysis results")
                .hasErrorMessages(
                        "-> Scoring of static analysis results has been enabled, but no results have been found.")
                .hasTotal(5).hasAchieved(0).hasRatio(0);
    }

    @Test
    void shouldScoreSingleAnalysisResult() {
        AggregatedScore aggregation = new AggregatedScore(
                "{\"analysis\": {\"maxScore\":100,\"errorImpact\":1,\"highImpact\":2,\"normalImpact\":3,\"lowImpact\":4}}");

        AnalysisSupplier supplier = mock(AnalysisSupplier.class);

        int impact = 25;

        AnalysisScore score = new AnalysisScore.AnalysisScoreBuilder().build();
        score.setTotalImpact(impact);
        when(supplier.createScores(any())).thenReturn(Collections.singletonList(score));

        assertThat(aggregation.addAnalysisScores(supplier)).isEqualTo(impact);

        assertThat(aggregation)
                .hasInfoMessages("Grading static analysis results")
                .hasInfoMessages("Total score for static analysis results: 25 of 100")
                .hasTotal(100).hasAchieved(impact).hasRatio(impact);
    }

    @Test
    void shouldScoreMultipleAnalysisResults() {
        AggregatedScore aggregation = new AggregatedScore(
                "{\"analysis\": {\"maxScore\":200,\"errorImpact\":1,\"highImpact\":2,\"normalImpact\":3,\"lowImpact\":4}}");

        AnalysisSupplier supplier = mock(AnalysisSupplier.class);

        AnalysisScore score50 = new AnalysisScore.AnalysisScoreBuilder().build();
        score50.setTotalImpact(50);
        AnalysisScore score100 = new AnalysisScore.AnalysisScoreBuilder().build();
        score100.setTotalImpact(100);

        when(supplier.createScores(any())).thenReturn(Arrays.asList(score50, score100));

        assertThat(aggregation.addAnalysisScores(supplier)).isEqualTo(150);

        assertThat(aggregation)
                .hasInfoMessages("Grading static analysis results")
                .hasInfoMessages("Total score for static analysis results: 150 of 200")
                .hasTotal(200).hasAchieved(150).hasRatio(75);
    }

    private AggregatedScore createScore() {
        return new AggregatedScore();
    }
}
