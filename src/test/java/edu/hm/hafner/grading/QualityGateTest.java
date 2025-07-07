package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.grading.QualityGate.Criticality;
import edu.hm.hafner.util.FilteredLog;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.grading.assertions.Assertions.*;

/**
 * Tests for {@link QualityGate}.
 */
class QualityGateTest {
    private static final FilteredLog LOGGER = new FilteredLog("Eeeo");

    @Test
    void shouldCreateQualityGate() {
        var gate = createLineCoverageQualityGate();

        assertThat(gate)
                .hasName("Line Coverage")
                .hasMetric("line")
                .hasThreshold(80.0)
                .hasCriticality(QualityGate.Criticality.FAILURE);
    }

    @Test
    void shouldEvaluateLineCoveragePassWithLargerValue() {
        var gate = createLineCoverageQualityGate();

        assertThat(gate.evaluate(85.0, LOGGER))
                .isPassed()
                .hasActualValue(85.0)
                .hasMessage("Line Coverage: 85.00 >= 80.00");
    }

    @Test
    void shouldEvaluateComplexityPassWithLowerValue() {
        var gate = new QualityGate("Complexity", "cyclomatic-complexity", 80.0, Criticality.FAILURE);

        assertThat(gate.evaluate(75.0, LOGGER))
                .isPassed()
                .hasActualValue(75.0)
                .hasMessage("Complexity: 75.00 <= 80.00");
    }

    @Test
    void shouldHandleInvalidMetric() {
        var gate = new QualityGate("Line Coverage", "not-valid", 80.0, Criticality.FAILURE);

        var log = new FilteredLog("Error");

        assertThat(gate.evaluate(85.0, log))
                .isPassed()
                .hasActualValue(85.0)
                .hasMessage("Line Coverage: 85.00 >= 80.00");

        assertThat(log.getInfoMessages()).isEmpty();
        assertThat(log.getErrorMessages())
                .anySatisfy(message ->
                        assertThat(message).contains("Unknown metric 'not-valid'. Assuming larger is better."))
                .anySatisfy(message ->
                        assertThat(message).contains("Unknown metric 'not-valid'. Using >= operator.")
                );
    }

    @Test
    void shouldEvaluateLineCoverageFailWithSmallerValue() {
        var gate = createLineCoverageQualityGate();

        assertThat(gate.evaluate(75.0, LOGGER))
                .isNotPassed()
                .hasActualValue(75.0)
                .hasMessage("Line Coverage: 75.00 >= 80.00");
    }

    @Test
    void shouldEvaluateLineCoveragePassWithEqualValue() {
        var gate = createLineCoverageQualityGate();

        assertThat(gate.evaluate(80.0, LOGGER))
                .isPassed()
                .hasActualValue(80.0)
                .hasMessage("Line Coverage: 80.00 >= 80.00");
    }

    @Test
    void shouldSupportDifferentCriticalityLevels() {
        var gateFailure = new QualityGate("Test", "line", 80.0, QualityGate.Criticality.FAILURE);
        assertThat(gateFailure.getCriticality()).isEqualTo(QualityGate.Criticality.FAILURE);

        var gateUnstable = new QualityGate("Test", "line", 80.0, QualityGate.Criticality.UNSTABLE);
        assertThat(gateUnstable.getCriticality()).isEqualTo(QualityGate.Criticality.UNSTABLE);
    }

    @Test
    void shouldImplementToString() {
        var gate = createLineCoverageQualityGate();

        assertThat(gate.toString())
                .contains("Line Coverage")
                .contains("line")
                .contains("80.00")
                .contains("FAILURE");
    }

    private QualityGate createLineCoverageQualityGate() {
        return new QualityGate("Line Coverage", "line", 80.0, QualityGate.Criticality.FAILURE);
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(QualityGate.class).verify();
    }
}
