package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.ReportFormatter;
import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

import java.util.List;
import java.util.function.Function;

/**
 * Renders the static analysis results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 * @author Jannik Ohme
 */
public class AnalysisMarkdown extends ScoreMarkdown<AnalysisScore, AnalysisConfiguration> {
    private static final ParserRegistry REGISTRY = new ParserRegistry();

    static final String TYPE = "Static Analysis Score";

    private static final ReportFormatter FORMATTER = new ReportFormatter();

    /**
     * Creates a new Markdown renderer for static analysis results.
     */
    public AnalysisMarkdown() {
        super(TYPE, emoji("warning"));
    }

    @Override
    protected List<AnalysisScore> createScores(final AggregatedScore aggregation) {
        return aggregation.getAnalysisScores();
    }

    @Override
    String createScoreSummary(final AnalysisScore score) {
        var report = score.getReport();

        var title = FORMATTER.formatSizeOfElements(report);
        var delta = score.hasDelta() ? " " + delta(score.getTotalSizeDelta(), true) : StringUtils.EMPTY;
        if (score.isEmpty()) {
            return title + delta;
        }
        return title + delta + " " + MDASH + " " + FORMATTER.formatSeverities(report);
    }

    @Override
    protected String createSpecificDetails(final List<AnalysisScore> scores) {
        var details = new TruncatedStringBuilder();
        for (AnalysisScore score : scores) {
            details.addText(getTitle(score, 2))
                    .addParagraph()
                    .addText(formatColumns("Icon", "Name", "Scope", "Warnings"))
                    .addTextIf(formatColumns("Impact"), score.hasMaxScore())
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:"))
                    .addTextIf(formatColumns(":-:"), score.hasMaxScore())
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(getIcon(subScore), subScore.getName(), subScore.getScope().getDisplayName(),
                            deltaCell(subScore.hasDelta(), subScore.getTotalSize(), subScore.getTotalSizeDelta(), false)))
                    .addTextIf(formatColumns(String.valueOf(subScore.getImpact())), score.hasMaxScore())
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns(":heavy_plus_sign:", "Total", EMPTY,
                                deltaCell(score.hasDelta(),
                                        sum(score, AnalysisScore::getTotalSize),
                                        sum(score, AnalysisScore::getTotalSizeDelta), false)))
                        .addTextIf(formatBoldColumns(sum(score, AnalysisScore::getImpact)), score.hasMaxScore())
                        .addNewline();
            }

            details.addNewline();
        }
        return details.build().buildByChars(MARKDOWN_MAX_SIZE);
    }

    private int sum(final AnalysisScore score, final Function<AnalysisScore, Integer> property) {
        return score.getSubScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }

    @Override
    protected String getToolIcon(final AnalysisScore score) {
        var parserId = score.getReport().getId();
        if (REGISTRY.contains(parserId)) {
            var descriptor = REGISTRY.get(parserId);
            if (!descriptor.getIconUrl().isBlank()) {
                return format("<img src=\"%s\" alt=\"%s\" height=\"%d\" width=\"%d\">",
                        descriptor.getIconUrl(), score.getName(), ICON_SIZE, ICON_SIZE);
            }
        }
        return getDefaultIcon(score);
    }
}
