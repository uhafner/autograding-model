package edu.hm.hafner.grading;

import com.google.errorprone.annotations.FormatMethod;
import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
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
 * @author Jannik Ohme
 */
abstract class ScoreMarkdown<S extends Score<S, C>, C extends Configuration> {
    static final int ICON_SIZE = 18;
    static final String SPACE = "&nbsp;";
    static final String LINE_BREAK_PARAGRAPH = "\\\n";
    static final String LINE_BREAK = "\n";
    static final String HORIZONTAL_RULE = "<hr />\n\n";
    static final String PARAGRAPH = "\n\n";
    static final String CHECK = ":white_check_mark:";
    static final String CROSS = ":x:";
    static final String EMPTY = "-";

    static final String N_A = "-";

    static final int MARKDOWN_MAX_SIZE = 10_000; // limit the size of the output to this number of characters
    private static final int HUNDRED_PERCENT = 100;
    private static final String OPEN_MOJI = "openmoji:";

    private final String type;
    private final String icon;

    ScoreMarkdown(final String type, final String icon) {
        this.type = type;
        this.icon = icon;
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
        return createSpecificDetails(scores);
    }

    /**
     * Renders the score details of the specific scores in Markdown. Since the Markdown size is limited on some backend
     * reporters, use a {@link TruncatedStringBuilder} to create the Markdown result.
     *
     * @param scores
     *         the scores to render the details for
     *
     * @return the specific details
     */
    protected abstract String createSpecificDetails(List<S> scores);

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
     *         determines whether headers should be shown for the subsections or not
     *
     * @return returns the summary in Markdown
     */
    public String createSummary(final AggregatedScore aggregation, final boolean showHeaders) {
        var summaries = new ArrayList<String>();
        for (S score : createScores(aggregation)) {
            var builder = new StringBuilder();
            if (showHeaders) {
                builder.append(getTextTitle(score, 3)).append(PARAGRAPH);
            }
            var subScores = createSummaryOfSubScores(score);
            builder.append(String.join(LINE_BREAK_PARAGRAPH, subScores));
            summaries.add(builder.toString());
        }
        if (showHeaders) {
            return String.join(PARAGRAPH, summaries);
        }
        return String.join(LINE_BREAK_PARAGRAPH, summaries);
    }

    private List<String> createSummaryOfSubScores(final S score) {
        return score.getSubScores().stream()
                .map(s -> SPACE + SPACE + getScopeTitle(s, 0) + ": " + createScoreSummary(s)).toList();
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

    protected String getTextTitle(final S score, final int size) {
        return "#".repeat(size) + " "
                + score.getName()
                + createScoreTitle(score);
    }

    protected String getTitle(final S score, final int size) {
        return "#".repeat(size)
                + " %s &nbsp; %s".formatted(getIcon(score), score.getName())
                + createScoreTitle(score);
    }

    protected String getScopeTitle(final S score, final int size) {
        return "#".repeat(size)
                + " %s &nbsp; %s (%s)".formatted(getIcon(score), score.getName(), score.getScope().getDisplayName())
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
        var scoreIcon = score.getIcon();
        if (StringUtils.isNotBlank(scoreIcon)) {
            return resolveEmoji(score, scoreIcon);
        }

        return getToolIcon(score);
    }

    private String resolveEmoji(final S score, final String scoreIcon) {
        if (scoreIcon.startsWith(OPEN_MOJI)) {
            return openmoji(scoreIcon, score.getName());
        }
        return emoji(scoreIcon);
    }

    protected abstract String getToolIcon(S score);

    protected String getDefaultIcon(final S score) {
        var configuredIcon = score.getConfiguration().getIcon();
        if (StringUtils.isNotBlank(configuredIcon)) {
            return resolveEmoji(score, configuredIcon);
        }
        return icon;
    }

    protected static String formatDelta(final int score, final int delta) {
        return delta == 0 ? String.valueOf(score) : score + " (" + formatDelta(delta) + ")";
    }

    private static String formatDelta(final int score) {
        return (score == 0 ? "±" : score > 0 ? "+" : "") + score;
    }

    protected static String emoji(final String configurationIcon) {
        return ":%s:".formatted(configurationIcon);
    }

    protected static String openmoji(final String configurationIcon, final String label) {
        var icon = Strings.CS.removeStart(configurationIcon, OPEN_MOJI);
        return ("<img src=\"https://openmoji.org/data/color/svg/"
                + "%s.svg\" alt=\"%s\" height=\"18\" width=\"18\">").formatted(icon, label);
    }

    String formatColumns(final Object... columns) {
        return format(i -> i, columns);
    }

    @Deprecated(since = "10.1.0", forRemoval = true)
    String formatItalicColumns(final Object... columns) {
        return format(s -> "*" + s + "*", columns);
    }

    String formatBoldColumns(final Object... columns) {
        return format(s -> "**" + s + "**", columns);
    }


    /**
     * Returns a formatted string using the specified format string and arguments. The English locale is always used to
     * format the string.
     *
     * @param format
     *         A <a href="../util/Formatter.html#syntax">format string</a>
     * @param args
     *         Arguments referenced by the format specifiers in the format string.  If there are more arguments than
     *         format specifiers, the extra arguments are ignored.  The number of arguments is variable and may be zero.
     *         The maximum number of arguments is limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *         The behaviour on a {@code null} argument depends on the <a
     *         href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @return A formatted string
     * @throws java.util.IllegalFormatException
     *         If a format string contains an illegal syntax, a format specifier that is incompatible with the given
     *         arguments, insufficient arguments given the format string, or other illegal conditions.  For
     *         specification of all possible formatting errors, see the <a
     *         href="../util/Formatter.html#detail">Details</a> section of the formatter class specification.
     * @see java.util.Formatter
     * @since 1.5
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

    @Deprecated(since = "10.1.0", forRemoval = true)
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
            return "## %s %s%s %n%n".formatted(icon, type, ": not enabled");
        }
        return StringUtils.EMPTY;
    }
}
