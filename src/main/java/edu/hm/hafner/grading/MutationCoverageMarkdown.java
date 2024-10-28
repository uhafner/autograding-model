package edu.hm.hafner.grading;

import java.util.List;

import edu.hm.hafner.coverage.Metric;

/**
 * Renders the mutation coverage results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class MutationCoverageMarkdown extends CoverageMarkdown {
    private static final String PIT = "pit";
    static final String TYPE = "Mutation Coverage Score";

    /**
     * Creates a new Markdown renderer for code coverage results.
     */
    public MutationCoverageMarkdown() {
        super(TYPE, "microscope", "Killed %", "Survived %");
    }

    @Override
    protected List<CoverageScore> createScores(final AggregatedScore aggregation) {
        return aggregation.getMutationCoverageScores();
    }

    @Override
    protected String getIcon(final CoverageScore score) {
        if (PIT.equals(score.getId()) && score.getMetric() == Metric.MUTATION) { // override icon for PIT
            return format("<img src=\"https://pitest.org/images/pit-black-150x152.png\" alt=\"PIT\" height=\"%d\" width=\"%d\">",
                    ICON_SIZE, ICON_SIZE);
        }
        else {
            return super.getIcon(score);
        }
    }
}
