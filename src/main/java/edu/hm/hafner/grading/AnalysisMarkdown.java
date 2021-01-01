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
        if (!score.getAnalysisConfiguration().isEnabled()) {
            return getNotEnabled();
        }
        if (score.getAnalysisScores().isEmpty()) {
            return getNotFound();
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getSummary(score.getAnalysisAchieved(), score.getAnalysisConfiguration().getMaxScore()));
        stringBuilder.append(formatColumns("Name", "Errors", "Warning High", "Warning Normal", "Warning Low", "Impact"));
        stringBuilder.append(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:"));
        score.getAnalysisScores().forEach(analysisScore -> stringBuilder.append(formatColumns(
                analysisScore.getName(),
                String.valueOf(analysisScore.getErrorsSize()),
                String.valueOf(analysisScore.getHighSeveritySize()),
                String.valueOf(analysisScore.getNormalSeveritySize()),
                String.valueOf(analysisScore.getLowSeveritySize()),
                String.valueOf(analysisScore.getTotalImpact()))));
        stringBuilder.append(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:"));

        return stringBuilder.toString();
    }
}
