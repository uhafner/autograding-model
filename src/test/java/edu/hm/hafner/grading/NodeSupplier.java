package edu.hm.hafner.grading;

import java.util.function.Function;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.FilteredLog;

/**
 * A {@link ToolParser} that supplies {@link Node} instances from a predefined function reference.
 *
 * @author Ullrich Hafner
 */
class NodeSupplier implements ToolParser {
    private final Function<ToolConfiguration, Node> reference;

    NodeSupplier(final Function<ToolConfiguration, Node> reference) {
        this.reference = reference;
    }

    @Override
    public Report readReport(final ToolConfiguration tool, final FilteredLog log) {
        throw new UnsupportedOperationException("This parser does not support reading reports");
    }

    @Override
    public Node readNode(final ToolConfiguration configuration, final FilteredLog log) {
        return reference.apply(configuration);
    }
}