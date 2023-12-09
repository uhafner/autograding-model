package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ReportFinderTest {
    @Test
    void shouldFindTestReports() {
        var finder = new ReportFinder();

        assertThat(finder.find("glob:**/TEST*.xml", "src/test/resources/")).hasSize(3);
        assertThat(finder.find("glob:src/test/resources/**/*edu*.xml", "src/test/resources/")).hasSize(2);
        assertThat(finder.find("glob:src/**/*.html", "src/test/resources/")).isEmpty();

        assertThat(finder.find("glob:**/*.xml", "src/java/")).isEmpty();
    }

    @Test
    void shouldFindSources() {
        var finder = new ReportFinder();

        assertThat(finder.find("regex:.*FileSystem.*\\.java", "src/main/java/")).hasSize(3);
    }
}
