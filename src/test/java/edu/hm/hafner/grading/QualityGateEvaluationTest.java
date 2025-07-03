package edu.hm.hafner.grading;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link QualityGateEvaluation}.
 */
class QualityGateEvaluationTest {
    private static final FilteredLog LOG = new FilteredLog("Test");

    @Test
    void shouldPassWhenCoverageAboveThreshold() {
        var metrics = Map.of("line", 85);
        var qualityGates = List.of(
            new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE)
        );
        
        var result = QualityGateResult.evaluate(metrics, qualityGates, LOG);
        
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(0);
        
        var evaluation = result.getEvaluations().get(0);
        assertThat(evaluation.isPassed()).isTrue();
        assertThat(evaluation.getActualValue()).isEqualTo(85.0);
        assertThat(evaluation.getCriticality()).isEqualTo(QualityGate.Criticality.FAILURE);
        assertThat(evaluation.getMessage()).contains("Line Coverage: 85.00 >= 80.00");
    }

    @Test
    void shouldFailWhenCoverageBelowThreshold() {
        var metrics = Map.of("line", 75);
        var qualityGates = List.of(
            new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE)
        );
        
        var result = QualityGateResult.evaluate(metrics, qualityGates, LOG);
        
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(1);
        
        var evaluation = result.getEvaluations().get(0);
        assertThat(evaluation.isPassed()).isFalse();
        assertThat(evaluation.getActualValue()).isEqualTo(75.0);
        assertThat(evaluation.getCriticality()).isEqualTo(QualityGate.Criticality.FAILURE);
        assertThat(evaluation.getMessage()).contains("Line Coverage: 75.00 >= 80.00");
    }

    @Test
    void shouldReturnCorrectCountsForEmptyGates() {
        var metrics = Map.of("line", 85);
        
        var result = QualityGateResult.evaluate(metrics, List.of(), LOG);
        
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getEvaluations()).isEmpty();
        assertThat(result.isSuccessful()).isTrue();
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
        
        var result = QualityGateResult.evaluate(metrics, qualityGates, LOG);
        
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.OverallStatus.SUCCESS);
    }

    @Test
    void shouldProvideQualityGateReference() {
        var metrics = Map.of("line", 85);
        var gate = new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE);
        var qualityGates = List.of(gate);
        
        var result = QualityGateResult.evaluate(metrics, qualityGates, LOG);
        var evaluation = result.getEvaluations().get(0);
        
        assertThat(evaluation.getQualityGate()).isEqualTo(gate);
        assertThat(evaluation.getQualityGate().getName()).isEqualTo("Line Coverage");
        assertThat(evaluation.getQualityGate().getMetric()).isEqualTo("line");
        assertThat(evaluation.getQualityGate().getThreshold()).isEqualTo(80.0);
        assertThat(evaluation.getQualityGate().getCriticality()).isEqualTo(QualityGate.Criticality.FAILURE);
    }

    @Test
    void shouldHandleNullQualityGatesList() {
        var metrics = Map.of("line", 85);
        
        var result = QualityGateResult.evaluate(metrics, null, LOG);
        
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getEvaluations()).isEmpty();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.OverallStatus.SUCCESS);
    }
}