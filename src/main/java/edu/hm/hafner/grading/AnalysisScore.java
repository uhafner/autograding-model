package edu.hm.hafner.grading;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.Generated;
import org.apache.commons.lang3.ObjectUtils;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static edu.hm.hafner.analysis.Severity.*;

/**
 * Computes the {@link Score} impact of static analysis results. These results are obtained by summing up the number of
 * static analysis warnings.
 *
 * @author Eva-Maria Zeintl
 * @author Jannik Ohme
 */
public final class AnalysisScore extends Score<AnalysisScore, AnalysisConfiguration> {
    @Serial
    private static final long serialVersionUID = 3L;

    private final int errorSize;
    private final int highSeveritySize;
    private final int normalSeveritySize;
    private final int lowSeveritySize;

    private final int errorSizeDelta;
    private final int highSeveritySizeDelta;
    private final int normalSeveritySizeDelta;
    private final int lowSeveritySizeDelta;

    private transient Report report; // do not persist the issues

    private AnalysisScore(final String name, final String icon, final Scope scope, final AnalysisConfiguration configuration,
            final List<AnalysisScore> scores) {
        super(name, icon, scope, configuration, scores.toArray(new AnalysisScore[0]));

        this.errorSize = scores.stream().reduce(0, (sum, score) -> sum + score.getErrorSize(), Integer::sum);
        this.highSeveritySize = scores.stream().reduce(0, (sum, score) -> sum + score.getHighSeveritySize(), Integer::sum);
        this.normalSeveritySize = scores.stream().reduce(0, (sum, score) -> sum + score.getNormalSeveritySize(), Integer::sum);
        this.lowSeveritySize = scores.stream().reduce(0, (sum, score) -> sum + score.getLowSeveritySize(), Integer::sum);

        this.errorSizeDelta = scores.stream().reduce(0, (sum, score) -> sum + score.getErrorSizeDelta(), Integer::sum);
        this.highSeveritySizeDelta = scores.stream().reduce(0, (sum, score) -> sum + score.getHighSeveritySizeDelta(), Integer::sum);
        this.normalSeveritySizeDelta = scores.stream().reduce(0, (sum, score) -> sum + score.getNormalSeveritySizeDelta(), Integer::sum);
        this.lowSeveritySizeDelta = scores.stream().reduce(0, (sum, score) -> sum + score.getLowSeveritySizeDelta(), Integer::sum);

        this.report = new Report();

        scores.stream().map(AnalysisScore::getReport).forEach(report::addAll);
    }

    private AnalysisScore(final String name, final String icon, final Scope scope, final AnalysisConfiguration configuration,
            final Report report, final Report deltaReport) {
        super(name, icon, scope, configuration);

        this.errorSize = report.getSizeOf(ERROR);
        this.highSeveritySize = report.getSizeOf(WARNING_HIGH);
        this.normalSeveritySize = report.getSizeOf(WARNING_NORMAL);
        this.lowSeveritySize = report.getSizeOf(WARNING_LOW);

        this.errorSizeDelta = this.errorSize - deltaReport.getSizeOf(ERROR);
        this.highSeveritySizeDelta = this.highSeveritySize - deltaReport.getSizeOf(WARNING_HIGH);
        this.normalSeveritySizeDelta = this.normalSeveritySize - deltaReport.getSizeOf(WARNING_NORMAL);
        this.lowSeveritySizeDelta = this.lowSeveritySize - deltaReport.getSizeOf(WARNING_LOW);

        this.report = report;
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
        return getReport().getSummary() + getReport().getSeverityDistribution();
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
    static class AnalysisScoreBuilder extends ScoreBuilder<AnalysisScore, AnalysisConfiguration> {
        public AnalysisScoreBuilder() {
            this(Optional.empty());
        }

        public AnalysisScoreBuilder(final Optional<Path> deltaReports) {
            super(deltaReports);
        }

        @Override
        public AnalysisScore aggregate(final List<AnalysisScore> scores) {
            return new AnalysisScore(getTopLevelName(), getIcon(), getScope(), getConfiguration(), scores);
        }

        @Override
        public AnalysisScore build() {
            return new AnalysisScore(getName(), getIcon(), getScope(), getConfiguration(), getReport(), getDeltaReport());
        }

        @Override
        public void read(final ToolParser factory, final ToolConfiguration tool, final FilteredLog log) {
            readReport(factory, tool, log);
        }

        @Override
        public String getType() {
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
