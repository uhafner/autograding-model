package edu.hm.hafner.grading;

/**
 * Renders the PIT results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class PitMarkdown extends ScoreMarkdown {
    static final String TYPE = "PIT Mutation Coverage";

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
        if (!score.getPitConfiguration().isEnabled()) {
            return getNotEnabled();
        }
        if (score.getPitScores().isEmpty()) {
            return getNotFound();
        }

        StringBuilder comment = new StringBuilder();
        if (score.hasTestFailures()) {
            comment.append(getSummary(0, score.getPitConfiguration().getMaxScore()))
                    .append(":exclamation: PIT mutation coverage cannot be computed if there are test failures\n");
            return comment.toString();
        }

        comment.append(getSummary(score.getPitAchieved(), score.getPitConfiguration().getMaxScore()));
        comment.append(formatColumns(new String[] {"Detected", "Undetected", "Detected %", "Undetected %", "Impact"}));
        comment.append(formatColumns(new String[] {":-:", ":-:", ":-:", ":-:", ":-:"}));
        score.getPitScores().forEach(pitScore -> comment.append(formatColumns(new String[] {
                String.valueOf(pitScore.getDetectedSize()),
                String.valueOf(pitScore.getUndetectedSize()),
                String.valueOf(pitScore.getDetectedPercentage()),
                String.valueOf(pitScore.getUndetectedPercentage()),
                String.valueOf(pitScore.getTotalImpact())})));
        return comment.toString();
    }

    private String formatColumns(final Object[] columns) {
        String format = "|%1$-10s|%2$-10s|%3$-10s|%4$-10s|%5$-10s|\n";
        return String.format(format, columns);
    }
}
