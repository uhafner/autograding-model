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
        CoverageConfiguration configuration = score.getCoverageConfiguration();
        if (!configuration.isEnabled()) {
            return getNotEnabled();
        }
        if (score.getCoverageScores().isEmpty()) {
            return getNotFound();
        }

        StringBuilder comment = new StringBuilder();
        comment.append(getSummary(score.getCoverageAchieved(), configuration.getMaxScore()));
        comment.append(formatColumns("Name", "Covered %", "Missed %", "Impact"));
        comment.append(formatColumns(":-:", ":-:", ":-:", ":-:"));
        score.getCoverageScores().forEach(coverageScore -> comment.append(formatColumns(
                coverageScore.getName(),
                String.valueOf(coverageScore.getCoveredPercentage()),
                String.valueOf(coverageScore.getMissedPercentage()),
                String.valueOf(coverageScore.getTotalImpact()))));
        comment.append(formatBoldColumns(IMPACT,
                configuration.getCoveredPercentageImpact(),
                configuration.getMissedPercentageImpact(),
                N_A
        ));
        return comment.toString();
    }
}
