package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.grading.QualityGate.Criticality;
import edu.hm.hafner.util.FilteredLog;

import java.util.List;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.grading.assertions.Assertions.*;

/**
 * Tests for {@link QualityGateEvaluation}.
 */
@DefaultLocale("en")
class QualityGateEvaluationTest {
    private static final FilteredLog LOG = new FilteredLog("Test");

    @Test
    void shouldPassWhenCoverageAboveThreshold() {
        var metrics = Map.of("line", 85);
        var qualityGate = new QualityGate("Line Coverage", "line", 80.0, Criticality.FAILURE);

        var log = new FilteredLog("Test");
        var result = QualityGateResult.evaluate(metrics, List.of(qualityGate), log);

        assertThat(result).isSuccessful()
                .hasSuccessCount(1).hasFailureCount(0)
                .hasOverallStatus(QualityGateResult.OverallStatus.SUCCESS);

        var evaluation = result.getEvaluations().get(0);
        assertThat(evaluation).isPassed()
                .hasActualValue(85.0)
                .hasCriticality(QualityGate.Criticality.FAILURE)
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
        var metrics = Map.of("line", 75);
        var qualityGate = new QualityGate("Line Coverage", "line", 80.0, Criticality.FAILURE);

        var log = new FilteredLog("Test");
        var result = QualityGateResult.evaluate(metrics, List.of(qualityGate), log);

        assertThat(result).isNotSuccessful()
                .hasOverallStatus(QualityGateResult.OverallStatus.FAILURE)
                .hasSuccessCount(0)
                .hasFailureCount(1);

        var evaluation = result.getEvaluations().get(0);
        assertThat(evaluation).isNotPassed()
                .hasActualValue(75.0)
                .hasCriticality(QualityGate.Criticality.FAILURE)
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
        var metrics = Map.of("line", 85);

        var log = new FilteredLog("Test");
        var result = QualityGateResult.evaluate(metrics, List.of(), log);

        assertThat(result).isSuccessful()
                .hasSuccessCount(0)
                .hasFailureCount(0)
                .hasOverallStatus(QualityGateResult.OverallStatus.SUCCESS);

        assertThat(log.getInfoMessages()).map(String::strip)
                .containsSubsequence("No quality gates to evaluate");
    }

    @Test
    void shouldHandleMultipleCoverageMetrics() {
        var metrics = Map.of(
                "line", 85,
                "branch", 70
        );

        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE),
                new QualityGate("Branch Coverage", "branch", 60.0, QualityGate.Criticality.UNSTABLE)
        );

        var log = new FilteredLog("Test");
        var result = QualityGateResult.evaluate(metrics, qualityGates, log);

        assertThat(result).isSuccessful()
                .hasSuccessCount(2)
                .hasFailureCount(0)
                .hasOverallStatus(QualityGateResult.OverallStatus.SUCCESS);

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
