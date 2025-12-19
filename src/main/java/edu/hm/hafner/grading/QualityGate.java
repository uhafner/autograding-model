package edu.hm.hafner.grading;

import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.util.Generated;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents a quality gate that checks if a specific metric meets a threshold. Quality gates can be used to fail or
 * mark builds as unstable based on quality metrics. The comparison operator is automatically determined based on the
 * metric's tendency.
 */
@SuppressWarnings("ClassCanBeRecord")
public final class QualityGate implements Serializable {
    @Serial
    private static final long serialVersionUID = 4L;

    private static final ParserRegistry PARSER_REGISTRY = new ParserRegistry();
    private static final String GT = ">=";
    private static final String LT = "<=";

    /**
     * Defines the criticality level when a quality gate fails.
     */
    public enum Criticality {
        /** Unstable: Mark the build as unstable. */
        UNSTABLE,
        /** Failure: Fail the build. */
        FAILURE
    }

    private final String name;
    private final String metric;
    private final Scope scope;
    private final double threshold;
    private final Criticality criticality;

    QualityGate(final String name, final String metric, final Scope scope, final double threshold,
            final Criticality criticality) {
        this.name = name;
        this.metric = metric;
        this.scope = scope;
        this.threshold = threshold;
        this.criticality = criticality;
    }

    public String getName() {
        return name;
    }

    public String getMetric() {
        return metric;
    }

    public Scope getScope() {
        return scope;
    }

    public double getThreshold() {
        return threshold;
    }

    public Criticality getCriticality() {
        return criticality;
    }

    /**
     * Evaluates this quality gate against the given actual value. The comparison direction is automatically determined
     * based on the metric's tendency.
     *
     * @param actualValue
     *         the actual value to compare against the threshold
     *
     * @return the evaluation result
     */
    public QualityGateEvaluation evaluate(final double actualValue) {
        boolean passed = isMetricThresholdMet(actualValue);

        String message = createEvaluationMessage(actualValue);
        return new QualityGateEvaluation(this, actualValue, passed, message);
    }

    /**
     * Determines if the metric value meets the quality gate threshold based on the metric tendency.
     *
     * @param actualValue
     *         the actual metric value
     *
     * @return true if the value is good (passes the gate), false otherwise
     */
    private boolean isMetricThresholdMet(final double actualValue) {
        if (PARSER_REGISTRY.contains(metric)) {
            return actualValue <= threshold; // for static analysis metrics, lower is better
        }

        return isLargerBetter() ? actualValue >= threshold : actualValue <= threshold;
    }

    private String getOperatorSymbol() {
        if (PARSER_REGISTRY.contains(metric)) {
            return LT;
        }

        return isLargerBetter() ? GT : LT;
    }

    /**
     * Creates a readable evaluation message.
     *
     * @param actualValue
     *         the actual value that was evaluated
     *
     * @return a formatted message describing the evaluation
     */
    private String createEvaluationMessage(final double actualValue) {
        // Remove icons here since they are handled by the summary formatter
        return String.format(Locale.ENGLISH, "%s: %.2f %s %.2f", name, actualValue, getOperatorSymbol(), threshold);
    }

    private boolean isLargerBetter() {
        if (metric.contains("-rate")) {
            return true; // Rates are always larger is better
        }
        try {
            var modelMetric = Metric.fromName(metric);
            return modelMetric.getTendency() == Metric.MetricTendency.LARGER_IS_BETTER;
        }
        catch (IllegalArgumentException e) {
            // If the metric is not recognized, default to smaller is better
            return false;
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (QualityGate) o;
        return Double.compare(threshold, that.threshold) == 0
                && Objects.equals(name, that.name)
                && Objects.equals(metric, that.metric)
                && Objects.equals(scope, that.scope)
                && criticality == that.criticality;
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(name, metric, scope, threshold, criticality);
    }

    @Override
    @Generated
    public String toString() {
        return "QualityGate{name='" + name + '\'' + ", metric='" + metric + '\'' + ", scope='"
                + scope + '\'' + ", threshold=" + threshold + ", criticality=" + criticality + '}';
    }
}
