package edu.hm.hafner.grading;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import edu.hm.hafner.grading.TestConfiguration.TestConfigurationBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static edu.hm.hafner.grading.assertions.Assertions.*;

/**
 * Tests the class {@link TestScore}.
 *
 * @author Eva-Maria Zeintl
 * @author Ullrich Hafner
 * @author Lukas Kirner
 */
class TestScoreTest {
    private static final String NAME = "Tests";
    private static final int MAX_SCORE = 25;

    @SuppressFBWarnings("UPM")
    private static Collection<Object[]> createTestConfigurationParameters() {
        return Arrays.asList(new Object[][] {
                {
                        createTestConfiguration(-1, -2, 1),
                        8, 1, 1,
                        3
                },
                {
                        createTestConfiguration(-1, -2, 1),
                        8, 5, 1,
                        -9
                },
                {
                        createTestConfiguration(-1, -2, -1),
                        8, 5, 1,
                        -13
                },
                {
                        createTestConfiguration(0, 0, 0),
                        0, 0, 0,
                        0
                },
                {
                        createTestConfiguration(99, 99, 99),
                        0, 0, 0,
                        0
                },
                {
                        createTestConfiguration(1, 1, 1),
                        3, 3, 0,
                        3
                },
        });
    }

    @ParameterizedTest
    @MethodSource("createTestConfigurationParameters")
    void shouldComputeTestScoreWith(final TestConfiguration configuration,
            final int totalSize, final int failedSize, final int skippedSize, final int expectedTotalImpact) {
        TestScore test = new TestScore.TestScoreBuilder().withDisplayName(NAME)
                .withConfiguration(configuration)
                .withTotalSize(totalSize)
                .withFailedSize(failedSize)
                .withSkippedSize(skippedSize)
                .build();

        assertThat(test).hasTotalSize(totalSize);
        assertThat(test).hasPassedSize(totalSize - failedSize - skippedSize);
        assertThat(test).hasFailedSize(failedSize);
        assertThat(test).hasSkippedSize(skippedSize);
        assertThat(test).hasId(TestScore.ID);
        assertThat(test).hasName(NAME);
        assertThat(test).hasTotalImpact(expectedTotalImpact);
    }

    private static TestConfiguration createTestConfiguration(
            final int skippedImpact, final int failureImpact, final int passedImpact) {
        return new TestConfigurationBuilder()
                .setMaxScore(MAX_SCORE)
                .setSkippedImpact(skippedImpact)
                .setFailureImpact(failureImpact)
                .setPassedImpact(passedImpact)
                .build();
    }

    @Test
    void shouldInitialiseWithDefaultValues() {
        TestConfiguration configuration = TestConfiguration.from("{}");

        assertThat(configuration).hasMaxScore(0);
        assertThat(configuration).hasFailureImpact(0);
        assertThat(configuration).hasPassedImpact(0);
        assertThat(configuration).hasSkippedImpact(0);
    }

    /**
     * Tests the Fluent Interface Pattern for null return by setter functions.
     */
    @Test
    void shouldThrowNullPointerExceptionIfSetSkippedImpactReturnsNull() {
        TestConfigurationBuilder configurationBuilder = new TestConfigurationBuilder()
                .setSkippedImpact(0)
                .setPassedImpact(0);
        assertThat(configurationBuilder).isNotNull();
    }

    @Test
    void shouldIgnoresAdditionalAttributes() {
        TestConfiguration configuration = TestConfiguration.from(
                "{\"additionalAttribute\":5}");

        assertThat(configuration)
                .hasMaxScore(0)
                .hasFailureImpact(0)
                .hasPassedImpact(0)
                .hasSkippedImpact(0)
                .isPositive();
    }

    @Test
    void shouldConvertFromJson() {
        TestConfiguration configuration = TestConfiguration.from(
                "{\"maxScore\":5,\"failureImpact\":1,\"passedImpact\":2,\"skippedImpact\":3}");

        assertThat(configuration)
                .hasMaxScore(5)
                .hasFailureImpact(1)
                .hasPassedImpact(2)
                .hasSkippedImpact(3)
                .isPositive()
                .hasToString("{"
                        + "\"enabled\":false,"
                        + "\"maxScore\":5,"
                        + "\"failureImpact\":1,"
                        + "\"passedImpact\":2,"
                        + "\"skippedImpact\":3,"
                        + "\"positive\":true}");
    }
}
