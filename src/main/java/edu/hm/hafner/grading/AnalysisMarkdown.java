package edu.hm.hafner.grading;

import java.util.List;
import java.util.function.Function;

import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

/**
 * Renders the static analysis results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class AnalysisMarkdown extends ScoreMarkdown<AnalysisScore, AnalysisConfiguration> {
    private static final ParserRegistry REGISTRY = new ParserRegistry();

    static final String TYPE = "Static Analysis Warnings Score";

    /**
     * Creates a new Markdown renderer for static analysis results.
     */
    public AnalysisMarkdown() {
        super(TYPE, "exclamation");
    }

    @Override
    protected List<AnalysisScore> createScores(final AggregatedScore aggregation) {
        return aggregation.getAnalysisScores();
    }

    @Override
    protected void createSpecificDetails(final AggregatedScore aggregation, final List<AnalysisScore> scores,
            final TruncatedStringBuilder details) {
        for (AnalysisScore score : scores) {
            details.addText(getTitle(score, 2))
                    .addNewline()
                    .addText(getPercentageImage(score))
                    .addNewline()
                    .addText(formatColumns("Name", "Reports", "Errors", "High", "Normal", "Low", "Total"))
                    .addTextIf(formatColumns("Impact"), score.hasMaxScore())
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:", ":-:"))
                    .addTextIf(formatColumns(":-:"), score.hasMaxScore())
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(subScore.getName(),
                            String.valueOf(subScore.getReportFiles()),
                            String.valueOf(subScore.getErrorSize()),
                            String.valueOf(subScore.getHighSeveritySize()),
                            String.valueOf(subScore.getNormalSeveritySize()),
                            String.valueOf(subScore.getLowSeveritySize()),
                            String.valueOf(subScore.getTotalSize())))
                    .addTextIf(formatColumns(String.valueOf(subScore.getImpact())), score.hasMaxScore())
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns("Total",
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
                details.addText(formatColumns(IMPACT, EMPTY))
                        .addText(formatItalicColumns(
                                renderImpact(configuration.getErrorImpact()),
                                renderImpact(configuration.getHighImpact()),
                                renderImpact(configuration.getNormalImpact()),
                                renderImpact(configuration.getLowImpact())))
                        .addText(formatColumns(TOTAL, LEDGER))
                        .addNewline();
            }
        }
    }

    private int sum(final AnalysisScore score, final Function<AnalysisScore, Integer> property) {
        return score.getSubScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }

    protected String extractSeverities(final AnalysisScore score) {
        if (score.getReport().isEmpty()) {
            return "No warnings found";
        }
        else {
            return String.format("%d warning%s found (%d error%s, %d high, %d normal, %d low)",
                    score.getTotalSize(), AnalysisScore.plural(score.getTotalSize()),
                    score.getErrorSize(), AnalysisScore.plural(score.getErrorSize()),
                    score.getHighSeveritySize(),
                    score.getNormalSeveritySize(),
                    score.getLowSeveritySize());
        }
    }

    @Override
    protected String createSummary(final AnalysisScore score) {
        var builder = new StringBuilder();
        for (AnalysisScore analysisScore : score.getSubScores()) {
            builder.append(SPACE)
                    .append(SPACE)
                    .append(getIconAndName(analysisScore))
                    .append(": ")
                    .append(extractSeverities(analysisScore))
                    .append(LINE_BREAK);
        }
        return builder.toString();
    }

    private String getIconAndName(final AnalysisScore analysisScore) {
        return " %s &nbsp; %s".formatted(extractParserIcon(analysisScore), analysisScore.getName())
                + createScoreTitle(analysisScore);
    }

    private String extractParserIcon(final AnalysisScore analysisScore) {
        var descriptor = REGISTRY.get(analysisScore.getId());
        if (descriptor.getIconUrl().isEmpty()) {
            return ":exclamation:";
        }
        else {
            return "<img src=\"%s\" alt=\"%s\" height=\"%d\" width=\"%d\">"
                        .formatted(descriptor.getIconUrl(), analysisScore.getName(), ICON_SIZE, ICON_SIZE);
        }
    }
}
