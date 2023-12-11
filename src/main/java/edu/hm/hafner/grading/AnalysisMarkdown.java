package edu.hm.hafner.grading;

import java.util.List;
import java.util.function.Function;

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
            final StringBuilder details) {
        for (AnalysisScore score : scores) {
            var configuration = score.getConfiguration();
            details.append(getTitle(score));
            details.append(formatColumns("Name", "Errors", "Warning High", "Warning Normal", "Warning Low", "Total", "Impact"));
            details.append(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:", ":-:"));
            score.getSubScores().forEach(subScore -> details.append(formatColumns(
                    subScore.getName(),
                    String.valueOf(subScore.getErrorSize()),
                    String.valueOf(subScore.getHighSeveritySize()),
                    String.valueOf(subScore.getNormalSeveritySize()),
                    String.valueOf(subScore.getLowSeveritySize()),
                    String.valueOf(subScore.getTotalSize()),
                    String.valueOf(subScore.getImpact()))));
            if (score.getSubScores().size() > 1) {
                details.append(formatBoldColumns("Total",
                        sum(aggregation, AnalysisScore::getErrorSize),
                        sum(aggregation, AnalysisScore::getHighSeveritySize),
                        sum(aggregation, AnalysisScore::getNormalSeveritySize),
                        sum(aggregation, AnalysisScore::getLowSeveritySize),
                        sum(aggregation, AnalysisScore::getTotalSize),
                        sum(aggregation, AnalysisScore::getImpact)));
            }
            details.append(formatItalicColumns(IMPACT,
                    renderImpact(configuration.getErrorImpact()),
                    renderImpact(configuration.getHighImpact()),
                    renderImpact(configuration.getNormalImpact()),
                    renderImpact(configuration.getLowImpact()),
                    TOTAL,
                    LEDGER));
        }
    }

    private int sum(final AggregatedScore score, final Function<AnalysisScore, Integer> property) {
        return score.getAnalysisScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }

    @Override
    protected void createSpecificSummary(final List<AnalysisScore> scores, final StringBuilder summary) {
        for (AnalysisScore score : scores) {
            summary.append(getTitle(score));
            if (score.getReport().isEmpty()) {
                summary.append("No warnings found");
            }
            else {
                summary.append(String.format("%d warning%s found (%d errors, %d high, %d normal, %d low)",
                        score.getTotalSize(), score.getTotalSize() > 1 ? "s" : "",
                        score.getErrorSize(),
                        score.getHighSeveritySize(), score.getNormalSeveritySize(), score.getLowSeveritySize()));
            }
            summary.append("\n");
        }
    }
}
