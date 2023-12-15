package edu.hm.hafner.grading;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.ParsingException;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.VisibleForTesting;

/**
 * GitHub action entrypoint for the autograding action.
 *
 * @author Ullrich Hafner
 */
public class AutoGradingRunner {
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
     * Runs the autograding.
     *
     * @return the grading score
     */
    public AggregatedScore run() {
        var log = new FilteredLog("Errors:");

        var logHandler = new LogHandler(outputStream, log);

        log.logInfo("------------------------------------------------------------------");
        log.logInfo("------------------------ Start Grading ---------------------------");
        log.logInfo("------------------------------------------------------------------");

        var configuration = getConfiguration(log);
        var score = new AggregatedScore(configuration, log);
        logHandler.print();

        try {
            log.logInfo("==================================================================");
            score.gradeTests(new FileSystemTestReportFactory());
            logHandler.print();

            log.logInfo("==================================================================");

            score.gradeCoverage(new FileSystemCoverageReportFactory());
            logHandler.print();

            log.logInfo("==================================================================");

            score.gradeAnalysis(new FileSystemAnalysisReportFactory());
            logHandler.print();

            log.logInfo("==================================================================");
            var results = new GradingReport();
            log.logInfo(results.getTextSummary(score));
            log.logInfo("==================================================================");

            logHandler.print();

            publishGradingResult(score, log);
        }
        catch (NoSuchElementException
               | ParsingException
               | SecureXmlParserFactory.ParsingException exception) {
            log.logInfo("==================================================================");
            log.logException(exception, "An error occurred while grading");
            log.logInfo("==================================================================");

            publishError(score, log, exception);
        }

        log.logInfo("------------------------------------------------------------------");
        log.logInfo("------------------------- End Grading ----------------------------");
        log.logInfo("------------------------------------------------------------------");
        logHandler.print();

        return score;
    }

    /**
     * Publishes the grading result. This default implementation does nothing.
     *
     * @param score
     *         the grading score
     * @param log
     *         the logger
     */
    protected void publishGradingResult(final AggregatedScore score, final FilteredLog log) {
        // empty default implementation
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
            var errors = new StringBuilder();

            errors.append("## :construction: Error Messages\n```\n");
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
        String configuration = System.getenv("CONFIG");
        if (StringUtils.isBlank(configuration)) {
            log.logInfo("No configuration provided (environment variable CONFIG not set), using default configuration");

            return readDefaultConfiguration();
        }

        log.logInfo("Obtaining configuration from environment variable CONFIG");
        return configuration;
    }

    private String readDefaultConfiguration() {
        try (var defaultConfig = AutoGradingRunner.class.getResourceAsStream("/default-config.json")) {
            if (defaultConfig == null) {
                throw new IOException("Can't find configuration in class path: default-conf.json");
            }
            return new String(defaultConfig.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException exception) {
            throw new IllegalStateException("Can't read default configuration 'default-conf.json'", exception);
        }
    }
}
