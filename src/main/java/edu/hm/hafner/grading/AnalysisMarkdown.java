package edu.hm.hafner.grading;

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
     * @return returns formatted string
     */
    public String create(final AggregatedScore score) {
        AnalysisConfiguration configuration = score.getAnalysisConfiguration();
        if (!configuration.isEnabled()) {
            return getNotEnabled();
        }
        if (score.getAnalysisScores().isEmpty()) {
            return getNotFound();
        }

        StringBuilder comment = new StringBuilder();
        comment.append(getSummary(score.getAnalysisAchieved(), configuration.getMaxScore()));
        comment.append(formatColumns("Name", "Errors", "Warning High", "Warning Normal", "Warning Low", "Impact"));
        comment.append(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:"));
        score.getAnalysisScores().forEach(analysisScore -> comment.append(formatColumns(
                analysisScore.getName(),
                String.valueOf(analysisScore.getErrorsSize()),
                String.valueOf(analysisScore.getHighSeveritySize()),
                String.valueOf(analysisScore.getNormalSeveritySize()),
                String.valueOf(analysisScore.getLowSeveritySize()),
                String.valueOf(analysisScore.getTotalImpact()))));
        comment.append(formatBoldColumns(IMPACT,
                configuration.getErrorImpact(),
                configuration.getHighImpact(),
                configuration.getNormalImpact(),
                configuration.getLowImpact(),
                N_A
        ));

        return comment.toString();
    }
}
