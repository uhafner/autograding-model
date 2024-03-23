package edu.hm.hafner.grading;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Mutation;
import edu.hm.hafner.util.LineRange;
import edu.hm.hafner.util.PathUtil;

/**
 * Creates comments for static analysis warnings, for lines with missing coverage, and for lines with survived
 * mutations.
 *
 * @author Ullrich Hafner
 */
public abstract class CommentBuilder {
    /**
     * Describes the type of the comment. Is the comment for a warning, a missed line, a partially covered line, or a
     * survived mutation?
     */
    public enum CommentType {
        WARNING,
        NO_COVERAGE,
        PARTIAL_COVERAGE,
        MUTATION_SURVIVED
    }

    private static final int NO_COLUMN = -1;
    private static final String NO_ADDITIONAL_DETAILS = StringUtils.EMPTY;
    private static final PathUtil PATH_UTIL = new PathUtil();

    private final List<String> prefixes;

    protected CommentBuilder(final String... prefixesToRemove) {
        prefixes = Arrays.asList(prefixesToRemove);
    }

    /**
     * Creates comments for static analysis warnings, for lines with missing coverage, and for lines with survived
     * mutations.
     *
     * @param score
     *         the score to create the comments for
     */
    public void createAnnotations(final AggregatedScore score) {
        var additionalAnalysisSourcePaths = extractAdditionalSourcePaths(score.getAnalysisScores());
        createAnnotationsForIssues(score, additionalAnalysisSourcePaths);

        var additionalCoverageSourcePaths = extractAdditionalSourcePaths(score.getCodeCoverageScores());
        createAnnotationsForMissedLines(score, additionalCoverageSourcePaths);
        createAnnotationsForPartiallyCoveredLines(score, additionalCoverageSourcePaths);

        var additionalMutationSourcePaths = extractAdditionalSourcePaths(score.getMutationCoverageScores());
        createAnnotationsForSurvivedMutations(score, additionalMutationSourcePaths);
    }

