package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.hm.hafner.util.Generated;
import edu.hm.hafner.util.FilteredLog;

/**
 * Represents the result of evaluating multiple quality gates.
 * Contains all individual evaluation results and provides an overall status.
 */
public class QualityGateResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;

    /**
     * Overall status of quality gate evaluation.
     */
    public enum OverallStatus {
        /** All gates passed. */
        SUCCESS,
        /** At least one gate failed with UNSTABLE criticality (but no FAILURE). */
        UNSTABLE,
        /** At least one gate failed with FAILURE criticality. */
        FAILURE
    }

    private final List<QualityGateEvaluation> evaluations;
    private final OverallStatus overallStatus;

    /**
     * Creates a new quality gate result with no evaluations (success by default).
     */
    public QualityGateResult() {
        this(new ArrayList<>());
    }

    /**
     * Creates a new quality gate result with the specified evaluations.
     *
     * @param evaluations the individual gate evaluations
     */
    public QualityGateResult(final List<QualityGateEvaluation> evaluations) {
        this.evaluations = List.copyOf(evaluations);
        this.overallStatus = calculateOverallStatus(evaluations);
    }

    /**
     * Evaluates a list of quality gates against the given metrics with detailed logging.
     *
     * @param metrics the metric values to evaluate against
     * @param qualityGates the quality gates to evaluate
     * @param log the logger for detailed feedback
     * @return the evaluation result
     */
    public static QualityGateResult evaluate(final Map<String, Integer> metrics, 
                                           final List<QualityGate> qualityGates,
                                           final FilteredLog log) {
        if (qualityGates == null || qualityGates.isEmpty()) {
            log.logInfo("No quality gates to evaluate");
            return new QualityGateResult();
        }

        log.logInfo("Evaluating %d quality gate(s)", qualityGates.size());
        
        var evaluations = new ArrayList<QualityGateEvaluation>();
        
        for (var gate : qualityGates) {
            var actualValue = metrics.getOrDefault(gate.getMetric(), 0).doubleValue();
            var evaluation = gate.evaluate(actualValue);
            evaluations.add(evaluation);
        }
        
        var result = new QualityGateResult(evaluations);
        
        log.logInfo("Quality gates evaluation completed: %s", result.getOverallStatus());
        log.logInfo("  Passed: %d, Failed: %d", result.getSuccessCount(), result.getFailureCount());
        
        for (var evaluation : result.getEvaluations()) {
            if (evaluation.isPassed()) {
                log.logInfo("  ✅ %s", evaluation.getMessage());
            } else {
                log.logError("  ❌ %s", evaluation.getMessage());
            }
        }
        
        return result;
    }

    /**
     * Calculates the overall status based on individual evaluations.
     */
    private OverallStatus calculateOverallStatus(final List<QualityGateEvaluation> evaluations) {
        boolean hasFailure = false;
        boolean hasUnstable = false;

        for (var evaluation : evaluations) {
            if (!evaluation.isPassed()) {
                var criticality = evaluation.getCriticality();
                if (criticality == QualityGate.Criticality.FAILURE) {
                    hasFailure = true;
                } else if (criticality == QualityGate.Criticality.UNSTABLE) {
                    hasUnstable = true;
                }
            }
        }

        if (hasFailure) {
            return OverallStatus.FAILURE;
        } else if (hasUnstable) {
            return OverallStatus.UNSTABLE;
        } else {
            return OverallStatus.SUCCESS;
        }
    }

    public List<QualityGateEvaluation> getEvaluations() {
        return evaluations;
    }

    public OverallStatus getOverallStatus() {
        return overallStatus;
    }

    /**
     * Returns the number of successful evaluations.
     *
     * @return the success count
     */
    public int getSuccessCount() {
        return (int) evaluations.stream().filter(QualityGateEvaluation::isPassed).count();
    }

    /**
     * Returns the number of failed evaluations.
     *
     * @return the failure count
     */
    public int getFailureCount() {
        return evaluations.size() - getSuccessCount();
    }

    /**
     * Returns whether all quality gates passed.
     *
     * @return true if all gates passed, false otherwise
     */
    public boolean isSuccessful() {
        return overallStatus == OverallStatus.SUCCESS;
    }

    /**
     * Returns whether the result contains any failures.
     *
     * @return true if there are failures, false otherwise
     */
    public boolean hasFailures() {
        return getFailureCount() > 0;
    }

    /**
     * Creates a markdown summary of the quality gate results.
     *
     * @return markdown formatted summary
     */
    public String createMarkdownSummary() {
        if (evaluations.isEmpty()) {
            return "";
        }

        var summary = new StringBuilder();
        summary.append("\n\n## 🚦 Quality Gates\n\n");
        
        // Overall status
        var statusIcon = switch (overallStatus) {
            case SUCCESS -> "✅";
            case UNSTABLE -> "⚠️";
            case FAILURE -> "❌";
        };
        
        summary.append(String.format("### Overall Status: %s %s\n\n", statusIcon, overallStatus));
        
        // Separate passed and failed evaluations
        var passedEvaluations = evaluations.stream().filter(QualityGateEvaluation::isPassed).toList();
        var failedEvaluations = evaluations.stream().filter(eval -> !eval.isPassed()).toList();
        
        // Show passed gates if any
        if (!passedEvaluations.isEmpty()) {
            summary.append("#### ✅ Passed Gates\n\n");
            for (var evaluation : passedEvaluations) {
                summary.append(String.format("- ✅ %s\n", evaluation.getMessage()));
            }
            summary.append("\n");
        }
        
        // Show failed gates if any
        if (!failedEvaluations.isEmpty()) {
            summary.append("#### ❌ Failed Gates\n\n");
            for (var evaluation : failedEvaluations) {
                summary.append(String.format("- ❌ %s\n", evaluation.getMessage()));
            }
            summary.append("\n");
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
        return String.format("QualityGateResult{status=%s, passed=%d, failed=%d}",
                overallStatus, getSuccessCount(), getFailureCount());
    }
} 