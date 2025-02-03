package io.jenkins.plugins.coverage.metrics.steps;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;

import hudson.Functions;

import io.jenkins.plugins.coverage.metrics.color.ColorProvider;
import io.jenkins.plugins.datatables.DetailedCell;

/**
 * A base class for coverage table models that handle the changes to a result of a reference build.
 */
abstract class ChangesTableModel extends CoverageTableModel {
    private final Node changeRoot;

    ChangesTableModel(final String id, final Node root, final Node changeRoot,
            final RowRenderer renderer, final ColorProvider colorProvider) {
        super(id, root, renderer, colorProvider);

        this.changeRoot = changeRoot;
    }

    @Override
    public List<Object> getRows() {
        Locale browserLocale = Functions.getCurrentLocale();
        return changeRoot.getAllFileNodes().stream()
                .map(file -> createRow(file, browserLocale))
                .collect(Collectors.toList());
    }

    abstract CoverageRow createRow(FileNode file, Locale browserLocale);

    FileNode getOriginalNode(final FileNode fileNode) {
        return getRoot().getAllFileNodes().stream()
                .filter(node -> node.getRelativePath().equals(fileNode.getRelativePath())
                        && node.getName().equals(fileNode.getName()))
                .findFirst()
                .orElse(fileNode); // return this as fallback to prevent exceptions
    }

    /**
     * UI row model for the rows of a table that show the changes.
     */
    static class ChangesRow extends CoverageRow {
        private final FileNode originalFile;

        ChangesRow(final FileNode originalFile, final FileNode changedFileNode,
                final Locale browserLocale, final RowRenderer renderer, final ColorProvider colorProvider) {
            super(changedFileNode, browserLocale, renderer, colorProvider);

            this.originalFile = originalFile;
        }

        FileNode getOriginalFile() {
            return originalFile;
        }

        @Override
        public DetailedCell<?> getLineCoverageDelta() {
            return createColoredModifiedLinesCoverageDeltaColumn(Metric.LINE);
        }

        @Override
        public DetailedCell<?> getBranchCoverageDelta() {
            return createColoredModifiedLinesCoverageDeltaColumn(Metric.BRANCH);
        }

        DetailedCell<?> createColoredModifiedLinesCoverageDeltaColumn(final Metric metric) {
            Coverage modifiedLinesCoverage = getFile().getTypedValue(metric, Coverage.nullObject(metric));
            if (modifiedLinesCoverage.isSet()) {
                return createColoredCoverageDeltaColumn(metric,
                        modifiedLinesCoverage.subtract(originalFile.getTypedValue(metric, Coverage.nullObject(metric))));
            }
            return NO_COVERAGE;
        }
    }
}
