package edu.hm.hafner.grading;

/**
 * Renders the PIT results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class PitMarkdown extends ScoreMarkdown {
    static final String TYPE = "PIT Mutation Coverage Score";

    /**
     * Creates a new Markdown renderer for PIT mutation coverage results.
     */
    public PitMarkdown() {
        super(TYPE, "microbe");
    }

    /**
     * Renders the PIT mutation coverage results in Markdown.
     *
     * @param score
     *         the aggregated score
     *
     * @return returns formatted string
     */
    public String create(final AggregatedScore score) {
        PitConfiguration configuration = score.getPitConfiguration();
        if (!configuration.isEnabled()) {
            return getNotEnabled();
        }
        if (score.getPitScores().isEmpty()) {
            return getNotFound();
        }

        StringBuilder comment = new StringBuilder();
        if (score.hasTestFailures()) {
            comment.append(getSummary(0, configuration.getMaxScore()))
                    .append(":exclamation: PIT mutation coverage cannot be computed if there are test failures\n");
            return comment.toString();
        }

        comment.append(getSummary(score.getPitAchieved(), configuration.getMaxScore()));
        comment.append(formatColumns("Name", "Detected", "Undetected", "Detected %", "Undetected %", "Impact"));
        comment.append(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:"));
        comment.append(formatItalicColumns(
                IMPACT,
                renderImpact(configuration.getDetectedImpact()),
                renderImpact(configuration.getUndetectedImpact()),
                renderImpact(configuration.getDetectedPercentageImpact()),
                renderImpact(configuration.getUndetectedPercentageImpact()),
                createSign(configuration)));
        score.getPitScores().forEach(pitScore -> comment.append(formatColumns(
                "PIT",
                String.valueOf(pitScore.getDetectedSize()),
                String.valueOf(pitScore.getUndetectedSize()),
                String.valueOf(pitScore.getDetectedPercentage()),
                String.valueOf(pitScore.getUndetectedPercentage()),
                String.valueOf(pitScore.getTotalImpact()))));

        return comment.toString();
    }
}
