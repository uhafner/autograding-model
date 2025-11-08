package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.hm.hafner.util.Generated;

/**
 * A tool configuration provides an identifier and report pattern for a specific development tool.
 *
 * @author Ullrich Hafner
 */
public final class ToolConfiguration implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;

    private static final JacksonFacade JACKSON_FACADE = new JacksonFacade();

    private final String id;
    private final String name;
    private final String icon;
    private final String pattern;
    private final String metric;
    private final String sourcePath;
    private final String baseline;

    @SuppressWarnings("unused") // Required for JSON conversion
    private ToolConfiguration() {
        this(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
                StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    ToolConfiguration(final String id, final String name, final String pattern) {
        this(id, name, pattern, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    /**
     * Creates a new {@link ToolConfiguration} instance.
     *
     * @param id
     *         the unique ID of the tool
     * @param name
     *         the human-readable name of the tool
     * @param pattern
     *         the Ant-style pattern to find the reports
     * @param metric
     *         the metric to extract from the report
     * @param icon
     *         the icon to use for this tool
     */
    public ToolConfiguration(final String id, final String name, final String pattern,
            final String metric, final String icon) { // TODO maybe delete
        this(id, name, pattern, metric, icon, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    public ToolConfiguration(final String id, final String name, final String pattern,
                             final String metric, final String icon, final String sourcePath, final String baseline) {
        this.id = id;
        this.name = name;
        this.pattern = pattern;
        this.metric = metric;
        this.icon = icon;
        this.sourcePath = sourcePath;
        this.baseline = baseline;
    }

    public String getId() {
        return StringUtils.defaultString(id);
    }

    public String getName() {
        return StringUtils.defaultString(name);
    }

    @JsonIgnore
    public String getDisplayName() {
        return StringUtils.defaultIfEmpty(getName(), getId());
    }

    public String getPattern() {
        return StringUtils.defaultString(pattern);
    }

    public String getMetric() {
        return StringUtils.defaultString(metric);
    }

    public String getIcon() {
        return StringUtils.defaultString(icon);
    }

    public String getSourcePath() {
        return StringUtils.defaultString(sourcePath);
    }

    public String getBaseline() {
        return StringUtils.defaultIfEmpty(baseline, Baseline.PROJECT.toString());
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
        var that = (ToolConfiguration) o;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(icon, that.icon)
                && Objects.equals(pattern, that.pattern)
                && Objects.equals(metric, that.metric)
                && Objects.equals(sourcePath, that.sourcePath)
                && Objects.equals(baseline, that.baseline);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(id, name, icon, pattern, metric, sourcePath, baseline);
    }

    @Override
    public String toString() {
        return JACKSON_FACADE.toJson(this);
    }
}
