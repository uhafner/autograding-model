package edu.hm.hafner.grading;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.errorprone.annotations.FormatMethod;

import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Base class to render results in Markdown.
 *
 * @param <S>
 *         the {@link Score} type
 * @param <C>
 *         the associated {@link Configuration} type
 *
 * @author Ullrich Hafner
 */
abstract class ScoreMarkdown<S extends Score<S, C>, C extends Configuration> {
    protected static final int ICON_SIZE = 18;
    static final String SPACE = "&nbsp;";
    static final String LINE_BREAK = "\n";
    static final String LEDGER = ":heavy_minus_sign:";
    static final String IMPACT = ":moneybag:";
    static final String TOTAL = ":heavy_minus_sign:";
    static final String EMPTY = ":heavy_minus_sign:";

    static final String N_A = "-";
    static final int CAPACITY = 1024;

    private static final int MAX_SIZE = 10_000; // limit the size of the output to this number of characters
    private static final String TRUNCATION_TEXT = "\n\nToo many test failures. Grading output truncated.";
    private static final int HUNDRED_PERCENT = 100;

    private final String type;
    private final String icon;

    ScoreMarkdown(final String type, final String icon) {
        this.type = type;
        this.icon = icon;
    }

    /**
     * Creates a percentage image tag.
     *
     * @param title
     *         the title of the image
     * @param percentage
     *         the percentage to show
     *
     * @return Markdown text
     */
    @SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE",
            justification = "Output is Unix anyway")
    public static String getPercentageImage(final String title, final int percentage) {
        if (percentage < 0 || percentage > HUNDRED_PERCENT) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100: " + percentage);
        }
        return format("""
                <img title="%s: %d%%" width="110" height="110"
                        align="left" alt="%s: %d%%"
                        src="https://raw.githubusercontent.com/uhafner/autograding-model/main/percentages/%03d.svg" />
                """, title, percentage, title, percentage, percentage);
    }

    String getPercentageImage(final Score<?, ?> score) {
        if (score.hasMaxScore()) {
            return getPercentageImage(score.getDisplayName(), score.getPercentage());
        }
        return StringUtils.EMPTY;
    }

    /**
     * Renders the score details in Markdown.
     *
     * @param aggregation
     *         aggregated score
     *
     * @return formatted Markdown
     */
    public String createDetails(final AggregatedScore aggregation) {
        var scores = createScores(aggregation);
        if (scores.isEmpty()) {
            return createNotEnabled();
        }

        var details = new TruncatedStringBuilder().withTruncationText(TRUNCATION_TEXT);
        createSpecificDetails(aggregation, scores, details);
        return details.build().buildByChars(MAX_SIZE);
    }

    /**
     * Renders the score details of the specific scores in Markdown.
     *
     * @param aggregation
     *         aggregated score
     * @param scores
     *         the scores to render the details for
     * @param details
     *         the details Markdown
     */
    protected abstract void createSpecificDetails(AggregatedScore aggregation, List<S> scores, TruncatedStringBuilder details);

    /**
     * Renders a summary of all scores in Markdown.
     *
     * @param aggregation
     *         aggregated score
     *
     * @return returns the summary in Markdown
     */
    public String createSummary(final AggregatedScore aggregation) {
        var scores = createScores(aggregation);
        if (scores.isEmpty()) {
            return createNotEnabled();
        }

        var summary = new StringBuilder(CAPACITY);
        for (S score : scores) {
            summary.append(createSummary(score));
        }
        return summary.toString();
    }

    protected abstract String createSummary(S score);

    /**
     * Creates the scores to render.
     *
     * @param aggregation
     *         the aggregated score
     *
     * @return the scores
     */
    protected abstract List<S> createScores(AggregatedScore aggregation);

    protected String getTitle(final S score, final int size) {
        return "#".repeat(size)
                + String.format(" %s &nbsp; %s", getIcon(score), score.getName())
                + createScoreTitle(score);
    }

    protected String createScoreTitle(final S score) {
        var maxScore = score.getMaxScore();
        var value = score.getValue();
        var percentage = score.getPercentage();
        return createScoreTitleSuffix(maxScore, value, percentage);
    }

    static String createScoreTitleSuffix(final int maxScore, final int value, final int percentage) {
        if (maxScore == 0) {
            return StringUtils.EMPTY;
        }
        if (maxScore == HUNDRED_PERCENT) {
            return format(" - %d of %d", value, maxScore); // no need to show percentage for a score of 100
        }
        return format(" - %d of %d (%d%%)", value, maxScore, percentage);
    }

    protected String getIcon(final S score) {
        return ":%s:".formatted(StringUtils.defaultIfBlank(score.getConfiguration().getIcon(), icon));
    }

    String formatColumns(final Object... columns) {
        return format(i -> i, columns);
    }

    String formatItalicColumns(final Object... columns) {
        return format(s -> "*" + s + "*", columns);
    }

    String formatBoldColumns(final Object... columns) {
        return format(s -> "**" + s + "**", columns);
    }

    /**
     * Returns a formatted string using the specified format string and
     * arguments. The English locale is always used to format the string.
     *
     * @param  format
     *         A <a href="../util/Formatter.html#syntax">format string</a>
     *
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The number of arguments is
     *         variable and may be zero.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *         The behaviour on a
     *         {@code null} argument depends on the <a
     *         href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @throws  java.util.IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the <a
     *          href="../util/Formatter.html#detail">Details</a> section of the
     *          formatter class specification.
     *
     * @return  A formatted string
     *
     * @see  java.util.Formatter
     * @since  1.5
     */
    @FormatMethod
    protected static String format(final String format, final Object... args) {
        return String.format(Locale.ENGLISH, format, args);
    }

    private String format(final Function<String, String> textFormatter, final Object... columns) {
        return Arrays.stream(columns)
                .map(Object::toString)
                .map(textFormatter)
                .collect(Collectors.joining("|", "|", ""));
    }

    protected String renderImpact(final int impact) {
        if (impact == 0) {
            return N_A;
        }
        else {
            return String.valueOf(impact);
        }
    }

    protected String createNotEnabled() {
        return String.format("## :%s: %s%s %n", icon, type, ": not enabled");
    }
}
