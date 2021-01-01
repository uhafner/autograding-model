package edu.hm.hafner.grading;

/**
 * Base class to render results in Markdown.
 *
 * @author Ullrich Hafner
 */
class ScoreMarkdown {
    private final String type;
    private final String icon;

    ScoreMarkdown(final String type, final String icon) {
        this.type = type;
        this.icon = icon;
    }

    String getNotEnabled() {
        return getTitle(" not enabled");
    }

    String getNotFound() {
        return String.format("## :construction: %s enabled but no results found :construction:%n", type);
    }
    
    String getSummary(final int score, final int total) {
        return getTitle(String.format(": %d of %d", score, total));
    }

    private String getTitle(final String message) {
        return String.format("## :%s: %s%s :%s:%n", icon, type, message, icon);
    }

    String formatColumns(final Object... columns) {
        StringBuilder row = new StringBuilder();
        for (Object column : columns) {
            row.append(String.format("|%s", column));
        }
        row.append('\n');
        return row.toString();
    }
}
