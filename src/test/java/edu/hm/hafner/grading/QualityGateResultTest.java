package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;

import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.grading.assertions.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link QualityGateResult}.
 */
class QualityGateResultTest {
    private static final FilteredLog LOG = new FilteredLog("Test");

    @Test
    void shouldCreateEmptyResult() {
        var result = new QualityGateResult();

        assertThat(result).isSuccessful()
                .hasSuccessCount(0)
                .hasFailureCount(0)
                .hasOverallStatus(QualityGateResult.OverallStatus.SUCCESS)
                .hasNoEvaluations()
                .doesNotHaveFailures();
    }

    @Test
    void shouldEvaluateAllPassingGates() {
        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE),
                new QualityGate("Branch Coverage", "branch", 65.0, QualityGate.Criticality.UNSTABLE)
        );

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble("line")).thenReturn(85.0);
        when(statistics.asDouble("branch")).thenReturn(70.0);

        var result = QualityGateResult.evaluate(statistics, qualityGates, LOG);

        assertThat(result).isSuccessful()
                .hasSuccessCount(2)
                .hasFailureCount(0)
                .hasOverallStatus(QualityGateResult.OverallStatus.SUCCESS)
                .doesNotHaveFailures();
        assertThat(result.getEvaluations()).hasSize(2)
                .map(QualityGateEvaluation::isPassed)
                .containsExactly(true, true);
    }

    @Test
    void shouldHandleFailureGates() {
        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE)
        );

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble("line")).thenReturn(75.0);

        var result = QualityGateResult.evaluate(statistics, qualityGates, LOG);

        assertThat(result).isNotSuccessful()
                .hasSuccessCount(0)
                .hasFailureCount(1)
                .hasOverallStatus(QualityGateResult.OverallStatus.FAILURE)
                .hasFailures();

        assertThat(result.getEvaluations()).hasSize(1);
        var evaluation = result.getEvaluations().get(0);
        assertThat(evaluation).isNotPassed().hasActualValue(75.0)
                .hasCriticality(QualityGate.Criticality.FAILURE)
                .hasMessage("Line Coverage: 75.00 >= 80.00")
                .hasGateName("Line Coverage")
                .hasMetric("line")
                .hasThreshold(80.0)
                .hasQualityGate(new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE));
    }

    @Test
    void shouldHandleUnstableGates() {
        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.UNSTABLE)
        );

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble("line")).thenReturn(75.0);

        var result = QualityGateResult.evaluate(statistics, qualityGates, LOG);

        assertThat(result).isNotSuccessful()
                .hasSuccessCount(0)
                .hasFailureCount(1)
                .hasOverallStatus(QualityGateResult.OverallStatus.UNSTABLE)
                .hasFailures();

        assertThat(result.getEvaluations()).hasSize(1);
        var evaluation = result.getEvaluations().get(0);
        assertThat(evaluation).isNotPassed().hasActualValue(75.0)
                .hasCriticality(QualityGate.Criticality.UNSTABLE)
                .hasMessage("Line Coverage: 75.00 >= 80.00")
                .hasGateName("Line Coverage")
                .hasMetric("line")
                .hasThreshold(80.0)
                .hasQualityGate(new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.UNSTABLE));
    }

    @Test
    void shouldPrioritizeFailureOverUnstable() {
        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE),
                new QualityGate("Branch Coverage", "branch", 65.0, QualityGate.Criticality.UNSTABLE)
        );

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble("line")).thenReturn(75.0);
        when(statistics.asDouble("branch")).thenReturn(60.0);

        var result = QualityGateResult.evaluate(statistics, qualityGates, LOG);

        assertThat(result).isNotSuccessful()
                .hasSuccessCount(0)
                .hasFailureCount(2)
                .hasOverallStatus(QualityGateResult.OverallStatus.FAILURE)
                .hasFailures();
        assertThat(result.getEvaluations()).hasSize(2)
                .map(QualityGateEvaluation::isPassed)
                .containsExactly(false, false);
    }

    @Test
    void shouldHandleMixedResults() {
        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE),
                new QualityGate("Branch Coverage", "branch", 65.0, QualityGate.Criticality.UNSTABLE)
        );

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble("line")).thenReturn(85.0);
        when(statistics.asDouble("branch")).thenReturn(60.0);

        var result = QualityGateResult.evaluate(statistics, qualityGates, LOG);

        assertThat(result).isNotSuccessful()
                .hasSuccessCount(1)
                .hasFailureCount(1)
                .hasOverallStatus(QualityGateResult.OverallStatus.UNSTABLE)
                .hasFailures();
        assertThat(result.toString())
                .contains("UNSTABLE")
                .contains("passed=1")
                .contains("failed=1");
    }

    @Test
    void shouldCreateMarkdownSummary() {
        var qualityGates = List.of(
                new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE),
                new QualityGate("Branch Coverage", "branch", 60.0, QualityGate.Criticality.UNSTABLE)
        );

        var statistics = mock(MetricStatistics.class);
        when(statistics.asDouble("line")).thenReturn(85.0);
        when(statistics.asDouble("branch")).thenReturn(50.0);

        var result = QualityGateResult.evaluate(statistics, qualityGates, LOG);
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
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(QualityGateResult.class).verify();
    }
}
