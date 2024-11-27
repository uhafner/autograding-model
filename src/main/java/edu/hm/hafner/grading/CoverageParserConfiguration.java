package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.util.Generated;

/**
 * A tool configuration provides an identifier and report pattern for a specific development tool.
 *
 * @author Ullrich Hafner
 */
public final class CoverageParserConfiguration implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;

    private static final JacksonFacade JACKSON_FACADE = new JacksonFacade();

    private final String name;
    private final String icon;
    private final String metric;

    @SuppressWarnings("unused") // Required for JSON conversion
    private CoverageParserConfiguration() {
        this(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    /**
     * Creates a new {@link CoverageParserConfiguration} instance.
     *
     * @param name
     *         the human-readable name of the tool
     * @param icon
     *         the icon to use for this tool
     * @param metric
     *         the metric to extract from the report
     */
    public CoverageParserConfiguration(final String name, final String icon, final String metric) {
        this.name = name;
        this.metric = metric;
        this.icon = icon;
    }

    /**
     * Creates a new {@link CoverageParserConfiguration} instance.
     *
     * @param name
     *         the human-readable name of the tool
     * @param icon
     *         the icon to use for this tool
     * @param metric
     *         the metric to extract from the report
     */
    public CoverageParserConfiguration(final String name, final String icon, final Metric metric) {
        this(name, icon, metric.name());
    }

    public String getName() {
        return StringUtils.defaultString(name);
    }

    public String getDisplayName() {
        return StringUtils.defaultIfEmpty(getName(), "FIXME");
    }

    public String getIcon() {
        return StringUtils.defaultString(icon);
    }

    public String getMetric() {
        return StringUtils.defaultString(metric);
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
        var that = (CoverageParserConfiguration) o;
        return Objects.equals(name, that.name)
                && Objects.equals(icon, that.icon)
                && Objects.equals(metric, that.metric);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(name, icon, metric);
    }

    @Override
    public String toString() {
        return JACKSON_FACADE.toJson(this);
    }
}
