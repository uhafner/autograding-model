package edu.hm.hafner.grading;

import java.util.function.Predicate;

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
        super(TYPE, emoji("footprints"), "Covered %");
    }

    @Override
    protected Predicate<CoverageScore> filterScores() {
        return Predicate.not(this::containsMutationMetrics);
    }
}
