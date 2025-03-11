package edu.hm.hafner.grading;

import java.util.function.Predicate;

/**
 * Renders the mutation coverage results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class MutationCoverageMarkdown extends CoverageMarkdown {
    private static final String PIT_ICON = format(
            "<img src=\"https://pitest.org/images/pit-black-150x152.png\" alt=\"PIT\" height=\"%d\" width=\"%d\">",
            ICON_SIZE, ICON_SIZE);

    static final String TYPE = "Mutation Coverage Score";

    /**
     * Creates a new Markdown renderer for mutation coverage results.
     */
    public MutationCoverageMarkdown() {
        super(TYPE, emoji("microscope"), "Killed %", "Survived %");
    }

    @Override
    protected Predicate<CoverageScore> filterScores() {
        return this::containsMutationMetrics;
    }

    @Override
    protected String getToolIcon(final CoverageScore score) {
        return switch (score.getMetric()) {
            case TEST_STRENGTH -> ":muscle:";
            case MUTATION -> PIT_ICON;
            default -> super.getToolIcon(score);
        };
    }
}
