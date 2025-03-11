package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.PathUtil;

import static org.assertj.core.api.Assertions.*;

class ReportFinderTest {
    private static final FilteredLog LOG = new FilteredLog("Errors");

    @Test
    void shouldFindTestReports() {
        var finder = new ReportFinder();

        assertThat(finder.findGlob("glob:**/grading/TEST*.xml", "src/test/resources/", LOG)).hasSize(3);
        assertThat(finder.findGlob("glob:src/test/resources/**/grading/*edu*.xml", "src/test/resources/", LOG)).hasSize(2);
        assertThat(finder.findGlob("glob:src/**/*.html", "src/test/resources/", LOG)).isEmpty();

        assertThat(finder.findGlob("glob:**/*.xml", "src/java/", LOG)).isEmpty();
    }

    @Test
    void shouldFindSources() {
        var finder = new ReportFinder();
        var pathUtil = new PathUtil();
        assertThat(finder.findGlob("regex:.*Markdown.*\\.java", "src/main/java/", LOG))
                .map(pathUtil::getRelativePath)
                .containsExactlyInAnyOrder("src/main/java/edu/hm/hafner/grading/CoverageMarkdown.java",
                        "src/main/java/edu/hm/hafner/grading/CodeCoverageMarkdown.java",
                        "src/main/java/edu/hm/hafner/grading/AnalysisMarkdown.java",
                        "src/main/java/edu/hm/hafner/grading/MutationCoverageMarkdown.java",
                        "src/main/java/edu/hm/hafner/grading/TestMarkdown.java",
                        "src/main/java/edu/hm/hafner/grading/MetricMarkdown.java",
                        "src/main/java/edu/hm/hafner/grading/ScoreMarkdown.java");
    }
}
