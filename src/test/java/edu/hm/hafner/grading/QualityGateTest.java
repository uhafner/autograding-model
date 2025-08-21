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
        var gate = new QualityGate("Line Coverage", "line", 80.0, Criticality.FAILURE);

        assertThat(gate)
                .hasName("Line Coverage")
                .hasMetric("line")
                .hasThreshold(80.0)
                .hasCriticality(Criticality.FAILURE);
        assertThat(gate.toString())
                .contains("Line Coverage")
                .contains("line")
                .contains("80.00")
                .contains("FAILURE");
    }

    @Test
    void shouldEvaluateLineCoveragePassWithLargerValue() {
        var gate = new QualityGate("Line Coverage", "line", 80.0, Criticality.FAILURE);
        var evaluation = gate.evaluate(85.0);

        assertThat(evaluation).isPassed().hasActualValue(85.0);
        assertThat(evaluation.getMessage()).contains("Line Coverage: 85.00 >= 80.00");
    }

    @Test
    void shouldEvaluateLineCoverageFailWithSmallerValue() {
        var gate = new QualityGate("Line Coverage", "line", 80.0, Criticality.FAILURE);
        var evaluation = gate.evaluate(75.0);

        assertThat(evaluation).isNotPassed().hasActualValue(75.0);
        assertThat(evaluation.getMessage()).contains("Line Coverage: 75.00 >= 80.00");
    }

    @Test
    void shouldEvaluateLineCoveragePassWithEqualValue() {
        var gate = new QualityGate("Line Coverage", "line", 80.0, Criticality.FAILURE);
        var evaluation = gate.evaluate(80.0);

        assertThat(evaluation).isPassed().hasActualValue(80.0);
        assertThat(evaluation.getMessage()).contains("Line Coverage: 80.00 >= 80.00");
    }

    @Test
    void shouldEvaluateCheckStyleWarnings() {
        var gate = new QualityGate("CheckStyle", "checkstyle", 0.0, Criticality.UNSTABLE);

        assertThat(gate.evaluate(0)).isPassed()
                .hasActualValue(0.0)
                .hasThreshold(0.0)
                .hasMessage("CheckStyle: 0.00 <= 0.00");
        assertThat(gate.evaluate(1)).isNotPassed()
                .hasActualValue(1.0)
                .hasThreshold(0.0)
                .hasMessage("CheckStyle: 1.00 <= 0.00");
    }

    @ParameterizedTest(name = "Criticality: {0}")
    @EnumSource(Criticality.class)
    void shouldSupportDifferentCriticalityLevels(final Criticality criticality) {
        var gateFailure = new QualityGate("Test", "line", 80.0, criticality);

        assertThat(gateFailure).hasCriticality(criticality);
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(QualityGate.class).verify();
    }

    @Test
    void shouldUseLessThanForUnknownMetrics() {
        var gate = new QualityGate("Unknown", "unknown", 1.0, Criticality.UNSTABLE);

        assertThat(gate.evaluate(0)).isPassed();
        assertThat(gate.evaluate(1)).isPassed();
        assertThat(gate.evaluate(2)).isNotPassed();
    }

    @Test
    void shouldUseGreaterThanForRates() {
        var gate = new QualityGate("Unknown", "tests-success-rate", 99, Criticality.UNSTABLE);

        assertThat(gate.evaluate(100)).isPassed();
        assertThat(gate.evaluate(99)).isPassed();
        assertThat(gate.evaluate(98)).isNotPassed();
    }
}
