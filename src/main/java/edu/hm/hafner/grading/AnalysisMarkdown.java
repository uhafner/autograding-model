package edu.hm.hafner.grading;

import java.util.List;
import java.util.function.Function;

import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

/**
 * Renders the static analysis results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class AnalysisMarkdown extends ScoreMarkdown<AnalysisScore, AnalysisConfiguration> {
    static final String TYPE = "Static Analysis Warnings Score";

    /**
     * Creates a new Markdown renderer for static analysis results.
     */
    public AnalysisMarkdown() {
        super(TYPE, "warning");
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
                    .addText(formatColumns("Name", "Errors", "Warning High", "Warning Normal", "Warning Low", "Total"))
                    .addTextIf(formatColumns("Impact"), score.hasMaxScore())
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:"))
                    .addTextIf(formatColumns(":-:"), score.hasMaxScore())
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(subScore.getName(),
                            String.valueOf(subScore.getErrorSize()),
                            String.valueOf(subScore.getHighSeveritySize()),
                            String.valueOf(subScore.getNormalSeveritySize()),
                            String.valueOf(subScore.getLowSeveritySize()),
                            String.valueOf(subScore.getTotalSize())))
                    .addTextIf(formatColumns(String.valueOf(subScore.getImpact())), score.hasMaxScore())
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns("Total",
                                sum(aggregation, AnalysisScore::getErrorSize),
                                sum(aggregation, AnalysisScore::getHighSeveritySize),
                                sum(aggregation, AnalysisScore::getNormalSeveritySize),
                                sum(aggregation, AnalysisScore::getLowSeveritySize),
                                sum(aggregation, AnalysisScore::getTotalSize)))
                        .addTextIf(formatBoldColumns(sum(aggregation, AnalysisScore::getImpact)), score.hasMaxScore())
                        .addNewline();
            }

            if (score.hasMaxScore()) {
                var configuration = score.getConfiguration();
                details.addText(formatColumns(IMPACT))
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

    private int sum(final AggregatedScore score, final Function<AnalysisScore, Integer> property) {
        return score.getAnalysisScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }
}
