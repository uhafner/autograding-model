package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link PathMapper}.
 */
class PathMapperTest {
    @Test
    void shouldMapUsingLongestSuffix() {
        var mapper = new PathMapper();
        var scm = List.of(
                "modules/service/src/main/java/com/example/Foo.java",
                "lib/core/src/main/kotlin/com/example/bar/Baz.kt",
                "no/match/Here.java"
        );
        var reports = List.of(
                "src/main/java/com/example/Foo.java",
                "src/main/kotlin/com/example/bar/Baz.kt",
                "src/main/java/other/Unrelated.java"
        );

        Map<String, String> mapping = mapper.mapScmToReportPaths(scm, reports);

        assertThat(mapping)
                .containsEntry("modules/service/src/main/java/com/example/Foo.java", "src/main/java/com/example/Foo.java")
                .containsEntry("lib/core/src/main/kotlin/com/example/bar/Baz.kt", "src/main/kotlin/com/example/bar/Baz.kt")
                .containsEntry("no/match/Here.java", "");
    }

    @Test
    void shouldFailOnAmbiguousReportMapping() {
        var mapper = new PathMapper();
        var scm = List.of(
                "root1/src/main/java/com/example/Duplicate.java",
                "root2/src/main/java/com/example/Duplicate.java"
        );
        // Both report paths end with the same filename; mapping both SCM paths to the same report path must fail
        var reports = List.of(
                "src/main/java/com/example/Duplicate.java"
        );

        assertThatThrownBy(() -> mapper.mapScmToReportPaths(scm, reports))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ambiguous fully qualified names");
    }
}


