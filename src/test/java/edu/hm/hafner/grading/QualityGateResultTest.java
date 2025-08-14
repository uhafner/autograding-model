package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link QualityGateResult}.
 */
class QualityGateResultTest {
    private static final FilteredLog LOG = new FilteredLog("Test");

    @Test
    void shouldCreateEmptyResult() {
        var result = new QualityGateResult();

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.OverallStatus.SUCCESS);
        assertThat(result.getEvaluations()).isEmpty();
        assertThat(result.hasFailures()).isFalse();
    }

    @Test
    void shouldEvaluateAllPassingGates() {
        var metrics = Map.of(
                "line", 85,
                "branch", 70
        );

        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE),
                new QualityGate("Branch Coverage", "branch", 65.0, QualityGate.Criticality.UNSTABLE)
        );

        var result = QualityGateResult.evaluate(metrics, qualityGates, LOG);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.OverallStatus.SUCCESS);
        assertThat(result.hasFailures()).isFalse();
        assertThat(result.getEvaluations()).hasSize(2);

        // Verify individual evaluations
        var evaluations = result.getEvaluations();
        assertThat(evaluations.get(0).isPassed()).isTrue();
        assertThat(evaluations.get(1).isPassed()).isTrue();
    }

    @Test
    void shouldHandleFailureGates() {
        var metrics = Map.of("line", 75);

        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE)
        );

        var result = QualityGateResult.evaluate(metrics, qualityGates, LOG);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.OverallStatus.FAILURE);
        assertThat(result.hasFailures()).isTrue();

        assertThat(result.getEvaluations().get(0).isPassed()).isFalse();
        assertThat(result.getEvaluations().get(0).getCriticality()).isEqualTo(QualityGate.Criticality.FAILURE);
    }

    @Test
    void shouldHandleUnstableGates() {
        var metrics = Map.of("line", 75);

        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.UNSTABLE)
        );

        var result = QualityGateResult.evaluate(metrics, qualityGates, LOG);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.OverallStatus.UNSTABLE);
        assertThat(result.hasFailures()).isTrue();
    }

    @Test
    void shouldPrioritizeFailureOverUnstable() {
        var metrics = Map.of(
                "line", 75,
                "branch", 60
        );

        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE),
                new QualityGate("Branch Coverage", "branch", 65.0, QualityGate.Criticality.UNSTABLE)
        );

        var result = QualityGateResult.evaluate(metrics, qualityGates, LOG);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(2);
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.OverallStatus.FAILURE);
        assertThat(result.hasFailures()).isTrue();
    }

    @Test
    void shouldHandleMixedResults() {
        var metrics = Map.of(
                "line", 85,
                "branch", 60
        );

        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE),
                new QualityGate("Branch Coverage", "branch", 65.0, QualityGate.Criticality.UNSTABLE)
        );

        var result = QualityGateResult.evaluate(metrics, qualityGates, LOG);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.OverallStatus.UNSTABLE);
        assertThat(result.hasFailures()).isTrue();
    }

    @Test
    void shouldCreateMarkdownSummary() {
        var metrics = Map.of(
                "line", 85,
                "branch", 50
        );

        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE),
                new QualityGate("Branch Coverage", "branch", 60.0, QualityGate.Criticality.UNSTABLE)
        );

        var result = QualityGateResult.evaluate(metrics, qualityGates, LOG);
        var markdown = result.createMarkdownSummary();

        assertThat(markdown)
                .contains("Quality Gates")
                .contains("Overall Status:")
                .contains("UNSTABLE")
                .contains("Line Coverage: 85.00 >= 80.00")
                .contains("Branch Coverage: 50.00 >= 60.00");
    }

    @Test
    void shouldCreateEmptyMarkdownForNoGates() {
        var result = new QualityGateResult();
        var markdown = result.createMarkdownSummary();

        assertThat(markdown).isEmpty();
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        var evaluation1 = new QualityGateEvaluation(
                new QualityGate("Test", "line", 80.0, QualityGate.Criticality.FAILURE),
                85.0, true, "Passed");
        var evaluation2 = new QualityGateEvaluation(
                new QualityGate("Test", "line", 80.0, QualityGate.Criticality.FAILURE),
                85.0, true, "Passed");

        var result1 = new QualityGateResult(List.of(evaluation1));
        var result2 = new QualityGateResult(List.of(evaluation2));
        var result3 = new QualityGateResult();

        assertThat(result1).isEqualTo(result2);
        assertThat(result1).isNotEqualTo(result3);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void shouldImplementToString() {
        var metrics = Map.of("line", 85, "branch", 70);
        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE),
                new QualityGate("Branch Coverage", "branch", 65.0, QualityGate.Criticality.UNSTABLE)
        );

        var result = QualityGateResult.evaluate(metrics, qualityGates, LOG);

        assertThat(result.toString())
                .contains("SUCCESS")
                .contains("passed=2")
                .contains("failed=0");
    }
}
