package edu.hm.hafner.grading;

import java.util.List;

import edu.hm.hafner.coverage.Metric;

/**
 * Renders the mutation coverage results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
// FIXME: This class is not used and can be removed?
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
        return aggregation.getCoverageScores();
    }

    @Override
    protected String getToolIcon(final CoverageScore score) {
        if (PIT.equals(score.getConfiguration().getParserId()) && score.getMetric() == Metric.MUTATION) { // override icon for PIT
            return format("<img src=\"https://pitest.org/images/pit-black-150x152.png\" alt=\"PIT\" height=\"%d\" width=\"%d\">",
                    ICON_SIZE, ICON_SIZE);
        }
        else {
            return super.getToolIcon(score);
        }
    }
}
