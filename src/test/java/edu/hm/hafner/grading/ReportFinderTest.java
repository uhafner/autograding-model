package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;

class ReportFinderTest {
    private static final FilteredLog LOG = new FilteredLog("Errors");

    @Test
    void shouldFindTestReports() {
        var finder = new ReportFinder();

        assertThat(finder.find("glob:**/TEST*.xml", "src/test/resources/", LOG)).hasSize(3);
        assertThat(finder.find("glob:src/test/resources/**/*edu*.xml", "src/test/resources/", LOG)).hasSize(2);
        assertThat(finder.find("glob:src/**/*.html", "src/test/resources/", LOG)).isEmpty();

        assertThat(finder.find("glob:**/*.xml", "src/java/", LOG)).isEmpty();
    }

    @Test
    void shouldFindSources() {
        var finder = new ReportFinder();

        assertThat(finder.find("regex:.*FileSystem.*\\.java", "src/main/java/", LOG)).hasSize(3);
    }
}
