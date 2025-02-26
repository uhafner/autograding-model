package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import com.google.errorprone.annotations.FormatMethod;

import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    static final String LINE_BREAK_PARAGRAPH = "\\\n";
    static final String LINE_BREAK = "\n";
    static final String HORIZONTAL_RULE = "<hr />\n\n";
    static final String PARAGRAPH = "\n\n";
    static final String LEDGER = ":heavy_minus_sign:";
    static final String IMPACT = ":moneybag:";
    static final String TOTAL = ":heavy_minus_sign:";
    static final String EMPTY = ":heavy_minus_sign:";
    static final int DEFAULT_PERCENTAGE_SIZE = 110;

    static final String N_A = "-";

    private static final int MAX_SIZE = 10_000; // limit the size of the output to this number of characters
    private static final String TRUNCATION_TEXT = "\n\nToo many test failures. Grading output truncated.\n\n";
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
        return getPercentageImage(title, percentage, DEFAULT_PERCENTAGE_SIZE);
    }

    /**
     * Creates a percentage image tag.
     *
     * @param title
     *         the title of the image
     * @param percentage
     *         the percentage to show
     * @param size
     *         the size of the image
     *
     * @return Markdown text
     */
    @SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE",
            justification = "Output is Unix anyway")
    public static String getPercentageImage(final String title, final int percentage, final int size) {
        if (percentage < 0 || percentage > HUNDRED_PERCENT) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100: " + percentage);
        }
        return format("""
                <img title="%s: %d%%" width="%d" height="%d"
                        align="left" alt="%s: %d%%"
                        src="https://raw.githubusercontent.com/uhafner/autograding-model/main/percentages/%03d.svg" />
                """, title, percentage, size, size, title, percentage, percentage);
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
        return createDetails(aggregation, false);
    }

    /**
     * Renders the score details in Markdown.
     *
     * @param aggregation
     *         aggregated score
     * @param showDisabled
     *         determines whether disabled scores should be shown or skipped
     *
     * @return formatted Markdown
     */
    public String createDetails(final AggregatedScore aggregation, final boolean showDisabled) {
        var scores = createScores(aggregation);
        if (scores.isEmpty()) {
            return createNotEnabled(showDisabled);
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
     * Renders a summary of all sub-scores in Markdown.
     *
     * @param aggregation
     *         aggregated score
     *
     * @return returns the summary in Markdown
     */
    public String createSummary(final AggregatedScore aggregation) {
        return createSummary(aggregation, false);
    }

    /**
     * Renders a summary of all sub-scores in Markdown.
     *
     * @param aggregation
     *         aggregated score
     * @param showHeaders
     *        determines whether headers should be shown for the subsections or not
     *
     * @return returns the summary in Markdown
     */
    public String createSummary(final AggregatedScore aggregation, final boolean showHeaders) {
        var summaries = new StringBuilder(1024);
        for (S score : createScores(aggregation)) {
            if (showHeaders) {
                summaries.append(getTitle(score, 3)).append(PARAGRAPH);
            }
            var subScores = createSummaryOfSubScores(score);
            summaries.append(String.join(LINE_BREAK_PARAGRAPH, subScores));
        }
        return summaries.toString();
    }

    private List<String> createSummaryOfSubScores(final S score) {
        return score.getSubScores().stream()
                .map(s -> SPACE + SPACE + getTitle(s, 0) + ": " + createScoreSummary(s)).toList();
    }

    protected String createScoreSummary(final S s) {
        return s.createSummary();
    }

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
                + " %s &nbsp; %s".formatted(getIcon(score), score.getName())
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

    private String getIcon(final S score) {
        var scoreIcon = score.getIcon();
        if (StringUtils.isNotBlank(scoreIcon)) {
            return emoji(scoreIcon);
        }

        return getToolIcon(score);
    }

    protected abstract String getToolIcon(S score);

    protected String getDefaultIcon(final S score) {
        var configuredIcon = score.getConfiguration().getIcon();
        if (StringUtils.isNotBlank(configuredIcon)) {
            return emoji(configuredIcon);
        }
        return emoji(icon);
    }

    protected String emoji(final String configurationIcon) {
        return ":%s:".formatted(configurationIcon);
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

    protected String createNotEnabled(final boolean showDisabled) {
        if (showDisabled) {
            return "## :%s: %s%s %n%n".formatted(icon, type, ": not enabled");
        }
        return StringUtils.EMPTY;
    }
}
