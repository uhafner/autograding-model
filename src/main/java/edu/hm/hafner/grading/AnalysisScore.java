package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.Generated;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Computes the {@link Score} impact of static analysis results. These results are obtained by summing up the number of
 * static analysis warnings.
 *
 * @author Eva-Maria Zeintl
 */
@SuppressWarnings("PMD.DataClass")
public final class AnalysisScore extends Score<AnalysisScore, AnalysisConfiguration> {
    @Serial
    private static final long serialVersionUID = 3L;

    private final int errorSize;
    private final int highSeveritySize;
    private final int normalSeveritySize;
    private final int lowSeveritySize;

    private transient Report report; // do not persist the issues

    private AnalysisScore(final String id, final String name, final AnalysisConfiguration configuration,
            final List<AnalysisScore> scores) {
        super(id, name, configuration, scores.toArray(new AnalysisScore[0]));

        this.errorSize = scores.stream().reduce(0, (sum, score) -> sum + score.getErrorSize(), Integer::sum);
        this.highSeveritySize = scores.stream().reduce(0, (sum, score) -> sum + score.getHighSeveritySize(), Integer::sum);
        this.normalSeveritySize = scores.stream().reduce(0, (sum, score) -> sum + score.getNormalSeveritySize(), Integer::sum);
        this.lowSeveritySize = scores.stream().reduce(0, (sum, score) -> sum + score.getLowSeveritySize(), Integer::sum);

        this.report = new Report();

        scores.stream().map(AnalysisScore::getReport).forEach(report::addAll);
    }

    private AnalysisScore(final String id, final String name, final AnalysisConfiguration configuration,
            final Report report) {
        super(id, name, configuration);

        this.errorSize = report.getSizeOf(Severity.ERROR);
        this.highSeveritySize = report.getSizeOf(Severity.WARNING_HIGH);
        this.normalSeveritySize = report.getSizeOf(Severity.WARNING_NORMAL);
        this.lowSeveritySize = report.getSizeOf(Severity.WARNING_LOW);

        this.report = report;
    }

    /**
     * Restore an empty report after de-serialization.
     *
     * @return this
     */
    @Serial @CanIgnoreReturnValue
    private Object readResolve() {
        report = new Report();

        return this;
    }

    @Override
    public int getImpact() {
        var analysisConfiguration = getConfiguration();

        int change = 0;

        change = change + analysisConfiguration.getErrorImpact() * getErrorSize();
        change = change + analysisConfiguration.getHighImpact() * getHighSeveritySize();
        change = change + analysisConfiguration.getNormalImpact() * getNormalSeveritySize();
        change = change + analysisConfiguration.getLowImpact() * getLowSeveritySize();

        return change;
    }

    @JsonIgnore
    public Report getReport() {
        return ObjectUtils.defaultIfNull(report, new Report());
    }

    public int getReportFiles() {
        return getReport().getOriginReportFiles().size();
    }

    public int getErrorSize() {
        return errorSize;
    }

    public int getHighSeveritySize() {
        return highSeveritySize;
    }

    public int getNormalSeveritySize() {
        return normalSeveritySize;
    }

    public int getLowSeveritySize() {
        return lowSeveritySize;
    }

    public int getTotalSize() {
        return getErrorSize() + getHighSeveritySize() + getNormalSeveritySize() + getLowSeveritySize();
    }

    @Override
    protected String createSummary() {
        if (getReport().isEmpty()) {
            return "No warnings";
        }
        else {
            return String.format("%d warning%s (%d error%s, %d high, %d normal, %d low)",
                    getTotalSize(), plural(getTotalSize()),
                    getErrorSize(), plural(getErrorSize()),
                    getHighSeveritySize(), getNormalSeveritySize(), getLowSeveritySize());
        }
    }

    static String plural(final int score) {
        return score > 1 ? "s" : "";
    }

    @Override @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        AnalysisScore that = (AnalysisScore) o;
        return errorSize == that.errorSize
                && highSeveritySize == that.highSeveritySize
                && normalSeveritySize == that.normalSeveritySize
                && lowSeveritySize == that.lowSeveritySize;
    }

    @Override @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), errorSize, highSeveritySize, normalSeveritySize, lowSeveritySize);
    }

    /**
     * A builder for {@link AnalysisScore} instances.
     */
    @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    public static class AnalysisScoreBuilder {
        @CheckForNull
        private String id;
        @CheckForNull
        private String name;
        @CheckForNull
        private AnalysisConfiguration configuration;

        private final List<AnalysisScore> scores = new ArrayList<>();
        @CheckForNull
        private Report report;

        /**
         * Sets the ID of the analysis score.
         *
         * @param id
         *         the ID
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public AnalysisScoreBuilder withId(final String id) {
            this.id = id;
            return this;
        }

        private String getId() {
            return StringUtils.defaultIfBlank(id, getConfiguration().getId());
        }

        /**
         * Sets the human-readable name of the analysis score.
         *
         * @param name
         *         the name to show
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public AnalysisScoreBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        private String getName() {
            return StringUtils.defaultIfBlank(name, getConfiguration().getName());
        }

        /**
         * Sets the grading configuration.
         *
         * @param configuration
         *         the grading configuration
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public AnalysisScoreBuilder withConfiguration(final AnalysisConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        private AnalysisConfiguration getConfiguration() {
            return Objects.requireNonNull(configuration);
        }

        /**
         * Sets the scores that should be aggregated by this score.
         *
         * @param scores
         *         the scores to aggregate
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public AnalysisScoreBuilder withScores(final List<AnalysisScore> scores) {
            Ensure.that(scores).isNotEmpty("You cannot add an empty list of scores.");
            this.scores.clear();
            this.scores.addAll(scores);

            return this;
        }

        /**
         * Sets the report with the issues that should be evaluated by this score.
         *
         * @param report
         *         the issues to evaluate
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public AnalysisScoreBuilder withReport(final Report report) {
            this.report = report;
            return this;
        }

        /**
         * Builds the {@link AnalysisScore} instance with the configured values.
         *
         * @return the new instance
         */
        public AnalysisScore build() {
            Ensure.that(report != null ^ !scores.isEmpty()).isTrue(
                    "You must either specify an analysis report or provide a list of sub-scores.");

            if (report == null) {
                return new AnalysisScore(getId(), getName(), getConfiguration(), scores);
            }
            return new AnalysisScore(getId(), getName(), getConfiguration(), Objects.requireNonNull(report));
        }
    }
}

