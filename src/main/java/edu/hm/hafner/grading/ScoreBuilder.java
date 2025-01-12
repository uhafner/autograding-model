package edu.hm.hafner.grading;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;

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
    private C configuration;
    private Node node;

    /**
     * Sets the human-readable name of the analysis score.
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

    String getName() {
        return StringUtils.defaultIfBlank(name, getConfiguration().getName());
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

    abstract S aggregate(List<S> scores);

    abstract S create(Metric metric);

    abstract String getType();

    public void read(final ToolParser factory, final ToolConfiguration tool,
            final FilteredLog log) {
        node = factory.readNode(tool, log);
    }

    public Node getNode() {
        return node;
    }

    @VisibleForTesting
    S create(final Node report, final Metric metric) {
        node = report;

        return create(metric);
    }
}
