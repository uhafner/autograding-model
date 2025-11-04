package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.grading.QualityGate.Criticality;
import edu.hm.hafner.grading.QualityGateResult.OverallStatus;
import edu.hm.hafner.util.FilteredLog;

import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.grading.assertions.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link QualityGateEvaluation}.
 */
@DefaultLocale("en")
class QualityGateEvaluationTest {
    @Test
    void shouldPassWhenCoverageAboveThreshold() {
        var log = new FilteredLog("Test");

        var qualityGate = new QualityGate("Line Coverage", "line", 80.0, Criticality.FAILURE);

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble("line")).thenReturn(85.0);

        var result = QualityGateResult.evaluate(statistics, List.of(qualityGate), log);

        assertThat(result).isSuccessful()
                .hasSuccessCount(1).hasFailureCount(0)
                .hasOverallStatus(OverallStatus.SUCCESS);

        var evaluation = result.getEvaluations().get(0);
        assertThat(evaluation).isPassed()
                .hasActualValue(85.0)
                .hasCriticality(Criticality.FAILURE)
                .hasMessage("Line Coverage: 85.00 >= 80.00")
                .hasGateName(qualityGate.getName())
                .hasMetric(qualityGate.getMetric())
                .hasThreshold(qualityGate.getThreshold())
                .hasQualityGate(qualityGate);

        assertThat(log.getInfoMessages()).map(String::strip)
                .containsSubsequence("Evaluating 1 quality gate(s)",
                        "Quality gates evaluation completed: ✅ SUCCESS",
                        "Passed: 1, Failed: 0",
                        "✅ Line Coverage: 85.00 >= 80.00");
    }

    @Test
    void shouldFailWhenCoverageBelowThreshold() {
        var log = new FilteredLog("Test");

        var qualityGate = new QualityGate("Line Coverage", "line", 80.0, Criticality.FAILURE);

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble("line")).thenReturn(75.0);

        var result = QualityGateResult.evaluate(statistics, List.of(qualityGate), log);

        assertThat(result).isNotSuccessful()
                .hasOverallStatus(OverallStatus.FAILURE)
                .hasSuccessCount(0)
                .hasFailureCount(1);

        var evaluation = result.getEvaluations().get(0);
        assertThat(evaluation).isNotPassed()
                .hasActualValue(75.0)
                .hasCriticality(Criticality.FAILURE)
                .hasMessage("Line Coverage: 75.00 >= 80.00")
                .hasGateName(qualityGate.getName())
                .hasMetric(qualityGate.getMetric())
                .hasThreshold(qualityGate.getThreshold())
                .hasQualityGate(qualityGate);

        assertThat(log.getInfoMessages()).map(String::strip)
                .containsSubsequence("Evaluating 1 quality gate(s)",
                        "Quality gates evaluation completed: ❌ FAILURE",
                        "Passed: 0, Failed: 1",
                        "❌ Line Coverage: 75.00 >= 80.00");
    }

    @Test
    void shouldReturnCorrectCountsForEmptyGates() {
        var log = new FilteredLog("Test");

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble("line")).thenReturn(85.0);

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
                new QualityGate("Line Coverage", "line", 80.0, Criticality.FAILURE),
                new QualityGate("Branch Coverage", "branch", 60.0, Criticality.UNSTABLE)
        );

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble("line")).thenReturn(85.0);
        when(statistics.asDouble("branch")).thenReturn(70.0);

        var result = QualityGateResult.evaluate(statistics, qualityGates, log);

        assertThat(result).isSuccessful()
                .hasSuccessCount(2)
                .hasFailureCount(0)
                .hasOverallStatus(OverallStatus.SUCCESS);

        assertThat(log.getInfoMessages()).map(String::strip)
                .containsSubsequence("Evaluating 2 quality gate(s)",
                        "Quality gates evaluation completed: ✅ SUCCESS",
                        "Passed: 2, Failed: 0",
                        "✅ Line Coverage: 85.00 >= 80.00",
                        "✅ Branch Coverage: 70.00 >= 60.00");
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(QualityGateEvaluation.class).verify();
    }
}
