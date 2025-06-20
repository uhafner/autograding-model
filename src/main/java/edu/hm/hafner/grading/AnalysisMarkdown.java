package edu.hm.hafner.grading;

import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

import java.util.List;
import java.util.function.Function;

/**
 * Renders the static analysis results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class AnalysisMarkdown extends ScoreMarkdown<AnalysisScore, AnalysisConfiguration> {
    private static final ParserRegistry REGISTRY = new ParserRegistry();

    static final String TYPE = "Static Analysis Score";

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
    protected String createSpecificDetails(final List<AnalysisScore> scores) {
        var details = new TruncatedStringBuilder();
        for (AnalysisScore score : scores) {
            details.addText(getTitle(score, 2))
                    .addParagraph()
                    .addText(getPercentageImage(score))
                    .addNewline()
                    .addText(formatColumns("Icon", "Name", "Reports", "Errors", "High", "Normal", "Low", "Total"))
                    .addTextIf(formatColumns("Impact"), score.hasMaxScore())
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:", ":-:", ":-:"))
                    .addTextIf(formatColumns(":-:"), score.hasMaxScore())
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(getIcon(subScore), subScore.getName(),
                            String.valueOf(subScore.getReportFiles()),
                            String.valueOf(subScore.getErrorSize()),
                            String.valueOf(subScore.getHighSeveritySize()),
                            String.valueOf(subScore.getNormalSeveritySize()),
                            String.valueOf(subScore.getLowSeveritySize()),
                            String.valueOf(subScore.getTotalSize())))
                    .addTextIf(formatColumns(String.valueOf(subScore.getImpact())), score.hasMaxScore())
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns(":heavy_plus_sign:", "Total",
                                sum(score, AnalysisScore::getReportFiles),
                                sum(score, AnalysisScore::getErrorSize),
                                sum(score, AnalysisScore::getHighSeveritySize),
                                sum(score, AnalysisScore::getNormalSeveritySize),
                                sum(score, AnalysisScore::getLowSeveritySize),
                                sum(score, AnalysisScore::getTotalSize)))
                        .addTextIf(formatBoldColumns(sum(score, AnalysisScore::getImpact)), score.hasMaxScore())
                        .addNewline();
            }

            if (score.hasMaxScore()) {
                var configuration = score.getConfiguration();
                details.addText(formatColumns(IMPACT, EMPTY, EMPTY))
                        .addText(formatItalicColumns(
                                renderImpact(configuration.getErrorImpact()),
                                renderImpact(configuration.getHighImpact()),
                                renderImpact(configuration.getNormalImpact()),
                                renderImpact(configuration.getLowImpact())))
                        .addText(formatColumns(TOTAL, LEDGER))
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
