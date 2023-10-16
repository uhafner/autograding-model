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
     * @param score
     *         the aggregated score
     *
     * @return returns formatted string
     */
    public String create(final AggregatedScore score) {
        var configuration = score.getAnalysisConfiguration();
        if (!configuration.isEnabled()) {
            return getNotEnabled();
        }
        if (score.getAnalysisScores().isEmpty()) {
            return getNotFound();
        }

        var comment = new StringBuilder(MESSAGE_INITIAL_CAPACITY);
        comment.append(getSummary(score.getAnalysisAchieved(), configuration.getMaxScore()));
        comment.append(formatColumns("Name", "Errors", "Warning High", "Warning Normal", "Warning Low", "Impact"));
        comment.append(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:"));
        comment.append(formatItalicColumns(IMPACT,
                renderImpact(configuration.getErrorImpact()),
                renderImpact(configuration.getHighImpact()),
                renderImpact(configuration.getNormalImpact()),
                renderImpact(configuration.getLowImpact()),
                LEDGER
        ));
        score.getAnalysisScores().forEach(analysisScore -> comment.append(formatColumns(
                analysisScore.getName(),
                String.valueOf(analysisScore.getErrorsSize()),
                String.valueOf(analysisScore.getHighSeveritySize()),
                String.valueOf(analysisScore.getNormalSeveritySize()),
                String.valueOf(analysisScore.getLowSeveritySize()),
                String.valueOf(analysisScore.getTotalImpact()))));
        if (score.getAnalysisScores().size() > 1) {
            comment.append(formatBoldColumns("Total",
                    sum(score, AnalysisScore::getErrorsSize),
                    sum(score, AnalysisScore::getHighSeveritySize),
                    sum(score, AnalysisScore::getNormalSeveritySize),
                    sum(score, AnalysisScore::getLowSeveritySize),
                    sum(score, AnalysisScore::getTotalImpact)));
        }

        return comment.toString();
    }

    private int sum(final AggregatedScore score, final Function<AnalysisScore, Integer> property) {
        return score.getAnalysisScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }
}
