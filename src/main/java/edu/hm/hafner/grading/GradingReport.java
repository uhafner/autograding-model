package edu.hm.hafner.grading;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import edu.hm.hafner.analysis.Report;

/**
 * Creates a human-readable report of the grading results.
 *
 * @author Tobias Effner
 */
public class GradingReport {
    /**
     * Returns a short header for the grading results, this value typically will be used as link name.
     *
     * @return the header (plain ASCII text)
     */
    public String getHeader() {
        return "Autograding results";
    }

    /**
     * Returns a short summary for the grading results. This text does not use Markdown and fits into a
     * single line.
     *
     * @param score
     *         the aggregated score
     *
     * @return the summary (plain ASCII text)
     */
    public String getSummary(final AggregatedScore score) {
        var summary = new StringJoiner(", ", " (", ")");
        summary.setEmptyValue(StringUtils.EMPTY);

        if (score.hasTests()) {
            summary.add(String.format("unit tests: %d/%d", score.getTestAchievedScore(),
                    score.getTestMaxScore()));
        }
        if (score.hasCodeCoverage()) {
            summary.add(String.format("code coverage: %d/%d", score.getCodeCoverageAchievedScore(),
                    score.getCodeCoverageMaxScore()));
        }
        if (score.hasMutationCoverage()) {
            summary.add(String.format("mutation coverage: %d/%d", score.getMutationCoverageAchievedScore(),
                    score.getMutationCoverageMaxScore()));
        }
        if (score.hasAnalysis()) {
            summary.add(String.format("analysis: %d/%d", score.getAnalysisAchievedScore(),
                    score.getAnalysisMaxScore()));
        }
        return String.format(
                "Total score: %d/%d%s",
                score.getAchievedScore(), score.getMaxScore(),
                summary);
    }

    /**
     * Creates a detailed description of the grading results in Markdown.
     *
     * @param score
     *         the aggregated score
     * @param testReports
     *         JUnit reports that many contain details about failed tests
     * @return Markdown text
     */
    public String getDetails(final AggregatedScore score, final List<Report> testReports) {
        return String.format("# Total score: %s/%s%n",
                score.getAchievedScore(), score.getMaxScore())
                + new TestMarkdown().create(score)
                + new AnalysisMarkdown().create(score)
                + new CodeCoverageMarkdown().create(score)
                + new MutationCoverageMarkdown().create(score);
    }

    /**
     * Creates an error message in Markdown.
     *
     * @param score
     *         the aggregated score
     * @param exception
     *         the exception that caused the error
     * @return Markdown text
     */
    public String getErrors(final AggregatedScore score, final Throwable exception) {
        return String.format("# Partial score: %s/%s%n:construction: The grading has been aborted due to an error.:construction:%n",
                score.getAchievedScore(), score.getMaxScore())
                + createExceptionSection(exception)
                + createLogSection(score);

    }

    private String createExceptionSection(final Throwable exception) {
        return String.format("## Exception%n```%n%s%n```%n", ExceptionUtils.getStackTrace(exception));
    }

    private String createLogSection(final AggregatedScore score) {
        return String.format("## Error Messages%n```%n%s%n```%n## Information Messages%n```%n%s%n```%n",
                joinMessages(score.getErrorMessages()),
                joinMessages(score.getInfoMessages()));
    }

    private String joinMessages(final List<String> messages) {
        return messages.stream().collect(Collectors.joining(System.lineSeparator()));
    }
}
