package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link QualityGate}.
 */
class QualityGateTest {
    @Test
    void shouldCreateQualityGate() {
        var gate = new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE);

        assertThat(gate.getName()).isEqualTo("Line Coverage");
        assertThat(gate.getMetric()).isEqualTo("line");
        assertThat(gate.getThreshold()).isEqualTo(80.0);
        assertThat(gate.getCriticality()).isEqualTo(QualityGate.Criticality.FAILURE);
    }

    @Test
    void shouldEvaluateLineCoveragePassWithLargerValue() {
        var gate = new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE);
        var evaluation = gate.evaluate(85.0);

        assertThat(evaluation.isPassed()).isTrue();
        assertThat(evaluation.getActualValue()).isEqualTo(85.0);
        assertThat(evaluation.getMessage()).contains("Line Coverage: 85.00 >= 80.00");
    }

    @Test
    void shouldEvaluateLineCoverageFailWithSmallerValue() {
        var gate = new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE);
        var evaluation = gate.evaluate(75.0);

        assertThat(evaluation.isPassed()).isFalse();
        assertThat(evaluation.getActualValue()).isEqualTo(75.0);
        assertThat(evaluation.getMessage()).contains("Line Coverage: 75.00 >= 80.00");
    }

    @Test
    void shouldEvaluateLineCoveragePassWithEqualValue() {
        var gate = new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE);
        var evaluation = gate.evaluate(80.0);

        assertThat(evaluation.isPassed()).isTrue();
        assertThat(evaluation.getActualValue()).isEqualTo(80.0);
        assertThat(evaluation.getMessage()).contains("Line Coverage: 80.00 >= 80.00");
    }

    @Test
    void shouldSupportDifferentCriticalityLevels() {
        var gateFailure = new QualityGate("Test", "line", 80.0, QualityGate.Criticality.FAILURE);
        var gateUnstable = new QualityGate("Test", "line", 80.0, QualityGate.Criticality.UNSTABLE);

        assertThat(gateFailure.getCriticality()).isEqualTo(QualityGate.Criticality.FAILURE);
        assertThat(gateUnstable.getCriticality()).isEqualTo(QualityGate.Criticality.UNSTABLE);
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        var gate1 = new QualityGate("Test", "line", 80.0, QualityGate.Criticality.FAILURE);
        var gate2 = new QualityGate("Test", "line", 80.0, QualityGate.Criticality.FAILURE);
        var gate3 = new QualityGate("Different", "line", 80.0, QualityGate.Criticality.FAILURE);

        assertThat(gate1).isEqualTo(gate2);
        assertThat(gate1).isNotEqualTo(gate3);
        assertThat(gate1.hashCode()).isEqualTo(gate2.hashCode());
    }

    @Test
    void shouldImplementToString() {
        var gate = new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE);

        assertThat(gate.toString())
                .contains("Line Coverage")
                .contains("line")
                .contains("80.00")
                .contains("FAILURE");
    }
}
