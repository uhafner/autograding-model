package edu.hm.hafner.grading;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;

/**
 * Computes patch coverage by marking modified lines on a coverage tree and deriving metrics
 * from the modified-lines filter view.
 */
final class PatchCoverage {
    /**
     * Marks the modified lines on the provided coverage root.
     *
     * @param root            coverage tree root
     * @param scmPathToLines  SCM path → changed lines
     */
    void markModifiedLines(final Node root, final Map<String, Set<Integer>> scmPathToLines) {
        if (scmPathToLines.isEmpty()) {
            return;
        }

        // Create mapping reportPath -> FileNode
        Map<String, FileNode> filesByPath = new HashMap<>();
        for (FileNode file : root.getAllFileNodes()) {
            filesByPath.put(file.getRelativePath(), file);
        }

        var mapper = new PathMapper();
        var mapping = mapper.mapScmToReportPaths(scmPathToLines.keySet(), filesByPath.keySet());

        for (Map.Entry<String, Set<Integer>> entry : scmPathToLines.entrySet()) {
            String scmPath = entry.getKey();
            String reportPath = mapping.getOrDefault(scmPath, "");
            if (!reportPath.isEmpty() && filesByPath.containsKey(reportPath)) {
                var node = filesByPath.get(reportPath);
                for (int line : entry.getValue()) {
                    if (line > 0) {
                        node.addModifiedLines(line);
                    }
                }
            }
        }
    }

    /**
     * Computes the line coverage percentage for modified lines only.
     * Returns -1 if there are no modified executable lines.
     */
    int computePatchLinePercentage(final Node root) {
        if (!root.hasModifiedLines()) {
            return -1; // signal N/A
        }
        var filtered = root.filterByModifiedLines();
        var value = filtered.getValue(Metric.LINE);
        if (value.isPresent() && value.get() instanceof Coverage coverage && coverage.isSet()) {
            return coverage.getCoveredPercentage().toInt();
        }
        return -1;
    }
}




