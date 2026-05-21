package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import java.util.List;
import java.util.Objects;

/**
 * A builder for {@link Score} instances.
 *
 * @param <S>
 *         the type of the score
 * @param <C>
 *         the type of the configuration
 */
abstract class ScoreBuilder<S extends Score<S, C>, C extends Configuration> {
    static final String NO_DELTA_REPORTS = ".";

    private String name = StringUtils.EMPTY;
    private String icon = StringUtils.EMPTY;
    private String metric = StringUtils.EMPTY;
    private Scope scope = Scope.PROJECT;
    private final String deltaReportsPath;

    @CheckForNull
    private C configuration;
    @CheckForNull
    private Node node;
    @CheckForNull
    private Node deltaNode;
    @CheckForNull
    private Report report;
    @CheckForNull
    private Report deltaReport;

    protected ScoreBuilder(final String deltaReportsPath) {
        this.deltaReportsPath = deltaReportsPath;
    }

    /**
     * Sets the human-readable name of the score.
     *
     * @param name
     *         the name to show
     *
     * @return this
     */
    @CanIgnoreReturnValue
    ScoreBuilder<S, C> setName(final String name) {
        this.name = name;

        return this;
    }

    String getTopLevelName() {
        return StringUtils.defaultIfBlank(name, getDefaultTopLevelName());
    }

    abstract String getDefaultTopLevelName();

    String getName() {
        return StringUtils.defaultIfBlank(name, getDefaultName());
    }

    String getDefaultName() {
        return getMetric().getDisplayName();
    }

    /**
     * Sets the metric of the score.
     *
     * @param metric
     *         the metric to set
     *
     * @return this
     */
    @CanIgnoreReturnValue
    ScoreBuilder<S, C> setMetric(final String metric) {
        if (StringUtils.isBlank(metric)) {
            this.metric = getConfiguration().getDefaultMetric();
        }
        else {
            this.metric = metric;
        }
        return this;
    }

    Metric getMetric() {
        return Metric.fromName(metric);
    }

    /**
     * Sets the icon of the score.
     *
     * @param icon
     *         the icon to show
     *
     * @return this
     */
    @CanIgnoreReturnValue
    ScoreBuilder<S, C> setIcon(final String icon) {
        this.icon = icon;

        return this;
    }

    String getIcon() {
        return StringUtils.defaultString(icon);
    }

    /**
     * Sets the scope of the score.
     *
     * @param scope
     *         the scope to set
     *
     * @return this
     */
    @CanIgnoreReturnValue
    ScoreBuilder<S, C> setScope(final Scope scope) {
        this.scope = scope;
        return this;
    }

    Scope getScope() {
        return scope;
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
    ScoreBuilder<S, C> setConfiguration(final C configuration) {
        this.configuration = configuration;

        return this;
    }

    C getConfiguration() {
        return Objects.requireNonNull(configuration);
    }

    /**
     * Aggregates the specified scores to a single score.
     *
     * @param scores
     *         the scores to aggregate
     *
     * @return the aggregated score
     */
    abstract S aggregate(List<S> scores);

    /**
     * Builds a new score instance using the configured builder properties.
     *
     * @return the new score instance
     */
    abstract S build();

    /**
     * Returns the type of the score.
     *
     * @return the type of the score
     */
    abstract String getType();

    void readNode(final ToolParser factory, final ToolConfiguration tool,
            final FilteredLog log) {
        node = factory.readNode(tool, NO_DELTA_REPORTS, deltaReportsPath, log);
        deltaNode = readDeltaNode(factory, tool, log);

        setName(tool.getName());
        setIcon(tool.getIcon());
        setScope(tool.getScope());
        setMetric(tool.getMetric());
    }

    private Node readDeltaNode(final ToolParser factory, final ToolConfiguration tool, final FilteredLog log) {
        if (hasDelta()) {
            return factory.readNode(tool, deltaReportsPath, NO_DELTA_REPORTS, log);
        }
        return Objects.requireNonNull(node);
    }

    void readReport(final ToolParser factory, final ToolConfiguration tool,
            final FilteredLog log) {
        report = factory.readReport(tool, NO_DELTA_REPORTS, deltaReportsPath, log);
        deltaReport = readDeltaReport(factory, tool, log);

        setName(StringUtils.defaultIfBlank(tool.getName(), Objects.requireNonNull(report).getName()));
        setIcon(tool.getIcon());
        setScope(tool.getScope());
    }

    private Report readDeltaReport(final ToolParser factory, final ToolConfiguration tool, final FilteredLog log) {
        if (hasDelta()) {
            return factory.readReport(tool, deltaReportsPath, NO_DELTA_REPORTS, log);
        }
        return Objects.requireNonNull(report);
    }

    Node getNode() {
        return Objects.requireNonNull(node);
    }

    Node getDeltaNode() {
        return Objects.requireNonNull(deltaNode);
    }

    Report getReport() {
        return Objects.requireNonNull(report);
    }

    Report getDeltaReport() {
        return Objects.requireNonNull(deltaReport);
    }

    boolean hasDelta() {
        return !deltaReportsPath.equals(NO_DELTA_REPORTS);
    }

    @VisibleForTesting
    @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    S create(final Node report, final Metric metric) {
        node = report;
        deltaNode = new ContainerNode(report.getName() + "_delta");
        setMetric(metric.name());

        return build();
    }

    @VisibleForTesting
    @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    S create(final Report report) {
        this.report = report;
        this.deltaReport = new Report();

        return build();
    }

    abstract void read(ToolParser factory, ToolConfiguration tool, FilteredLog log);
}
