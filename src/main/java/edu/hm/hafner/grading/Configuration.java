package edu.hm.hafner.grading;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import one.util.streamex.StreamEx;

/**
 * Base class for configurations with a maximum score.
 *
 * @author Ullrich Hafner
 */
// FIXME: make sure that the configuration is valid
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

    @Override @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Configuration that = (Configuration) o;

        if (getMaxScore() != that.getMaxScore()) {
            return false;
        }
        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) {
            return false;
        }
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) {
            return false;
        }
        return getTools() != null ? getTools().equals(that.getTools()) : that.getTools() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + getMaxScore();
        result = 31 * result + (getTools() != null ? getTools().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return JacksonFacade.get().toJson(this);
    }
}
