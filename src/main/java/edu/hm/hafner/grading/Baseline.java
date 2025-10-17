package edu.hm.hafner.grading;

public enum Baseline {
    PROJECT,
    MODIFIED_FILES,
    MODIFIED_LINES;

    public static Baseline fromString(String value) {
        for (Baseline baseline : values()) {
            if (baseline.name().equalsIgnoreCase(value)) {
                return baseline;
            }
        }
        return PROJECT;
    }
}
