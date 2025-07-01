package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;

import java.util.List;

import static edu.hm.hafner.grading.assertions.Assertions.*;

/**
 * Tests for quality gate evaluation functionality
 *
 * @author Ullrich Hafner
 */
class QualityGateEvaluationTest {
    
    @Test
    void shouldEvaluateQualityGatesWithAllPassingGates() {
        var aggregation = createQualityAggregation();
        
        // Create quality gates that should all pass based on the metrics
        var qualityGates = List.of(
            new QualityGate("Line Coverage Gate", "line", 50.0, QualityGate.Operator.GREATER_THAN_OR_EQUAL, QualityGate.Criticality.FAILURE, true),
            new QualityGate("Branch Coverage Gate", "branch", 40.0, QualityGate.Operator.GREATER_THAN_OR_EQUAL, QualityGate.Criticality.UNSTABLE, true)
        );
        
        var result = aggregation.evaluateQualityGates(qualityGates);
        
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.Status.SUCCESS);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getEvaluations()).hasSize(2);
        
        assertThat(aggregation.shouldFailBuild(qualityGates)).isFalse();
        assertThat(aggregation.shouldMarkUnstable(qualityGates)).isFalse();
    }

    @Test
    void shouldEvaluateQualityGatesWithFailureGate() {
        var aggregation = createQualityAggregation();
        
        // Create a quality gate that should fail with FAILURE criticality
        var qualityGates = List.of(
            new QualityGate("Line Coverage Gate", "line", 95.0, QualityGate.Operator.GREATER_THAN_OR_EQUAL, QualityGate.Criticality.FAILURE, true)
        );
        
        var result = aggregation.evaluateQualityGates(qualityGates);
        
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.Status.FAILURE);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(1);
        
        var failedEvaluations = result.getFailedEvaluations();
        assertThat(failedEvaluations).hasSize(1);
        assertThat(failedEvaluations.get(0).getCriticality()).isEqualTo(QualityGate.Criticality.FAILURE);
        assertThat(failedEvaluations.get(0).isPassed()).isFalse();
        
        assertThat(aggregation.shouldFailBuild(qualityGates)).isTrue();
        assertThat(aggregation.shouldMarkUnstable(qualityGates)).isTrue();
    }

    @Test
    void shouldEvaluateQualityGatesWithUnstableCriticalities() {
        var aggregation = createQualityAggregation();
        
        // Test all criticalities that should result in UNSTABLE status
        var qualityGates = List.of(
            new QualityGate("Error Gate", "line", 95.0, QualityGate.Operator.GREATER_THAN_OR_EQUAL, QualityGate.Criticality.ERROR, true),
            new QualityGate("Unstable Gate", "branch", 95.0, QualityGate.Operator.GREATER_THAN_OR_EQUAL, QualityGate.Criticality.UNSTABLE, true),
            new QualityGate("Note Gate", "mutation", 95.0, QualityGate.Operator.GREATER_THAN_OR_EQUAL, QualityGate.Criticality.NOTE, true)
        );
        
        var result = aggregation.evaluateQualityGates(qualityGates);
        
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.Status.UNSTABLE);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(3);
        
        var failedEvaluations = result.getFailedEvaluations();
        assertThat(failedEvaluations).hasSize(3);
        assertThat(failedEvaluations).extracting(QualityGateEvaluation::getCriticality)
            .containsExactly(QualityGate.Criticality.ERROR, QualityGate.Criticality.UNSTABLE, QualityGate.Criticality.NOTE);
        
        assertThat(aggregation.shouldFailBuild(qualityGates)).isFalse();
        assertThat(aggregation.shouldMarkUnstable(qualityGates)).isTrue();
    }

    @Test
    void shouldHandleEmptyQualityGatesList() {
        var aggregation = createQualityAggregation();
        
        var result = aggregation.evaluateQualityGates(List.of());
        
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.Status.SUCCESS);
        assertThat(result.getEvaluations()).isEmpty();
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(0);
        
        assertThat(aggregation.shouldFailBuild(List.of())).isFalse();
        assertThat(aggregation.shouldMarkUnstable(List.of())).isFalse();
    }
    
    @Test
    void shouldHandleDisabledQualityGates() {
        var aggregation = createQualityAggregation();
        
        // Create quality gates where some are disabled
        var qualityGates = List.of(
            new QualityGate("Enabled Gate", "line", 95.0, QualityGate.Operator.GREATER_THAN_OR_EQUAL, QualityGate.Criticality.FAILURE, true),
            new QualityGate("Disabled Gate", "branch", 95.0, QualityGate.Operator.GREATER_THAN_OR_EQUAL, QualityGate.Criticality.FAILURE, false)
        );
        
        var result = aggregation.evaluateQualityGates(qualityGates);
        
        // Only the enabled gate should be evaluated
        assertThat(result.getEvaluations()).hasSize(1);
        assertThat(result.getEvaluations().get(0).getQualityGate().getName()).isEqualTo("Enabled Gate");
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(0);
    }

    @Test
    void shouldHandleUnknownMetrics() {
        var aggregation = createQualityAggregation();
        
        // Create quality gate for metric that doesn't exist
        var qualityGates = List.of(
            new QualityGate("Unknown Metric Gate", "unknown-metric", 50.0, QualityGate.Operator.GREATER_THAN_OR_EQUAL, QualityGate.Criticality.FAILURE, true)
        );
        
        var result = aggregation.evaluateQualityGates(qualityGates);
        
        // Should evaluate against 0 (default value for unknown metrics)
        assertThat(result.getEvaluations()).hasSize(1);
        assertThat(result.getFailureCount()).isEqualTo(1); // 0 >= 50.0 fails
        assertThat(result.getOverallStatus()).isEqualTo(QualityGateResult.Status.FAILURE);
    }

    /**
     * Creates a quality aggregation with known metrics for testing.
     * This reuses the logic from AggregatedScoreTest but creates a simplified version
     * focused on quality gate testing.
     */
    private static AggregatedScore createQualityAggregation() {
        // Reuse the existing method from AggregatedScoreTest
        return AggregatedScoreTest.createQualityAggregation();
    }
} 