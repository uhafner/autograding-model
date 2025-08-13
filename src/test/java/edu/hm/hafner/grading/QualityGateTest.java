package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import edu.hm.hafner.grading.QualityGate.Criticality;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.grading.assertions.Assertions.*;

/**
 * Tests for {@link QualityGate}.
 */
class QualityGateTest {
    @Test
    void shouldCreateQualityGate() {
        var gate = new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE);

        assertThat(gate)
                .hasName("Line Coverage")
                .hasMetric("line")
                .hasThreshold(80.0)
                .hasCriticality(QualityGate.Criticality.FAILURE);
        assertThat(gate.toString())
                .contains("Line Coverage")
                .contains("line")
                .contains("80.00")
                .contains("FAILURE");
    }

    @Test
    void shouldEvaluateLineCoveragePassWithLargerValue() {
        var gate = new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE);
        var evaluation = gate.evaluate(85.0);

        assertThat(evaluation).isPassed().hasActualValue(85.0);
        assertThat(evaluation.getMessage()).contains("Line Coverage: 85.00 >= 80.00");
    }

    @Test
    void shouldEvaluateLineCoverageFailWithSmallerValue() {
        var gate = new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE);
        var evaluation = gate.evaluate(75.0);

        assertThat(evaluation).isNotPassed().hasActualValue(75.0);
        assertThat(evaluation.getMessage()).contains("Line Coverage: 75.00 >= 80.00");
    }

    @Test
    void shouldEvaluateLineCoveragePassWithEqualValue() {
        var gate = new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE);
        var evaluation = gate.evaluate(80.0);

        assertThat(evaluation).isPassed().hasActualValue(80.0);
        assertThat(evaluation.getMessage()).contains("Line Coverage: 80.00 >= 80.00");
    }

    @ParameterizedTest(name = "Criticality: {0}")
    @EnumSource(QualityGate.Criticality.class)
    void shouldSupportDifferentCriticalityLevels(final Criticality criticality) {
        var gateFailure = new QualityGate("Test", "line", 80.0, criticality);
        
        assertThat(gateFailure).hasCriticality(criticality);
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(QualityGate.class).verify();
    }
}
