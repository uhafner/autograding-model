package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Locale;

import edu.hm.hafner.util.Generated;

/**
 * Represents the aggregated result of evaluating multiple quality gates.
 * Contains all individual evaluation results and provides summary information.
 */
public class QualityGateResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Overall status of quality gate evaluation.
     */
    public enum Status {
        /** All gates passed. */
        SUCCESS,
        /** Some gates failed with UNSTABLE criticality. */
        UNSTABLE,
        /** Some gates failed with FAILURE criticality. */
        FAILURE
    }

    private final List<QualityGateEvaluation> evaluations;
    private Status overallStatus;

    /**
     * Creates a new quality gate result.
     */
    public QualityGateResult() {
        this.evaluations = new ArrayList<>();
        this.overallStatus = Status.SUCCESS;
    }

    /**
     * Adds an evaluation result to this aggregated result.
     *
     * @param evaluation the evaluation to add
     */
    public void addEvaluation(final QualityGateEvaluation evaluation) {
        evaluations.add(evaluation);
        updateOverallStatus();
    }

    /**
     * Updates the overall status based on all evaluations.
     */
    private void updateOverallStatus() {
        boolean hasFailures = false;
        boolean hasUnstable = false;

        for (var evaluation : evaluations) {
            if (!evaluation.isPassed()) {
                var criticality = evaluation.getCriticality();
                if (criticality == QualityGate.Criticality.FAILURE) {
                    hasFailures = true;
                }
                else if (criticality == QualityGate.Criticality.ERROR || 
                         criticality == QualityGate.Criticality.UNSTABLE || 
                         criticality == QualityGate.Criticality.NOTE) {
                    hasUnstable = true;
                }
            }
        }

        if (hasFailures) {
            overallStatus = Status.FAILURE;
        }
        else if (hasUnstable) {
            overallStatus = Status.UNSTABLE;
        }
        else {
            overallStatus = Status.SUCCESS;
        }
    }

    public List<QualityGateEvaluation> getEvaluations() {
        return List.copyOf(evaluations);
    }

    public Status getOverallStatus() {
        return overallStatus;
    }

    /**
     * Returns whether all quality gates passed.
     *
     * @return true if all gates passed, false otherwise
     */
    public boolean isSuccessful() {
        return overallStatus == Status.SUCCESS;
    }

    /**
     * Returns the number of quality gates that failed.
     *
     * @return the failure count
     */
    public long getFailureCount() {
        return evaluations.stream()
                .filter(eval -> !eval.isPassed())
                .count();
    }

    /**
     * Returns the number of quality gates that passed.
     *
     * @return the success count
     */
    public long getSuccessCount() {
        return evaluations.stream()
                .filter(QualityGateEvaluation::isPassed)
                .count();
    }

    /**
     * Returns only the evaluations that failed.
     *
     * @return the failed evaluations
     */
    public List<QualityGateEvaluation> getFailedEvaluations() {
        return evaluations.stream()
                .filter(eval -> !eval.isPassed())
                .collect(Collectors.toList());
    }

    /**
     * Returns only the evaluations that passed.
     *
     * @return the successful evaluations
     */
    public List<QualityGateEvaluation> getSuccessfulEvaluations() {
        return evaluations.stream()
                .filter(QualityGateEvaluation::isPassed)
                .collect(Collectors.toList());
    }

    /**
     * Creates a summary message of all quality gate results.
     *
     * @return the summary message
     */
    public String getSummary() {
        if (evaluations.isEmpty()) {
            return "No quality gates configured";
        }

        var summary = new StringBuilder();
        summary.append(String.format(Locale.ROOT, "Quality Gates: %d total, %d passed, %d failed%n",
                evaluations.size(), getSuccessCount(), getFailureCount()));

        if (getFailureCount() > 0) {
            summary.append("%nFailed Gates:%n");
            for (var evaluation : getFailedEvaluations()) {
                summary.append(String.format("  %s%n", evaluation.getMessage()));
            }
        }

        return summary.toString();
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
        var that = (QualityGateResult) o;
        return Objects.equals(evaluations, that.evaluations)
                && overallStatus == that.overallStatus;
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(evaluations, overallStatus);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "QualityGateResult{status=%s, evaluations=%d, failures=%d}",
                overallStatus, evaluations.size(), getFailureCount());
    }
} 