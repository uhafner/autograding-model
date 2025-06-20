package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.Generated;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

    @CheckForNull
    @SuppressFBWarnings("UWF_UNWRITTEN_FIELD") // Initialized via JSON
    private String name;
    @CheckForNull
    @SuppressFBWarnings("UWF_UNWRITTEN_FIELD") // Initialized via JSON
    private String icon;
    @CheckForNull
    @SuppressFBWarnings("UWF_UNWRITTEN_FIELD") // Initialized via JSON
    private String sourcePath;
    @SuppressFBWarnings("UWF_UNWRITTEN_FIELD") // Initialized via JSON
    private int maxScore;
    private final List<ToolConfiguration> tools = new ArrayList<>(); // Initialized via JSON

    public List<ToolConfiguration> getTools() {
        return List.copyOf(tools);
    }

    /**
     * Returns whether the impact of all properties is positive or negative.
     *
     * @return {@code true} if the impact is positive, {@code false} if the impact is negative
     */
    @JsonIgnore
    public abstract boolean isPositive();

    public String getName() {
        return StringUtils.defaultIfBlank(name, getDefaultName());
    }

    /**
     * Returns the default metric for this configuration.
     *
     * @return the default metric
     */
    public String getDefaultMetric() {
        return StringUtils.EMPTY;
    }

    /**
     * Returns a default name for this configuration.
     *
     * @return the default name of this configuration
     */
    @JsonIgnore
    protected abstract String getDefaultName();

    public String getSourcePath() {
        return StringUtils.defaultString(sourcePath);
    }

    public String getIcon() {
        return StringUtils.defaultString(icon);
    }

    public int getMaxScore() {
        return maxScore;
    }

    private void validateDefaults() {
        Ensure.that(getMaxScore() == 0 && hasImpact()).isFalse("%s: %s%n%s",
                getName(), "When configuring impacts then the score must not be zero.", toString());
        Ensure.that(getMaxScore() > 0 && !hasImpact()).isFalse("%s: %s%n%s",
                getName(), "When configuring a score then an impact must be defined as well.", toString());
        Ensure.that(tools).isNotEmpty("%s: %s%n%s",
                getName(), "No tools configured.", toString());

        tools.forEach(this::validate);
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
     * @throws IllegalArgumentException
     *         if this configuration is invalid
     */
    protected void validate() {
        // default implementation does nothing
    }

    /**
     * Validates a tool specified in this configuration. This default implementation does nothing. Overwrite this method
     * in subclasses to add specific validation logic.
     *
     * @param tool
     *         the tool to validate
     *
     * @throws IllegalArgumentException
     *         if this configuration is invalid
     */
    protected void validate(final ToolConfiguration tool) {
        // default implementation does nothing
    }

    @Override
    public String toString() {
        return JacksonFacade.get().toJson(this);
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (Configuration) o;
        return maxScore == that.maxScore
                && Objects.equals(name, that.name)
                && Objects.equals(icon, that.icon)
                && Objects.equals(sourcePath, that.sourcePath)
                && Objects.equals(tools, that.tools);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(name, icon, sourcePath, maxScore, tools);
    }
}
