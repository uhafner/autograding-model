package edu.hm.hafner.grading;

import edu.hm.hafner.analysis.ParsingException;
import edu.hm.hafner.grading.QualityGateResult.OverallStatus;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * GitHub action entrypoint for the autograding action.
 *
 * @author Ullrich Hafner
 */
public class AutoGradingRunner {
    private static final String SINGLE_LINE = "--------------------------------------------------------------------------------";
    private static final String DOUBLE_LINE = "================================================================================";
    private static final int ERROR_CAPACITY = 1024;
    private static final String LOG_CONTAINS_ERRORS = "Autograding finished with some errors in the log, failing the action";
    private static final String QUALITY_GATES_FAILED = "Quality gates failed, failing the action";

    private final PrintStream outputStream;

    /**
     * Creates a new instance of {@link AutoGradingRunner}.
     *
     * @param outputStream
     *         the output stream to write the log to
     */
    public AutoGradingRunner(final PrintStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * Creates a new instance of {@link AutoGradingRunner}. This runner writes all logs to {@link System#out}.
     */
    @SuppressWarnings("SystemOut")
    public AutoGradingRunner() {
        this(System.out);
    }

    /**
     * Returns the name of the default configuration file to use when the environment variable CONFIG is not set.
     *
     * @return the name of the default configuration file
     */
    protected String getDefaultConfigurationPath() {
        return "/default-config.json";
    }

    protected String getDisplayName() {
        return "Autograding";
    }

    private String getFullDisplayName(final FilteredLog log) {
        return "%s %s (#%s)".formatted(getDisplayName(), readVersion(log), readSha(log));
    }

    /**
     * Runs the autograding.
     *
     * @return the grading score
     */
    public AggregatedScore run() {
        var log = new FilteredLog(getDisplayName() + " Errors:");

        var logHandler = new LogHandler(outputStream, log);

        log.logInfo(SINGLE_LINE);
        log.logInfo(center("Start", log));
        log.logInfo(SINGLE_LINE);

        var configuration = getConfiguration(log);
        var score = new AggregatedScore(log);
        logHandler.print();

        try {
            log.logInfo(DOUBLE_LINE);
            var parserFacade = new FileSystemToolParser(getModifiedLines(log));
            downloadArtefacts(log);
            score.gradeTests(parserFacade, TestConfiguration.from(configuration));
            logHandler.print();

            log.logInfo(DOUBLE_LINE);

            score.gradeCoverage(parserFacade, CoverageConfiguration.from(configuration));
            logHandler.print();

            log.logInfo(DOUBLE_LINE);

            score.gradeAnalysis(parserFacade, AnalysisConfiguration.from(configuration));
            logHandler.print();

            log.logInfo(DOUBLE_LINE);

            score.gradeMetrics(parserFacade, MetricConfiguration.from(configuration));
            logHandler.print();

            log.logInfo(DOUBLE_LINE);
            var results = new GradingReport();
            log.logInfo(results.getTextSummary(score));
            log.logInfo(DOUBLE_LINE);

            logHandler.print();

            log.logInfo(SINGLE_LINE);
            log.logInfo(center("Evaluate Quality Gates", log));
            log.logInfo(SINGLE_LINE);

            var qualityGates = readQualityGatesFromEnvVariable(log);
            var qualityGateResult = QualityGateResult.evaluate(score.getStatistics(), qualityGates, log);

            logHandler.print();

            log.logInfo(SINGLE_LINE);
            log.logInfo(center("Publish Results", log));
            log.logInfo(SINGLE_LINE);

            publishGradingResult(score, qualityGateResult, log);

            logHandler.print();

            if (failOnQualityGate()) {
                handleFailedQualityGates(qualityGateResult, log);
            }
        }
        catch (IllegalArgumentException | ParsingException | SecureXmlParserFactory.ParsingException exception) {
            log.logInfo(DOUBLE_LINE);
            log.logException(exception, "An error occurred while grading");
            log.logInfo(DOUBLE_LINE);

            publishError(score, log, exception);
        }
        finally {
            end(log, logHandler);
        }

        return score;
    }

    private List<QualityGate> readQualityGatesFromEnvVariable(final FilteredLog log) {
        String qualityGates = System.getenv("QUALITY_GATES");
        if (StringUtils.isBlank(qualityGates)) {
            log.logInfo("Environment variable '%s' not found or empty", "QUALITY_GATES");
            return List.of();
        }
        else {
            log.logInfo("Found quality gates configuration in environment variable '%s'", "QUALITY_GATES");
            return QualityGatesConfiguration.parseQualityGates(qualityGates, log);
        }
    }

    private void handleFailedQualityGates(final QualityGateResult qualityGateResult, final FilteredLog log) {
        if (qualityGateResult.getOverallStatus() != OverallStatus.SUCCESS) {
            log.logInfo(SINGLE_LINE);
            log.logInfo(QUALITY_GATES_FAILED);

            throw new IllegalStateException(QUALITY_GATES_FAILED);
        }
        if (log.hasErrors()) {
            log.logInfo(SINGLE_LINE);
            log.logInfo(LOG_CONTAINS_ERRORS);

            throw new IllegalStateException(LOG_CONTAINS_ERRORS);
        }
    }

    private void end(final FilteredLog log, final LogHandler logHandler) {
        logHandler.print();
        log.logInfo(SINGLE_LINE);
        log.logInfo(center("End", log));
        log.logInfo(SINGLE_LINE);
        logHandler.print();
    }

    /**
     * Reads the Maven version information from the git.properties file.
     *
     * @param log
     *         the logger
     *
     * @return the version information
     */
    protected String readVersion(final FilteredLog log) {
        return readGitProperty("git.build.version", log);
    }

    /**
     * Reads the Git SHA from the git.properties file.
     *
     * @param log
     *         the logger
     *
     * @return the Git SHA
     */
    protected String readSha(final FilteredLog log) {
        return readGitProperty("git.commit.id.abbrev", log);
    }

    @SuppressFBWarnings(value = "UI_INHERITANCE_UNSAFE_GETRESOURCE",
            justification = "This is required to get the correct file from the classpath")
    protected String readGitProperty(final String key, final FilteredLog log) {
        try (var propertiesFile = getClass().getResourceAsStream("/git.properties")) {
            if (propertiesFile == null) {
                log.logError("Version information file '/git.properties' not found in class path");

                return StringUtils.EMPTY;
            }

            try {
                var gitProperties = new Properties();

                gitProperties.load(propertiesFile);

                return gitProperties.getProperty(key);
            }
            catch (IOException exception) {
                log.logError("Can't read version information in '/git.properties'.");
            }
            return StringUtils.EMPTY;
        }
        catch (IOException exception) {
            return StringUtils.EMPTY; // ignore exception on close
        }
    }

    private String center(final String message, final FilteredLog log) {
        return StringUtils.center(message + " " + getFullDisplayName(log), 80);
    }

    /**
     * Publishes the grading result. This default implementation does nothing.
     *
     * @param score
     *         the grading score
     * @param qualityGateResult
     *         the result of the quality gate evaluation
     * @param log
     *         the logger
     */
    @SuppressWarnings("unused")
    protected void publishGradingResult(final AggregatedScore score, final QualityGateResult qualityGateResult,
            final FilteredLog log) {
        // empty default implementation
    }

    /**
     * Determines whether the grading process should fail if the quality gate is not successful. The default
     * implementation returns always {@code false}.
     *
     * @return {@code true} if the grading should fail on a failed quality gate, {@code false} otherwise
     */
    protected boolean failOnQualityGate() {
        return false;
    }

    /**
     * Publishes errors during grading. This default implementation does nothing.
     *
     * @param score
     *         the grading score
     * @param log
     *         the logger
     * @param exception
     *         the exception that occurred
     */
    @SuppressWarnings("unused")  // Subclasses may override this method and use the parameters
    protected void publishError(final AggregatedScore score, final FilteredLog log, final Throwable exception) {
        // empty default implementation
    }

    /**
     * Creates a text in Markdown format that contains all error messages.
     *
     * @param log
     *         the log to get the error messages from
     *
     * @return the error messages in Markdown format
     */
    protected String createErrorMessageMarkdown(final FilteredLog log) {
        if (log.hasErrors()) {
            var errors = new StringBuilder(ERROR_CAPACITY);

            errors.append("\n## :construction: &nbsp; Error Messages\n\n```\n");
            var messages = new StringJoiner("\n");
            log.getErrorMessages().forEach(messages::add);
            errors.append(messages);
            errors.append("\n```\n");

            return errors.toString();
        }
        return StringUtils.EMPTY;
    }

    @VisibleForTesting
    String getConfiguration(final FilteredLog log) {
        var configuration = System.getenv("CONFIG");
        if (StringUtils.isBlank(configuration)) {
            log.logInfo("No configuration provided (environment variable CONFIG not set), using default configuration");

            return readDefaultConfiguration();
        }

        log.logInfo("Obtaining configuration from environment variable CONFIG");
        return configuration;
    }

    private String readDefaultConfiguration() {
        var name = getDefaultConfigurationPath();
        try (var defaultConfig = AutoGradingRunner.class.getResourceAsStream(name)) {
            if (defaultConfig == null) {
                throw new IllegalStateException("Can't find configuration in class path: " + name);
            }
            return new String(defaultConfig.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException exception) {
            throw new IllegalStateException("Can't read default configuration: " + name, exception);
        }
    }

    /**
     * Returns the modified lines for the files under analysis. These lines are used to filter issues that are outside
     * the modified lines and to compute the coverage and analysis scores based on the modified lines only. The default
     * implementation returns an empty map.
     *
     * @param log
     *         the logger
     *
     * @return a map with file paths as keys and a set of modified line numbers as values
     */
    @SuppressWarnings("unused")
    protected Map<String, Set<Integer>> getModifiedLines(final FilteredLog log) {
        return Map.of();
    }

    @SuppressWarnings("unused")
    protected void downloadArtefacts(FilteredLog log) {

    }

}
