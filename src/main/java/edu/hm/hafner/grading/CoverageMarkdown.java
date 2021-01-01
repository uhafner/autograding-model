package edu.hm.hafner.grading;

/**
 * Renders the code coverage results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class CoverageMarkdown extends ScoreMarkdown {
    static final String TYPE = "Code Coverage Score";
    
    /**
     * Creates a new Markdown renderer for code coverage results.
     */
    public CoverageMarkdown() {
        super(TYPE, "paw_prints");
    }

    /**
     * Renders the code coverage results in Markdown.
     *
     * @param score
     *         the aggregated score
     *
     * @return returns formatted string
     */
    public String create(final AggregatedScore score) {
        if (!score.getCoverageConfiguration().isEnabled()) {
            return getNotEnabled();
        }
        if (score.getCoverageScores().isEmpty()) {
            return getNotFound();
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getSummary(score.getCoverageAchieved(), score.getCoverageConfiguration().getMaxScore()));
        stringBuilder.append(formatColumns("Name", "Covered %", "Missed %", "Impact"));
        stringBuilder.append(formatColumns(":-:", ":-:", ":-:", ":-:"));
        score.getCoverageScores().forEach(coverageScore -> stringBuilder.append(formatColumns(
                coverageScore.getName(),
                String.valueOf(coverageScore.getCoveredPercentage()),
                String.valueOf(coverageScore.getMissedPercentage()),
                String.valueOf(coverageScore.getTotalImpact()))));
        return stringBuilder.toString();
    }
}
