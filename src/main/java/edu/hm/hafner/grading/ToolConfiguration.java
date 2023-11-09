package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;

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

    @SuppressWarnings("unused") // Required for JSON conversion
    ToolConfiguration() {
        this(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    public ToolConfiguration(final String id, final String name, final String pattern) {
        this.id = id;
        this.name = name;
        this.pattern = pattern;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPattern() {
        return pattern;
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

        if (!getId().equals(that.getId())) {
            return false;
        }
        if (!getName().equals(that.getName())) {
            return false;
        }
        return getPattern().equals(that.getPattern());
    }

    @Override
    public int hashCode() {
        int result = getId().hashCode();
        result = 31 * result + getName().hashCode();
        result = 31 * result + getPattern().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return JACKSON_FACADE.toJson(this);
    }
}
