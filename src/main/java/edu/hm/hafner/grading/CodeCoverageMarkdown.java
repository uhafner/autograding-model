package edu.hm.hafner.grading;

import java.util.List;

/**
 * Renders the code coverage results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class CodeCoverageMarkdown extends CoverageMarkdown {
    static final String TYPE = "Code Coverage Score";

    /**
     * Creates a new Markdown renderer for code coverage results.
     */
    public CodeCoverageMarkdown() {
        super(TYPE, "footprints", "Covered %", "Missed %", "coverage achieved");
    }

    @Override
    protected List<CoverageScore> createScores(final AggregatedScore aggregation) {
        return aggregation.getCodeCoverageScores();
    }
}
