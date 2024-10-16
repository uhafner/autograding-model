package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import edu.hm.hafner.util.Generated;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import one.util.streamex.StreamEx;

/**
 * Base class for configurations with a maximum score.
 *
 * @author Ullrich Hafner
 */
public abstract class Configuration implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;

    static <T extends Configuration> List<T> extractConfigurations(
            final String json, final String id, final Class<T> type) {
        var jackson = JacksonFacade.get();

        var configurations = jackson.readJson(json);
        if (configurations.has(id)) {
            var deserialized = deserialize(id, type, configurations, jackson);
            deserialized.forEach(Configuration::validateDefaults);
            deserialized.forEach(Configuration::validate);
            return deserialized;
        }
        return Collections.emptyList();
    }

    private static <T extends Configuration> List<T> deserialize(final String id, final Class<T> type,
            final JsonNode configurations, final JacksonFacade jackson) {
        var array = configurations.get(id);

        if (array.isArray()) {
            return StreamEx.of(array.iterator())
                    .map(node -> jackson.fromJson(node, type))
                    .toList();
        }
        return List.of(jackson.fromJson(array, type));
    }

    @CheckForNull @SuppressWarnings("unused") @SuppressFBWarnings("UWF_UNWRITTEN_FIELD") // Initialized via JSON
    private String id;
    @CheckForNull @SuppressWarnings("unused") @SuppressFBWarnings("UWF_UNWRITTEN_FIELD") // Initialized via JSON
    private String name;
    @CheckForNull @SuppressWarnings("unused") @SuppressFBWarnings("UWF_UNWRITTEN_FIELD") // Initialized via JSON
    private String icon;
    @SuppressWarnings("unused") @SuppressFBWarnings("UWF_UNWRITTEN_FIELD") // Initialized via JSON
    private int maxScore;

    private final List<ToolConfiguration> tools = new ArrayList<>();

    /**
     * Returns whether the impact of all properties is positive or negative.
     *
     * @return {@code true} if the impact is positive, {@code false} if the impact is negative
     */
    @JsonIgnore
    public abstract boolean isPositive();

    /**
     * Returns the unique ID of this configuration.
     *
     * @return the ID of this configuration
     */
    public String getId() {
        return StringUtils.defaultIfBlank(id, getDefaultId());
    }

    /**
     * Returns a default ID for this configuration.
     *
     * @return the default ID of this configuration
     */
    @JsonIgnore
    protected abstract String getDefaultId();

    public String getName() {
        return StringUtils.defaultIfBlank(name, getDefaultName());
    }

    /**
     * Returns a default name for this configuration.
     *
     * @return the default name of this configuration
     */
    @JsonIgnore
    protected abstract String getDefaultName();

    public String getIcon() {
        return StringUtils.defaultString(icon);
    }

    public int getMaxScore() {
        return maxScore;
    }

    public List<ToolConfiguration> getTools() {
        return tools;
    }

    private void validateDefaults() {
        if (tools.isEmpty()) {
            throwIllegalArgumentException("Configuration ID '" + getId() + "' has no tools");
        }
        if (getMaxScore() == 0 && hasImpact()) {
            throwIllegalArgumentException("When configuring impacts then the score must not be zero.");
        }
        if (getMaxScore() > 0 && !hasImpact()) {
            throwIllegalArgumentException(
                    "When configuring a max score than an impact must be defined as well.");
        }
    }

    private void throwIllegalArgumentException(final String errorMessage) {
        throw new IllegalArgumentException(errorMessage + "\nConfiguration: " + this);
    }

    /**
     * Returns whether the specified configuration has impact properties defined, or not.
     *
     * @return {@code true} if the configuration has impact properties, {@code false} if not
     */
    protected abstract boolean hasImpact();

    /**
     * Validates this configuration. This default implementation does nothing. Overwrite this method in subclasses to
     * add specific validation logic.
     *
     * @throws IllegalArgumentException if this configuration is invalid
     */
    protected void validate() {
        // empty default implementation
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
        var that = (Configuration) o;
        return maxScore == that.maxScore
                && Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(icon, that.icon)
                && Objects.equals(tools, that.tools);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(id, name, icon, maxScore, tools);
    }

    @Override
    public String toString() {
        return JacksonFacade.get().toJson(this);
    }
}
