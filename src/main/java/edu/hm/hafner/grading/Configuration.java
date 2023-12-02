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

import one.util.streamex.StreamEx;

/**
 * Base class for configurations with a maximum score.
 *
 * @author Ullrich Hafner
 */
// TODO: make sure that the configuration is valid
public abstract class Configuration implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;

    static <T extends Configuration> List<T> extractConfigurations(
            final String json, final String id, final Class<T> type) {
        var jackson = JacksonFacade.get();

        var configurations = jackson.readJson(json);
        if (configurations.has(id)) {
            var deserialized = deserialize(id, type, configurations, jackson);
            if (deserialized.isEmpty()) {
                throw new IllegalArgumentException("Configuration ID '" + id + "' is empty in JSON: " + json);
            }
            deserialized.forEach(Configuration::validateDefaults);
//            deserialized.forEach(Configuration::validate);
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

    private String id;
    private String name;
    private String icon;
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
            throw new IllegalArgumentException("Configuration ID '" + getId() + "' has no tools");
        }
    }

//    /**
//     * Validates this configuration.
//     *
//     * @throws IllegalArgumentException if this configuration is invalid
//     */
//    protected abstract void validate();

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Configuration that = (Configuration) o;

        if (maxScore != that.maxScore) {
            return false;
        }
        if (!Objects.equals(id, that.id)) {
            return false;
        }
        if (!Objects.equals(name, that.name)) {
            return false;
        }
        if (!Objects.equals(icon, that.icon)) {
            return false;
        }
        return tools != null ? tools.equals(that.tools) : that.tools == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (icon != null ? icon.hashCode() : 0);
        result = 31 * result + maxScore;
        result = 31 * result + (tools != null ? tools.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return JacksonFacade.get().toJson(this);
    }
}
