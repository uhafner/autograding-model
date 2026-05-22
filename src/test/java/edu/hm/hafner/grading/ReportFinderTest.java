package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.PathUtil;

import static edu.hm.hafner.grading.ScoreBuilder.*;
import static org.assertj.core.api.Assertions.*;

class ReportFinderTest {
    private static final FilteredLog LOG = new FilteredLog("Errors");

    @Test
    void shouldFindTestReports() {
        var finder = new ReportFinder();

        assertThat(finder.findGlob("glob:**/grading/TEST*.xml", "src/test/resources/", NO_DELTA_REPORTS, LOG)).hasSize(3);
        assertThat(finder.findGlob("glob:src/test/resources/**/grading/*edu*.xml", "src/test/resources/", NO_DELTA_REPORTS, LOG)).hasSize(2);
        assertThat(finder.findGlob("glob:src/**/*.html", "src/test/resources/", NO_DELTA_REPORTS, LOG)).isEmpty();

        assertThat(finder.findGlob("glob:**/*.xml", "src/java/", NO_DELTA_REPORTS, LOG)).isEmpty();

        assertThat(finder.findGlob("glob:**/*.xml", "src/test/resources/edu/hm/hafner/grading/", NO_DELTA_REPORTS, LOG)).hasSize(19);
        assertThat(finder.findGlob("glob:**/*.xml", "src/test/resources/edu/hm/hafner/grading/delta", NO_DELTA_REPORTS, LOG)).hasSize(1);

        assertThat(finder.findGlob("glob:**/*.xml", "src/test/resources/edu/hm/hafner/grading/",
                StringUtils.EMPTY, LOG)).hasSize(19);
        assertThat(finder.findGlob("glob:**/*.xml", "src/test/resources/edu/hm/hafner/grading/",
                "src/test/resources/edu/hm/hafner/grading/delta", LOG)).hasSize(18);
        assertThat(finder.findGlob("glob:**/*.xml", "src/test/resources/edu/hm/hafner/grading/",
                "src/test/resources/", LOG)).isEmpty();
    }

    @Test
    void shouldHandleWrongPatterns() {
        var finder = new ReportFinder();

        assertThatThrownBy(() -> finder.findGlob("undefined:.*\\.xml", NO_DELTA_REPORTS, NO_DELTA_REPORTS, LOG))
                .hasMessageContaining("Syntax 'undefined' not recognized");
        assertThatThrownBy(() -> finder.findGlob("regex:[", NO_DELTA_REPORTS, NO_DELTA_REPORTS, LOG))
                .hasMessageContaining("Pattern not valid for FileSystem.getPathMatcher: regex:[");
    }

    @Test
    void shouldFindSources() {
        var finder = new ReportFinder();
        var pathUtil = new PathUtil();
        assertThat(finder.findGlob("regex:.*Markdown.*\\.java", "src/main/java/", NO_DELTA_REPORTS, LOG))
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