    /**
     * Creates a new comment.
     *
     * @param commentType
     *         the type of the comment
     * @param relativePath
     *         relative path of the file in the Git repository
     * @param lineStart
     *         start line of the comment
     * @param lineEnd
     *         end line of the comment
     * @param message
     *         plain text message of the comment
     * @param title
     *         plain text title of the comment
     * @param columnStart
     *         column of the comment (-1 if not applicable)
     * @param columnEnd
     *         column of the comment (-1 if not applicable)
     * @param details
     *         additional plain text details of the comment (empty if not applicable)
     * @param markDownDetails
     *         additional details of the comment in Markdown (empty if not applicable)
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    protected abstract void createComment(CommentType commentType, String relativePath,
            int lineStart, int lineEnd,
            String message, String title,
            int columnStart, int columnEnd,
            String details, String markDownDetails);

    private void createComment(final CommentType commentType, final String relativePath,
            final int lineStart, final int lineEnd,
            final String message, final String title) {
        createComment(commentType, relativePath, lineStart, lineEnd, message, title,
                NO_COLUMN, NO_COLUMN,
                NO_ADDITIONAL_DETAILS, NO_ADDITIONAL_DETAILS);
    }

    private Set<String> extractAdditionalSourcePaths(final List<? extends Score<?, ?>> scores) {
        return scores.stream()
                .map(Score::getConfiguration)
                .map(Configuration::getTools)
                .flatMap(Collection::stream)
                .map(ToolConfiguration::getSourcePath).collect(Collectors.toSet());
    }

    private void createAnnotationsForIssues(final AggregatedScore score,
            final Set<String> sourcePaths) {
        score.getIssues().forEach(issue -> createAnnotationForIssue(issue, sourcePaths));
    }

    private void createAnnotationForIssue(final Issue issue,
            final Set<String> sourcePaths) {
        var relativePath = cleanPath(createRelativeRepositoryPath(issue.getFileName(), sourcePaths));

        var text = getDescription(issue);

        createComment(CommentType.WARNING, relativePath, issue.getLineStart(), issue.getLineEnd(),
                issue.getMessage(), issue.getOriginName() + ": " + issue.getType(), issue.getColumnStart(),
                issue.getColumnEnd(), NO_ADDITIONAL_DETAILS, text);
    }

    private String getDescription(final Issue issue) {
        var parserRegistry = new ParserRegistry();
        if (parserRegistry.contains(issue.getOrigin())) {
            return parserRegistry.get(issue.getOrigin()).getDescription(issue);
        }
        return issue.getDescription();
    }

    private String cleanPath(final String path) {
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                return StringUtils.removeStart(path, prefix);
            }
        }
        return path;
    }

    private void createAnnotationsForMissedLines(final AggregatedScore score, final Set<String> sourcePaths) {
        score.getCoveredFiles(Metric.LINE).forEach(file -> createAnnotationsForMissedLines(file, sourcePaths));
    }

    private void createAnnotationsForMissedLines(final FileNode file, final Set<String> sourcePaths) {
        file.getMissedLineRanges()
                .forEach(range -> createAnnotationForMissedLineRange(file, range, sourcePaths));
    }

    private void createAnnotationForMissedLineRange(final FileNode file, final LineRange range,
            final Set<String> sourcePaths) {
        var relativePath = createRelativeRepositoryPath(file.getRelativePath(), sourcePaths);

        createComment(CommentType.NO_COVERAGE,
                relativePath, range.getStart(),
                range.getEnd(), getMissedLinesDescription(range),
                getMissedLinesMessage(range));
    }

    private String getMissedLinesMessage(final LineRange range) {
        if (range.getStart() == range.getEnd()) {
            return "Not covered line";
        }
        return "Not covered lines";
    }

    private String getMissedLinesDescription(final LineRange range) {
        if (range.getStart() == range.getEnd()) {
            return String.format("Line %d is not covered by tests", range.getStart());
        }
        return String.format("Lines %d-%d are not covered by tests", range.getStart(), range.getEnd());
    }

    private void createAnnotationsForPartiallyCoveredLines(final AggregatedScore score,
            final Set<String> sourcePaths) {
        score.getCoveredFiles(Metric.BRANCH)
                .forEach(file -> createAnnotationsForMissedBranches(file, sourcePaths));
    }

    private void createAnnotationsForMissedBranches(final FileNode file,
            final Set<String> sourcePaths) {
        file.getPartiallyCoveredLines().entrySet()
                .forEach(entry -> createAnnotationForMissedBranches(file, entry, sourcePaths));
    }

    private void createAnnotationForMissedBranches(final FileNode file,
            final Entry<Integer, Integer> branchCoverage,
            final Set<String> sourcePaths) {
        createComment(CommentType.PARTIAL_COVERAGE,
                createRelativeRepositoryPath(file.getRelativePath(), sourcePaths), branchCoverage.getKey(),
                branchCoverage.getKey(), createBranchMessage(branchCoverage.getKey(), branchCoverage.getValue()),
                "Partially covered line");
    }

    private String createBranchMessage(final int line, final int missed) {
        if (missed == 1) {
            return String.format("Line %d is only partially covered, one branch is missing", line);
        }
        return String.format("Line %d is only partially covered, %d branches are missing", line, missed);
    }

    private String createRelativeRepositoryPath(final String fileName, final Set<String> sourcePaths) {
        var cleaned = cleanPath(fileName);
        if (Files.exists(Path.of(cleaned))) {
            return cleaned;
        }
        for (String s : sourcePaths) {
            var added = PATH_UTIL.createAbsolutePath(s, cleaned);
            if (Files.exists(Path.of(added))) {
                return added;
            }
        }
        return cleaned;
    }

    private void createAnnotationsForSurvivedMutations(final AggregatedScore score,
            final Set<String> sourcePaths) {
        score.getCoveredFiles(Metric.MUTATION)
                .forEach(file -> createAnnotationsForSurvivedMutations(file, sourcePaths));
    }

    private void createAnnotationsForSurvivedMutations(final FileNode file,
            final Set<String> sourcePaths) {
        file.getSurvivedMutationsPerLine().entrySet()
                .forEach(entry -> createAnnotationForSurvivedMutation(file, entry, sourcePaths));
    }

    private void createAnnotationForSurvivedMutation(final FileNode file,
            final Entry<Integer, List<Mutation>> mutationsPerLine,
            final Set<String> sourcePaths) {
        var mutationDetails = createMutationDetails(mutationsPerLine.getValue());
        createComment(CommentType.MUTATION_SURVIVED,
                createRelativeRepositoryPath(file.getRelativePath(), sourcePaths), mutationsPerLine.getKey(),
                mutationsPerLine.getKey(),
                createMutationMessage(mutationsPerLine.getKey(), mutationsPerLine.getValue()),
                "Mutation survived", NO_COLUMN, NO_COLUMN,
                mutationDetails, mutationDetails);
    }

    private String createMutationMessage(final int line, final List<Mutation> survived) {
        if (survived.size() == 1) {
            return String.format("One mutation survived in line %d (%s)", line, formatMutator(survived));
        }
        return String.format("%d mutations survived in line %d", survived.size(), line);
    }

    private String formatMutator(final List<Mutation> survived) {
        return survived.get(0).getMutator().replaceAll(".*\\.", "");
    }

    private String createMutationDetails(final List<Mutation> mutations) {
        return mutations.stream()
                .map(mutation -> String.format("- %s (%s)", mutation.getDescription(), mutation.getMutator()))
                .collect(Collectors.joining("\n", "Survived mutations:\n", ""));
    }
}
