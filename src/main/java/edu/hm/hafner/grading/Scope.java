package edu.hm.hafner.grading;

import java.util.Locale;

/**
 * Defines the scope of a tool.
 *
 * @author Jannik Ohme
 */
public enum Scope {
    PROJECT,
    MODIFIED_FILES,
    MODIFIED_LINES;

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
        String normalized = normalize(value);
        for (Scope scope : values()) {
            if (normalized.equals(scope.toString())) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Could not find Scope: " + value);
    }

    @Override
    public String toString() {
        return normalize(name());
    }

    private static String normalize(final String name) {
        return name.toLowerCase(Locale.ENGLISH);
    }
}
