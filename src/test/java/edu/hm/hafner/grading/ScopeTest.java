package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the enum {@link Scope}.
 *
 * @author Jannik Ohme
 */
public class ScopeTest {
    @Test
    void shouldReturnAppropriateScope() {
        assertThat(Scope.fromString("project")).isEqualTo(Scope.PROJECT);
        assertThat(Scope.fromString("modified_files")).isEqualTo(Scope.MODIFIED_FILES);
        assertThat(Scope.fromString("modified_lines")).isEqualTo(Scope.MODIFIED_LINES);
    }
}
