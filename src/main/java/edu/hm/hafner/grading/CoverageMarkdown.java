package edu.hm.hafner.grading;

import java.util.function.Function;

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
        if (score.getCoverageScores().size() > 1) {
            comment.append(formatBoldColumns("Total",
                    average(score, CoverageScore::getCoveredPercentage),
                    average(score, CoverageScore::getMissedPercentage),
                    sum(score, CoverageScore::getTotalImpact)));
        }
        comment.append(formatItalicColumns(N_A,
                renderImpact(configuration.getCoveredPercentageImpact()),
                renderImpact(configuration.getMissedPercentageImpact()),
                IMPACT
        ));
        return comment.toString();
    }

    private int sum(final AggregatedScore score, final Function<CoverageScore, Integer> property) {
        return score.getCoverageScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }

    private int average(final AggregatedScore score, final Function<CoverageScore, Integer> property) {
        return sum(score, property) / score.getCoverageScores().size();
    }
}
