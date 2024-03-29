package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

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
    private final String pattern;
    private final String sourcePath;
    private final String metric;

    @SuppressWarnings("unused") // Required for JSON conversion
    private ToolConfiguration() {
        this(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    ToolConfiguration(final String id, final String name, final String pattern, final String sourcePath) {
        this(id, name, pattern, sourcePath, StringUtils.EMPTY);
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
     * @param sourcePath
     *         the source path to find the affected files
     * @param metric
     *         the metric to extract from the report
     */
    public ToolConfiguration(final String id, final String name, final String pattern, final String sourcePath,
            final String metric) {
        this.id = id;
        this.name = name;
        this.pattern = pattern;
        this.metric = metric;
        this.sourcePath = sourcePath;
    }

    public String getId() {
        return StringUtils.defaultString(id);
    }

    public String getName() {
        return StringUtils.defaultString(name);
    }

    public String getDisplayName() {
        return StringUtils.defaultIfEmpty(getName(), getId());
    }

    public String getPattern() {
        return StringUtils.defaultString(pattern);
    }

    public String getSourcePath() {
        return StringUtils.defaultString(sourcePath);
    }

    public String getMetric() {
        return StringUtils.defaultString(metric);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ToolConfiguration that = (ToolConfiguration) o;

        if (!Objects.equals(id, that.id)) {
            return false;
        }
        if (!Objects.equals(name, that.name)) {
            return false;
        }
        if (!Objects.equals(pattern, that.pattern)) {
            return false;
        }
        if (!Objects.equals(sourcePath, that.sourcePath)) {
            return false;
        }
        return Objects.equals(metric, that.metric);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (pattern != null ? pattern.hashCode() : 0);
        result = 31 * result + (sourcePath != null ? sourcePath.hashCode() : 0);
        result = 31 * result + (metric != null ? metric.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return JACKSON_FACADE.toJson(this);
    }
}
