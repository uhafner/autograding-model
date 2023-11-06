package edu.hm.hafner.grading;

/**
 * Base class to render results in Markdown.
 *
 * @author Ullrich Hafner
 */
class ScoreMarkdown {
    static final String LEDGER = ":ledger:";
    static final String IMPACT = ":moneybag:";
    static final String N_A = "-";

    static final int MESSAGE_INITIAL_CAPACITY = 1024;

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

    protected String getTitle(final String message) {
        return getTitle(message, type);
    }

    protected String getTitle(final String message, final String name) {
        return String.format("## :%s: %s%s :%s:%n", icon, name, message, icon);
    }

    String formatColumns(final Object... columns) {
        return format("|%s", columns);
    }

    String formatItalicColumns(final Object... columns) {
        return format("|*%s*", columns);
    }

    String formatBoldColumns(final Object... columns) {
        return format("|**%s**", columns);
    }

    private String format(final String format, final Object... columns) {
        var row = new StringBuilder(MESSAGE_INITIAL_CAPACITY);
        for (Object column : columns) {
            row.append(String.format(format, column));
        }
        row.append('\n');
        return row.toString();
    }

    protected String renderImpact(final int impact) {
        if (impact == 0) {
            return N_A;
        }
        else {
            return String.valueOf(impact);
        }
    }

}
