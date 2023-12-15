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
                    .addText(formatColumns("Name", "Errors", "Warning High", "Warning Normal", "Warning Low",
                            "Total", "Impact"))
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:", ":-:"))
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(subScore.getName(),
                            String.valueOf(subScore.getErrorSize()),
                            String.valueOf(subScore.getHighSeveritySize()),
                            String.valueOf(subScore.getNormalSeveritySize()),
                            String.valueOf(subScore.getLowSeveritySize()),
                            String.valueOf(subScore.getTotalSize()),
                            String.valueOf(subScore.getImpact())))
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns("Total",
                                sum(aggregation, AnalysisScore::getErrorSize),
                                sum(aggregation, AnalysisScore::getHighSeveritySize),
                                sum(aggregation, AnalysisScore::getNormalSeveritySize),
                                sum(aggregation, AnalysisScore::getLowSeveritySize),
                                sum(aggregation, AnalysisScore::getTotalSize),
                                sum(aggregation, AnalysisScore::getImpact)))
                        .addNewline();
            }

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

    private int sum(final AggregatedScore score, final Function<AnalysisScore, Integer> property) {
        return score.getAnalysisScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }

    @Override
    protected void createSpecificSummary(final AnalysisScore score, final StringBuilder summary) {
        if (score.getReport().isEmpty()) {
            summary.append("No warnings found");
        }
        else {
            summary.append(String.format("%d warning%s found (%d error%s, %d high, %d normal, %d low)",
                    score.getTotalSize(), plural(score.getTotalSize()),
                    score.getErrorSize(), plural(score.getErrorSize()),
                    score.getHighSeveritySize(), score.getNormalSeveritySize(), score.getLowSeveritySize()));
        }
    }

    private String plural(final int score) {
        return score > 1 ? "s" : "";
    }
}
