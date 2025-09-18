package edu.hm.hafner.grading;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.hm.hafner.util.FilteredLog;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

class LogHandlerTest {
    private static final String NOT_SHOWN = "Not shown";

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "Log some messages and evaluate quiet flag value (quiet = {0})")
    void shouldLogInfoAndErrorMessage(final boolean quiet) {
        var outputStream = new ByteArrayOutputStream();
        var printStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8);

        var logger = new FilteredLog("Title");
        logger.logInfo(NOT_SHOWN);
        logger.logError(NOT_SHOWN);

        var logHandler = new LogHandler(printStream, logger);
        logHandler.setQuiet(quiet);

        logger.logInfo("Info 1");
        logger.logInfo("Info 2");
        logger.logError("Error 1");

        logHandler.print();

        if (quiet) {
            assertThat(outputStream.toString(StandardCharsets.UTF_8)).isEmpty();
        }
        else {
            assertThat(outputStream.toString(StandardCharsets.UTF_8)).contains("Info 1", "Info 2", "Error 1").doesNotContain(NOT_SHOWN);
        }

        logger.logInfo("Info 3");
        logger.logInfo("Info 4");
        logger.logError("Error 2");

        logHandler.print();

        if (quiet) {
            assertThat(outputStream.toString(StandardCharsets.UTF_8)).isEmpty();
        }
        else {
            assertThat(outputStream.toString(StandardCharsets.UTF_8))
                    .containsOnlyOnce("Info 1")
                    .containsOnlyOnce("Info 2")
                    .containsOnlyOnce("Error 1")
                    .contains("Info 3", "Info 4", "Error 2");
        }
    }
}
