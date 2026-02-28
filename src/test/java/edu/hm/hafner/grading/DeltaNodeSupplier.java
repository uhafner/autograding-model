package edu.hm.hafner.grading;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.FilteredLog;

import java.util.function.BiFunction;

/**
 * A {@link ToolParser} that supplies {@link Node} instances from a predefined function reference.
 * This parser also receives the directory as an argument, which allows it to read delta reports from a
 * different location than the main report.
 *
 * @author Ullrich Hafner
 */
class DeltaNodeSupplier implements ToolParser {
    private final BiFunction<ToolConfiguration, String, Node> reference;

    DeltaNodeSupplier(final BiFunction<ToolConfiguration, String, Node> reference) {
        this.reference = reference;
    }

    @Override
    public Report readReport(final ToolConfiguration tool, final String directory, final FilteredLog log) {
        throw new UnsupportedOperationException("This parser does not support reading reports");
    }

    @Override
    public Node readNode(final ToolConfiguration configuration, final String directory, final FilteredLog log) {
        return reference.apply(configuration, directory);
    }
}
