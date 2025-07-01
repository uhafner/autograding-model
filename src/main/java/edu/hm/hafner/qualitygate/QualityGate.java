package edu.hm.hafner.qualitygate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import edu.hm.hafner.util.Generated;

/**
 * Represents a quality gate that checks if a specific metric meets a threshold.
 * Quality gates can be used to fail or mark builds as unstable based on quality metrics.
 */
public class QualityGate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Defines the criticality level when a quality gate fails.
     */
    public enum Criticality {
        /** Note: Log a message but don't affect build status. */
        NOTE,
        /** Unstable: Mark build as unstable. */
        UNSTABLE,
        /** Error: Log an error but don't fail the build. */
        ERROR,
        /** Failure: Fail the build. */
        FAILURE
    }

    /**
     * Defines the comparison operator for threshold evaluation.
     */
    public enum Operator {
        /** Greater than or equal to threshold. */
        GREATER_THAN_OR_EQUAL,
        /** Less than or equal to threshold. */
        LESS_THAN_OR_EQUAL,
        /** Greater than threshold. */
        GREATER_THAN,
        /** Less than threshold. */
        LESS_THAN,
        /** Equal to threshold. */
        EQUAL,
        /** Not equal to threshold. */
        NOT_EQUAL
    }

    private String name;
    private String metric;
    private double threshold;
    private Operator operator;
    private Criticality criticality;
    private boolean enabled;

    /**
     * Creates a new quality gate.
     */
    public QualityGate() {
        this("", "", 0.0, Operator.GREATER_THAN_OR_EQUAL, Criticality.UNSTABLE, true);
    }

    /**
     * Creates a new quality gate with the specified parameters.
     *
     * @param name the name of the quality gate
     * @param metric the metric to evaluate
     * @param threshold the threshold value
     * @param operator the comparison operator
     * @param criticality the criticality level
     * @param enabled whether the gate is enabled
     */
    public QualityGate(final String name, final String metric, final double threshold,
                       final Operator operator, final Criticality criticality, final boolean enabled) {
        this.name = name;
        this.metric = metric;
        this.threshold = threshold;
        this.operator = operator;
        this.criticality = criticality;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(final String metric) {
        this.metric = metric;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(final double threshold) {
        this.threshold = threshold;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(final Operator operator) {
        this.operator = operator;
    }

    public Criticality getCriticality() {
        return criticality;
    }

    public void setCriticality(final Criticality criticality) {
        this.criticality = criticality;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Evaluates this quality gate against the given actual value.
     *
     * @param actualValue the actual value to compare against the threshold
     * @return the evaluation result
     */
    public QualityGateEvaluation evaluate(final double actualValue) {
        if (!enabled) {
            return new QualityGateEvaluation(this, actualValue, true, "Quality gate is disabled");
        }

        boolean passed;
        switch (operator) {
            case GREATER_THAN_OR_EQUAL:
                passed = actualValue >= threshold;
                break;
            case LESS_THAN_OR_EQUAL:
                passed = actualValue <= threshold;
                break;
            case GREATER_THAN:
                passed = actualValue > threshold;
                break;
            case LESS_THAN:
                passed = actualValue < threshold;
                break;
            case EQUAL:
                passed = Double.compare(actualValue, threshold) == 0;
                break;
            case NOT_EQUAL:
                passed = Double.compare(actualValue, threshold) != 0;
                break;
            default:
                passed = false;
                break;
        }

        String message = passed 
                ? String.format(Locale.ROOT, "✅ %s: %.2f %s %.2f", name, actualValue, getOperatorSymbol(), threshold)
                : String.format(Locale.ROOT, "❌ %s: %.2f %s %.2f", name, actualValue, getOperatorSymbol(), threshold);

        return new QualityGateEvaluation(this, actualValue, passed, message);
    }

    private String getOperatorSymbol() {
        switch (operator) {
            case GREATER_THAN_OR_EQUAL:
                return ">=";
            case LESS_THAN_OR_EQUAL:
                return "<=";
            case GREATER_THAN:
                return ">";
            case LESS_THAN:
                return "<";
            case EQUAL:
                return "==";
            case NOT_EQUAL:
                return "!=";
            default:
                return "?";
        }
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
        var that = (QualityGate) o;
        return Double.compare(that.threshold, threshold) == 0
                && enabled == that.enabled
                && Objects.equals(name, that.name)
                && Objects.equals(metric, that.metric)
                && operator == that.operator
                && criticality == that.criticality;
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(name, metric, threshold, operator, criticality, enabled);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "QualityGate{name='%s', metric='%s', threshold=%.2f, operator=%s, criticality=%s, enabled=%s}",
                name, metric, threshold, operator, criticality, enabled);
    }
} 