package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.analysis.Report;
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
    private String name = StringUtils.EMPTY;
    private String icon = StringUtils.EMPTY;
    private String metric = StringUtils.EMPTY;
    private String scope = StringUtils.EMPTY;

    @CheckForNull
    private C configuration;
    @CheckForNull
    private Node node;
    @CheckForNull
    private Node deltaNode;
    @CheckForNull
    private Report report;

    /**
     * Sets the human-readable name of the score.
     *
     * @param name
     *         the name to show
     *
     * @return this
     */
    @CanIgnoreReturnValue
    public ScoreBuilder<S, C> setName(final String name) {
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
    public ScoreBuilder<S, C> setMetric(final String metric) {
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
     * Sets the icon of the test score.
     *
     * @param icon
     *         the icon to show
     *
     * @return this
     */
    @CanIgnoreReturnValue
    public ScoreBuilder<S, C> setIcon(final String icon) {
        this.icon = icon;

        return this;
    }

    String getIcon() {
        return StringUtils.defaultString(icon);
    }

    @CanIgnoreReturnValue
    public ScoreBuilder<S, C> setScope(final String scope) {
        this.scope = scope;
        return this;
    }

    String getScope() {
        return StringUtils.defaultIfBlank(scope, Scope.PROJECT.toString());
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
    public ScoreBuilder<S, C> setConfiguration(final C configuration) {
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
    public abstract S aggregate(List<S> scores);

    /**
     * Builds a new score instance using the configured builder properties.
     *
     * @return the new score instance
     */
    public abstract S build();

    /**
     * Returns the type of the score.
     *
     * @return the type of the score
     */
    public abstract String getType();

    void readNode(final ToolParser factory, final ToolConfiguration tool,
            final FilteredLog log) {
        node = factory.readNode(tool, ".", log);
        deltaNode = factory.readNode(tool, System.getProperty("java.io.tmpdir"), log);

        setName(tool.getName());
        setIcon(tool.getIcon());
        setScope(tool.getScope());
        setMetric(tool.getMetric());
    }

    void readReport(final ToolParser factory, final ToolConfiguration tool,
            final FilteredLog log) {
        report = factory.readReport(tool, log);

        setName(StringUtils.defaultIfBlank(tool.getName(), report.getName()));
        setIcon(tool.getIcon());
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

    @VisibleForTesting
    @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    S create(final Node report, final Metric metric) {
        node = report;
        setMetric(metric.name());

        return build();
    }

    @VisibleForTesting
    @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    S create(final Report report) {
        this.report = report;

        return build();
    }

    public abstract void read(ToolParser factory, ToolConfiguration tool, FilteredLog log);
}
