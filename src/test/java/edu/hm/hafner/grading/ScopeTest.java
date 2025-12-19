package edu.hm.hafner.grading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the enum {@link Scope}.
 *
 * @author Jannik Ohme
 */
class ScopeTest {
    @ParameterizedTest(name = "value={0}")
    @ValueSource(strings = {"project", "PROJECT", "all", "all", ""})
    @DisplayName("Convert String value to Project Scope")
    void shouldReturnProjectScope(final String value) {
        assertThat(Scope.fromString(value)).isEqualTo(Scope.PROJECT);
    }

    @ParameterizedTest(name = "value={0}")
    @ValueSource(strings = {"modified-files", "MODIFIED_FILES", "files", "Files"})
    @DisplayName("Convert String value to Modified Files Scope")
    void shouldReturnFilesScope(final String value) {
        assertThat(Scope.fromString(value)).isEqualTo(Scope.MODIFIED_FILES);
    }

    @ParameterizedTest(name = "value={0}")
    @ValueSource(strings = {"modified-lines", "MODIFIED_LINES", "lines", "Lines"})
    @DisplayName("Convert String value to Modified Lines Scope")
    void shouldReturnCodeScope(final String value) {
        assertThat(Scope.fromString(value)).isEqualTo(Scope.MODIFIED_LINES);
    }
}
