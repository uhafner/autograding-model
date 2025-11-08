package edu.hm.hafner.grading;

public enum Scope {
    PROJECT,
    MODIFIED_FILES,
    MODIFIED_LINES;

    public static Scope fromString(String value) {
        for (Scope scope : values()) {
            if (scope.name().equalsIgnoreCase(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Could not find Scope: " + value);
    }
}
