package edu.hm.hafner.grading;

import java.util.function.Function;

/**
 * Renders the static analysis results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class AnalysisMarkdown extends ScoreMarkdown {
    static final String TYPE = "Static Analysis Warnings Score";

    /**
     * Creates a new Markdown renderer for static analysis results.
     */
    public AnalysisMarkdown() {
        super(TYPE, "warning");
    }

    /**
     * Renders the static analysis results in Markdown.
     *
     * @param aggregation
     *         the aggregation
     *
     * @return returns formatted string
     */
    public String create(final AggregatedScore aggregation) {
        var scores = aggregation.getAnalysisScores();
        if (scores.isEmpty()) {
            return getTitle(": not enabled");
        }

        var comment = new StringBuilder(MESSAGE_INITIAL_CAPACITY);

        for (AnalysisScore score : scores) {
            var configuration = score.getConfiguration();
            comment.append(getTitle(String.format(": %d of %d", score.getValue(), score.getMaxScore()), score.getName()));
            comment.append(formatColumns("Name", "Errors", "Warning High", "Warning Normal", "Warning Low", "Impact"));
            comment.append(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:"));
            score.getSubScores().forEach(subScore -> comment.append(formatColumns(
                    subScore.getName(),
                    String.valueOf(subScore.getErrorSize()),
                    String.valueOf(subScore.getHighSeveritySize()),
                    String.valueOf(subScore.getNormalSeveritySize()),
                    String.valueOf(subScore.getLowSeveritySize()),
                    String.valueOf(subScore.getImpact()))));
            if (score.getSubScores().size() > 1) {
                comment.append(formatBoldColumns("Total",
                        sum(aggregation, AnalysisScore::getErrorSize),
                        sum(aggregation, AnalysisScore::getHighSeveritySize),
                        sum(aggregation, AnalysisScore::getNormalSeveritySize),
                        sum(aggregation, AnalysisScore::getLowSeveritySize),
                        sum(aggregation, AnalysisScore::getImpact)));
            }
            comment.append(formatItalicColumns(IMPACT,
                    renderImpact(configuration.getErrorImpact()),
                    renderImpact(configuration.getHighImpact()),
                    renderImpact(configuration.getNormalImpact()),
                    renderImpact(configuration.getLowImpact()),
                    LEDGER));
        }

        return comment.toString();
    }

    private int sum(final AggregatedScore score, final Function<AnalysisScore, Integer> property) {
        return score.getAnalysisScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }
}
