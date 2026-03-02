package edu.hm.hafner.grading;

import org.apache.commons.lang3.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.Generated;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static edu.hm.hafner.analysis.Severity.*;

/**
 * Computes the {@link Score} impact of static analysis results. These results are obtained by summing up the number of
 * static analysis warnings.
 *
 * @author Eva-Maria Zeintl
 * @author Jannik Ohme
 * @author Ullrich Hafner
 */
public final class AnalysisScore extends Score<AnalysisScore, AnalysisConfiguration> {
    @Serial
    private static final long serialVersionUID = 3L;

    private final int errorSize;
    private final int highSeveritySize;
    private final int normalSeveritySize;
    private final int lowSeveritySize;

    private /* almost final */ int errorSizeDelta;
    private /* almost final */ int highSeveritySizeDelta;
    private /* almost final */ int normalSeveritySizeDelta;
    private /* almost final */ int lowSeveritySizeDelta;

    private transient Report report; // do not persist the issues

    private AnalysisScore(final String name, final String icon, final Scope scope,
            final AnalysisConfiguration configuration, final List<AnalysisScore> scores) {
        super(name, icon, scope, configuration, scores);

        this.errorSize = sum(scores, AnalysisScore::getErrorSize);
        this.highSeveritySize = sum(scores, AnalysisScore::getHighSeveritySize);
        this.normalSeveritySize = sum(scores, AnalysisScore::getNormalSeveritySize);
        this.lowSeveritySize = sum(scores, AnalysisScore::getLowSeveritySize);

        this.errorSizeDelta = sum(scores, AnalysisScore::getErrorSizeDelta);
        this.highSeveritySizeDelta = sum(scores, AnalysisScore::getHighSeveritySizeDelta);
        this.normalSeveritySizeDelta = sum(scores, AnalysisScore::getNormalSeveritySizeDelta);
        this.lowSeveritySizeDelta = sum(scores, AnalysisScore::getLowSeveritySizeDelta);

        this.report = new Report();

        scores.stream().map(AnalysisScore::getReport).forEach(report::addAll);
    }

    private int sum(final List<AnalysisScore> scores, final Function<AnalysisScore, Integer> property) {
        return scores.stream().map(property).reduce(Integer::sum).orElse(0);
    }

    private AnalysisScore(final String name, final String icon, final Scope scope,
            final AnalysisConfiguration configuration, final Report report, final boolean hasDelta) {
        super(name, icon, scope, configuration, hasDelta);

        this.errorSize = report.getSizeOf(ERROR);
        this.highSeveritySize = report.getSizeOf(WARNING_HIGH);
        this.normalSeveritySize = report.getSizeOf(WARNING_NORMAL);
        this.lowSeveritySize = report.getSizeOf(WARNING_LOW);

        this.report = report;
    }

    private AnalysisScore(final String name, final String icon, final Scope scope,
            final AnalysisConfiguration configuration, final Report report) {
        this(name, icon, scope, configuration, report, false);
    }

    private AnalysisScore(final String name, final String icon, final Scope scope,
            final AnalysisConfiguration configuration, final Report report, final Report deltaReport) {
        this(name, icon, scope, configuration, report, true);

        this.errorSizeDelta = this.errorSize - deltaReport.getSizeOf(ERROR);
        this.highSeveritySizeDelta = this.highSeveritySize - deltaReport.getSizeOf(WARNING_HIGH);
        this.normalSeveritySizeDelta = this.normalSeveritySize - deltaReport.getSizeOf(WARNING_NORMAL);
        this.lowSeveritySizeDelta = this.lowSeveritySize - deltaReport.getSizeOf(WARNING_LOW);
    }

    /**
     * Restore an empty report after deserialization.
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
        return ObjectUtils.getIfNull(report, new Report());
    }

    public int getReportFiles() {
        return getReport().getOriginReportFiles().size();
    }

    public Value getSize() {
        return new Value(mapType(), getTotalSize());
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

    public boolean isEmpty() {
        return getTotalSize() == 0;
    }

    public int getErrorSizeDelta() {
        return errorSizeDelta;
    }

    public int getHighSeveritySizeDelta() {
        return highSeveritySizeDelta;
    }

    public int getNormalSeveritySizeDelta() {
        return normalSeveritySizeDelta;
    }

    public int getLowSeveritySizeDelta() {
        return lowSeveritySizeDelta;
    }

    public int getTotalSizeDelta() {
        return getErrorSizeDelta() + getHighSeveritySizeDelta() + getNormalSeveritySizeDelta() + getLowSeveritySizeDelta();
    }

    private Metric mapType() {
        return switch (getReport().getElementType()) {
            case WARNING -> Metric.WARNINGS;
            case BUG -> Metric.BUGS;
            case DUPLICATION -> Metric.DUPLICATIONS;
            case VULNERABILITY -> Metric.VULNERABILITIES;
        };
    }

    @Override
    protected String createSummary() {
        if (getReport().isEmpty()) {
            return getReport().getSummary();
        }
        return getReport().getSummary() + getReport().getSeverityDistribution();
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
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
                && lowSeveritySize == that.lowSeveritySize
                && errorSizeDelta == that.errorSizeDelta
                && highSeveritySizeDelta == that.highSeveritySizeDelta
                && normalSeveritySizeDelta == that.normalSeveritySizeDelta
                && lowSeveritySizeDelta == that.lowSeveritySizeDelta;
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), errorSize, highSeveritySize, normalSeveritySize, lowSeveritySize,
                errorSizeDelta, highSeveritySizeDelta, normalSeveritySizeDelta, lowSeveritySizeDelta);
    }

    /**
     * A builder for {@link AnalysisScore} instances.
     */
    static class AnalysisScoreBuilder extends ScoreBuilder<AnalysisScore, AnalysisConfiguration> {
        AnalysisScoreBuilder() {
            this(Optional.empty());
        }

        AnalysisScoreBuilder(final Optional<Path> deltaReports) {
            super(deltaReports);
        }

        @Override
        AnalysisScore aggregate(final List<AnalysisScore> scores) {
            return new AnalysisScore(getTopLevelName(), getIcon(), getScope(), getConfiguration(), scores);
        }

        @Override
        AnalysisScore build() {
            if (hasDelta()) {
                return new AnalysisScore(getName(), getIcon(), getScope(), getConfiguration(), getReport(), getDeltaReport());
            }
            return new AnalysisScore(getName(), getIcon(), getScope(), getConfiguration(), getReport());
        }

        @Override
        void read(final ToolParser factory, final ToolConfiguration tool, final FilteredLog log) {
            readReport(factory, tool, log);
        }

        @Override
        String getType() {
            return "static analysis";
        }

        @Override
        String getDefaultName() {
            return getReport().getName();
        }

        @Override
        String getDefaultTopLevelName() {
            return "Static Analysis Results";
        }
    }
}




