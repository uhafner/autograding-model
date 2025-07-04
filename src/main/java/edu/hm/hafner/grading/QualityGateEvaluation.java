package edu.hm.hafner.grading;

import edu.hm.hafner.util.Generated;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents the result of evaluating a quality gate. Contains the gate that was evaluated, the actual value, whether
 * it passed, and a message.
 */
public class QualityGateEvaluation implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;

    private final QualityGate qualityGate;
    private final double actualValue;
    private final boolean passed;
    private final String message;

    /**
     * Creates a new quality gate evaluation result.
     *
     * @param qualityGate
     *         the quality gate that was evaluated
     * @param actualValue
     *         the actual value that was compared
     * @param passed
     *         whether the evaluation passed
     * @param message
     *         a descriptive message about the result
     */
    public QualityGateEvaluation(final QualityGate qualityGate, final double actualValue,
            final boolean passed, final String message) {
        this.qualityGate = qualityGate;
        this.actualValue = actualValue;
        this.passed = passed;
        this.message = message;
    }

    public QualityGate getQualityGate() {
        return qualityGate;
    }

    public double getActualValue() {
        return actualValue;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Returns the name of the quality gate that was evaluated.
     *
     * @return the gate name
     */
    public String getGateName() {
        return qualityGate.getName();
    }

    /**
     * Returns the metric that was evaluated.
     *
     * @return the metric name
     */
    public String getMetric() {
        return qualityGate.getMetric();
    }

    /**
     * Returns the threshold that was compared against.
     *
     * @return the threshold value
     */
    public double getThreshold() {
        return qualityGate.getThreshold();
    }

    /**
     * Returns the criticality level of the quality gate.
     *
     * @return the criticality
     */
    public QualityGate.Criticality getCriticality() {
        return qualityGate.getCriticality();
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (QualityGateEvaluation) o;
        return Double.compare(that.actualValue, actualValue) == 0
                && passed == that.passed
                && Objects.equals(qualityGate, that.qualityGate)
                && Objects.equals(message, that.message);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(qualityGate, actualValue, passed, message);
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                "QualityGateEvaluation{gate='%s', actualValue=%.2f, passed=%s, message='%s'}",
                qualityGate.getName(), actualValue, passed, message);
    }
}
