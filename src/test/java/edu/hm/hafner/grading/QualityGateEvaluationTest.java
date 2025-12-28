package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.grading.QualityGate.Criticality;
import edu.hm.hafner.grading.QualityGateResult.OverallStatus;
import edu.hm.hafner.util.FilteredLog;

import java.util.List;
import java.util.NoSuchElementException;
import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.grading.assertions.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link QualityGateEvaluation}.
 */
@DefaultLocale("en")
class QualityGateEvaluationTest {
    private static final String LINE_COVERAGE_NAME = "Line Coverage";
    private static final String LINE_METRIC = "line";
    private static final Scope SCOPE = Scope.PROJECT;
    private static final String BRANCH_COVERAGE_NAME = "Branch Coverage";
    private static final String BRANCH_METRIC = "branch";

    @Test
    void shouldPassWhenCoverageAboveThreshold() {
        var log = new FilteredLog("Test");

        var qualityGate = new QualityGate(LINE_COVERAGE_NAME, LINE_METRIC, SCOPE,
                80.0, Criticality.FAILURE);

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble(LINE_METRIC, SCOPE)).thenReturn(85.0);

        var result = QualityGateResult.evaluate(statistics, List.of(qualityGate), log);

        assertThat(result).isSuccessful()
                .hasSuccessCount(1).hasFailureCount(0)
                .hasOverallStatus(OverallStatus.SUCCESS);

        var evaluation = result.getEvaluations().getFirst();
        assertThat(evaluation).isPassed()
                .hasActualValue(85.0)
                .hasCriticality(Criticality.FAILURE)
                .hasMessage("Line Coverage: **85.00** >= 80.00")
                .hasGateName(qualityGate.getName())
                .hasMetric(qualityGate.getMetric())
                .hasThreshold(qualityGate.getThreshold())
                .hasQualityGate(qualityGate);

        assertThat(log.getInfoMessages()).map(String::strip)
                .containsSubsequence("Evaluating 1 quality gate(s)",
                        "Quality gates evaluation completed: ✅ SUCCESS",
                        "Passed: 1, Failed: 0",
                        "✅ Line Coverage: **85.00** >= 80.00");
    }

    @Test
    void shouldFailWhenCoverageBelowThreshold() {
        var log = new FilteredLog("Test");

        var qualityGate = new QualityGate(LINE_COVERAGE_NAME, LINE_METRIC, SCOPE,
                80.0, Criticality.FAILURE);

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble(LINE_METRIC, SCOPE)).thenReturn(75.0);

        var result = QualityGateResult.evaluate(statistics, List.of(qualityGate), log);

        assertThat(result).isNotSuccessful()
                .hasOverallStatus(OverallStatus.FAILURE)
                .hasSuccessCount(0)
                .hasFailureCount(1);

        var evaluation = result.getEvaluations().get(0);
        assertThat(evaluation).isNotPassed()
                .hasActualValue(75.0)
                .hasCriticality(Criticality.FAILURE)
                .hasMessage("Line Coverage: **75.00** >= 80.00")
                .hasGateName(qualityGate.getName())
                .hasMetric(qualityGate.getMetric())
                .hasThreshold(qualityGate.getThreshold())
                .hasQualityGate(qualityGate);

        assertThat(log.getInfoMessages()).map(String::strip)
                .containsSubsequence("Evaluating 1 quality gate(s)",
                        "Quality gates evaluation completed: ❌ FAILURE",
                        "Passed: 0, Failed: 1",
                        "❌ Line Coverage: **75.00** >= 80.00");
    }

    @Test
    void shouldThrowExceptionWhenValueIsMissing() {
        var log = new FilteredLog("Test");

        var qualityGate = new QualityGate(LINE_COVERAGE_NAME, LINE_METRIC, SCOPE,
                80.0, Criticality.FAILURE);

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble(LINE_METRIC, SCOPE))
                .thenThrow(new NoSuchElementException("Nothing there"));

        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
                () -> QualityGateResult.evaluate(statistics, List.of(qualityGate), log));
    }

    @Test
    void shouldReturnCorrectCountsForEmptyGates() {
        var log = new FilteredLog("Test");

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble(LINE_METRIC, SCOPE)).thenReturn(85.0);

        var result = QualityGateResult.evaluate(statistics, List.of(), log);

        assertThat(result).isSuccessful()
                .hasSuccessCount(0)
                .hasFailureCount(0)
                .hasOverallStatus(OverallStatus.SUCCESS);

        assertThat(log.getInfoMessages()).map(String::strip)
                .containsSubsequence("No quality gates to evaluate");
    }

    @Test
    void shouldHandleMultipleCoverageMetrics() {
        var log = new FilteredLog("Test");

        var qualityGates = List.of(
                new QualityGate(LINE_COVERAGE_NAME, LINE_METRIC, SCOPE,
                        80.0, Criticality.FAILURE),
                new QualityGate(BRANCH_COVERAGE_NAME, BRANCH_METRIC, SCOPE,
                        60.0, Criticality.UNSTABLE)
        );

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble(LINE_METRIC, SCOPE)).thenReturn(85.0);
        when(statistics.asDouble(BRANCH_METRIC, SCOPE)).thenReturn(70.0);

        var result = QualityGateResult.evaluate(statistics, qualityGates, log);

        assertThat(result).isSuccessful()
                .hasSuccessCount(2)
                .hasFailureCount(0)
                .hasOverallStatus(OverallStatus.SUCCESS);

        assertThat(log.getInfoMessages()).map(String::strip)
                .containsSubsequence("Evaluating 2 quality gate(s)",
                        "Quality gates evaluation completed: ✅ SUCCESS",
                        "Passed: 2, Failed: 0",
                        "✅ Line Coverage: **85.00** >= 80.00",
                        "✅ Branch Coverage: **70.00** >= 60.00");
    }

    @Test
    void shouldHandleMultipleScopes() {
        var log = new FilteredLog("Test");

        var qualityGates = List.of(
                new QualityGate("Line Coverage - Whole Project", LINE_METRIC, Scope.PROJECT,
                        80.0, Criticality.UNSTABLE),
                new QualityGate("Line Coverage - Modified Files", LINE_METRIC, Scope.MODIFIED_FILES,
                        70.0, Criticality.UNSTABLE),
                new QualityGate("Line Coverage - Changed Code", LINE_METRIC, Scope.MODIFIED_LINES,
                        60.0, Criticality.UNSTABLE)
        );

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble(LINE_METRIC, Scope.PROJECT)).thenReturn(85.0);
        when(statistics.asDouble(LINE_METRIC, Scope.MODIFIED_FILES)).thenReturn(75.0);
        when(statistics.asDouble(LINE_METRIC, Scope.MODIFIED_LINES)).thenReturn(65.0);

        var result = QualityGateResult.evaluate(statistics, qualityGates, log);

        assertThat(result).isSuccessful()
                .hasSuccessCount(3)
                .hasFailureCount(0)
                .hasOverallStatus(OverallStatus.SUCCESS);

        assertThat(log.getInfoMessages()).map(String::strip)
                .containsSubsequence("Evaluating 3 quality gate(s)",
                        "Quality gates evaluation completed: ✅ SUCCESS",
                        "Passed: 3, Failed: 0",
                        "✅ Line Coverage - Whole Project: **85.00** >= 80.00",
                        "✅ Line Coverage - Modified Files: **75.00** >= 70.00",
                        "✅ Line Coverage - Changed Code: **65.00** >= 60.00");
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(QualityGateEvaluation.class).verify();
    }
}
