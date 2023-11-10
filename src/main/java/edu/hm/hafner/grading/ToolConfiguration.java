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
    private final String metric;

    @SuppressWarnings("unused") // Required for JSON conversion
    ToolConfiguration() {
        this(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    public ToolConfiguration(final String id, final String name, final String pattern) {
        this(id, name, pattern, StringUtils.EMPTY);
    }

    public ToolConfiguration(final String id, final String name, final String pattern, final String metric) {
        this.id = id;
        this.name = name;
        this.pattern = pattern;
        this.metric = metric;
    }

    public String getId() {
        return StringUtils.defaultString(id);
    }

    public String getName() {
        return StringUtils.defaultString(name);
    }

    public String getPattern() {
        return StringUtils.defaultString(pattern);
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
        return Objects.equals(metric, that.metric);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (pattern != null ? pattern.hashCode() : 0);
        result = 31 * result + (metric != null ? metric.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return JACKSON_FACADE.toJson(this);
    }
}
