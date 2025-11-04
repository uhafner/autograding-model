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
import java.util.List;
import java.util.Objects;

import static edu.hm.hafner.analysis.Severity.*;

/**
 * Computes the {@link Score} impact of static analysis results. These results are obtained by summing up the number of
 * static analysis warnings.
 *
 * @author Eva-Maria Zeintl
 */
public final class AnalysisScore extends Score<AnalysisScore, AnalysisConfiguration> {
    @Serial
    private static final long serialVersionUID = 3L;

    private final int errorSize;
    private final int highSeveritySize;
    private final int normalSeveritySize;
    private final int lowSeveritySize;

    private transient Report report; // do not persist the issues

    private AnalysisScore(final String name, final String icon, final AnalysisConfiguration configuration,
            final List<AnalysisScore> scores) {
        super(name, icon, configuration, scores.toArray(new AnalysisScore[0]));

        this.errorSize = scores.stream().reduce(0, (sum, score) -> sum + score.getErrorSize(), Integer::sum);
        this.highSeveritySize = scores.stream().reduce(0, (sum, score) -> sum + score.getHighSeveritySize(), Integer::sum);
        this.normalSeveritySize = scores.stream().reduce(0, (sum, score) -> sum + score.getNormalSeveritySize(), Integer::sum);
        this.lowSeveritySize = scores.stream().reduce(0, (sum, score) -> sum + score.getLowSeveritySize(), Integer::sum);

        this.report = new Report();

        scores.stream().map(AnalysisScore::getReport).forEach(report::addAll);
    }

    private AnalysisScore(final String name, final String icon, final AnalysisConfiguration configuration,
            final Report report) {
        super(name, icon, configuration);

        this.errorSize = report.getSizeOf(ERROR);
        this.highSeveritySize = report.getSizeOf(WARNING_HIGH);
        this.normalSeveritySize = report.getSizeOf(WARNING_NORMAL);
        this.lowSeveritySize = report.getSizeOf(WARNING_LOW);

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
        @Override
        public AnalysisScore aggregate(final List<AnalysisScore> scores) {
            return new AnalysisScore(getTopLevelName(), getIcon(), getConfiguration(), scores);
        }

        @Override
        public AnalysisScore build() {
            return new AnalysisScore(getName(), getIcon(), getConfiguration(), getReport());
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
