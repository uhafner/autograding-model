package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.PackageNode;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link PatchCoverage}.
 */
class PatchCoverageTest {
    @Test
    void shouldReturnMinusOneIfNoModifiedLines() {
        var root = new ModuleNode("root");
        var pkg = new PackageNode("com.example");
        var file = new FileNode("Foo.java", "src/main/java/com/example/Foo.java");
        // Provide some counters, but do not mark any modified lines
        file.addCounters(10, 1, 0);
        file.addCounters(20, 0, 1);
        pkg.addChild(file);
        root.addChild(pkg);

        var patch = new PatchCoverage();

        assertThat(patch.computePatchLinePercentage(root)).isEqualTo(-1);
    }

    @Test
    void shouldMarkModifiedLinesAndComputePercentage() {
        var root = new ModuleNode("root");
        var pkg = new PackageNode("com.example");
        var file = new FileNode("Foo.java", "src/main/java/com/example/Foo.java");
        // One covered line and one missed line → 50%
        file.addCounters(10, 1, 0); // covered
        file.addCounters(20, 0, 1); // missed
        pkg.addChild(file);
        root.addChild(pkg);

        var patch = new PatchCoverage();

        var scmPath = "workspace/project/app/src/main/java/com/example/Foo.java";
        // Include also non-positive lines which should be ignored
        patch.markModifiedLines(root, Map.of(scmPath, Set.of(10, 20, 0, -5)));

        assertThat(patch.computePatchLinePercentage(root)).isEqualTo(50);
    }

    @Test
    void shouldIgnoreNonMatchingScmPaths() {
        var root = new ModuleNode("root");
        var pkg = new PackageNode("com.example");
        var file = new FileNode("Foo.java", "src/main/java/com/example/Foo.java");
        file.addCounters(10, 1, 0);
        file.addCounters(20, 0, 1);
        pkg.addChild(file);
        root.addChild(pkg);

        var patch = new PatchCoverage();

        // SCM path does not end with the file's relative report path → no marking
        patch.markModifiedLines(root, Map.of("some/other/path/Foo.java", Set.of(10, 20)));

        assertThat(patch.computePatchLinePercentage(root)).isEqualTo(-1);
    }
}


