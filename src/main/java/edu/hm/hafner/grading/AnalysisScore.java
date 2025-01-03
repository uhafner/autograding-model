package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.analysis.registry.ParserDescriptor;
import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.Generated;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import static edu.hm.hafner.analysis.Severity.*;

/**
 * Computes the {@link Score} impact of static analysis results. These results are obtained by summing up the number of
 * static analysis warnings.
 *
 * @author Eva-Maria Zeintl
 */
@SuppressWarnings("PMD.DataClass")
public final class AnalysisScore extends Score<AnalysisScore, AnalysisConfiguration> {
    private static final ParserRegistry REGISTRY = new ParserRegistry();

    @Serial
    private static final long serialVersionUID = 3L;

    private final int errorSize;
    private final int highSeveritySize;
    private final int normalSeveritySize;
    private final int lowSeveritySize;

    private transient Report report; // do not persist the issues

    private AnalysisScore(final String id, final String name, final String icon, final AnalysisConfiguration configuration,
            final List<AnalysisScore> scores) {
        super(id, name, icon, configuration, scores.toArray(new AnalysisScore[0]));

        this.errorSize = scores.stream().reduce(0, (sum, score) -> sum + score.getErrorSize(), Integer::sum);
        this.highSeveritySize = scores.stream().reduce(0, (sum, score) -> sum + score.getHighSeveritySize(), Integer::sum);
        this.normalSeveritySize = scores.stream().reduce(0, (sum, score) -> sum + score.getNormalSeveritySize(), Integer::sum);
        this.lowSeveritySize = scores.stream().reduce(0, (sum, score) -> sum + score.getLowSeveritySize(), Integer::sum);

        this.report = new Report();

        scores.stream().map(AnalysisScore::getReport).forEach(report::addAll);
    }

    private AnalysisScore(final String id, final String name, final String icon, final AnalysisConfiguration configuration,
            final Report report) {
        super(id, name, icon, configuration);

        this.errorSize = report.getSizeOf(ERROR);
        this.highSeveritySize = report.getSizeOf(WARNING_HIGH);
        this.normalSeveritySize = report.getSizeOf(WARNING_NORMAL);
        this.lowSeveritySize = report.getSizeOf(WARNING_LOW);

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

    /**
     * Returns the number of issues with the specified severity.
     *
     * @param severity the severity to get the size for
     * @return the number of issues with the specified severity
     */
    public int getSize(final Severity severity) {
        return switch (severity.getName()) {
            case "ERROR" -> getErrorSize();
            case "HIGH" -> getHighSeveritySize();
            case "NORMAL" -> getNormalSeveritySize();
            case "LOW" -> getLowSeveritySize();
            default -> throw new IllegalStateException("Unexpected severity: " + severity.getName());
        };
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
            return "No " + getItemCount(0);
        }
        else {
            var warnings = format("%d %s", getTotalSize(), getItemCount(getTotalSize()));
            var details = getPredefinedValues()
                    .stream()
                    .map(this::reportSeverity)
                    .flatMap(Optional::stream)
                    .collect(Collectors.joining(", "));
            return warnings + " (" + details + ")";
        }
    }

    private Optional<String> reportSeverity(final Severity severity) {
        var size = getSize(severity);
        if (size > 0) {
            return Optional.of(format("%s: %d", StringUtils.lowerCase(severity.getName()), size));
        }
        return Optional.empty();
    }

    private String getItemCount(final int count) {
        if (REGISTRY.contains(getId())
                && REGISTRY.get(getId()).getType() == ParserDescriptor.Type.VULNERABILITY) {
            if (count == 1) {
                return "vulnerability";
            }
            return "vulnerabilities";
        }
        return getItemName() + plural(count);
    }

    private String getItemName() {
        if (REGISTRY.contains(getId())) {
            return switch (REGISTRY.get(getId()).getType()) {
                case WARNING -> "warning";
                case BUG -> "bug";
                case DUPLICATION -> "duplication";
                case VULNERABILITY -> "vulnerability";
            };
        }
        return "warning"; // default name
    }

    static String plural(final int score) {
        if (score == 1) {
            return StringUtils.EMPTY;
        }
        return "s";
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
        var that = (AnalysisScore) o;
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
        private String icon = StringUtils.EMPTY;
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
         * Sets the icon of the analysis score.
         *
         * @param icon
         *         the icon to show
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public AnalysisScoreBuilder withIcon(final String icon) {
            this.icon = icon;
            return this;
        }

        private String getIcon() {
            return StringUtils.defaultString(icon);
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
                return new AnalysisScore(getId(), getName(), getIcon(), getConfiguration(), scores);
            }
            return new AnalysisScore(getId(), getName(), getIcon(), getConfiguration(), Objects.requireNonNull(report));
        }
    }
}
