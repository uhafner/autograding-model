package edu.hm.hafner.grading;

import org.apache.commons.lang3.Strings;

/**
 * Defines the scope of a tool.
 *
 * @author Jannik Ohme
 */
public enum Scope {
    PROJECT("Whole Project"),
    MODIFIED_FILES("Modified Files"),
    MODIFIED_LINES("Changed Code");

    private final String displayName;

    /**
     * Converts the given string to the corresponding Scope enum value.
     *
     * @param value
     *         the string representation of the scope
     *
     * @return the corresponding Scope enum value
     *
     * @throws IllegalArgumentException
     *         if the string does not match any Scope value
     */
    public static Scope fromString(final String value) {
        return switch (value) {
            case String s when s.isBlank() -> PROJECT;
            case String s when Strings.CI.containsAny(s, "project", "all") -> PROJECT;
            case String s when Strings.CI.contains(s, "files") -> MODIFIED_FILES;
            case String s when Strings.CI.containsAny(s, "lines", "code") -> MODIFIED_LINES;
            default -> throw new IllegalArgumentException("No such scope available: " + value);
        };
    }

    Scope(final String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
