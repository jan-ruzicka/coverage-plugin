package io.jenkins.plugins.coverage.metrics.restapi;

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Value;

import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.model.CoverageStatistics;
import io.jenkins.plugins.coverage.metrics.model.ElementFormatter;
import io.jenkins.plugins.util.QualityGateResult;
import io.jenkins.plugins.util.QualityGateResult.QualityGateResultApi;

/**
 * Remote API to list the details of the coverage results.
 *
 * @author Ullrich Hafner
 */
@ExportedBean
public class CoverageApi {
    private static final ElementFormatter FORMATTER = new ElementFormatter();
    private final CoverageStatistics statistics;
    private final QualityGateResult qualityGateResult;
    private final String referenceBuild;

    /**
     * Creates a new instance of {@link CoverageApi}.
     *
     * @param statistics
     *         the coverage statistics of the build.
     * @param qualityGateResult
     *         the quality gate result of the build.
     * @param referenceBuild
     *         the build referenced for comparison purposes.
     */
    public CoverageApi(final CoverageStatistics statistics, final QualityGateResult qualityGateResult,
            final String referenceBuild) {
        this.statistics = statistics;
        this.qualityGateResult = qualityGateResult;
        this.referenceBuild = referenceBuild;
    }

    @Exported(inline = true)
    public QualityGateResultApi getQualityGates() {
        return new QualityGateResultApi(qualityGateResult);
    }

    @Exported
    public String getReferenceBuild() {
        return referenceBuild;
    }

    /**
     * Returns the statistics for the project coverage.
     *
     * @return a mapping of metrics to their values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getProjectStatistics() {
        return mapToStrings(Baseline.PROJECT);
    }

    /**
     * Returns the delta values for the project coverage.
     *
     * @return a mapping of metrics to their values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getProjectDelta() {
        return mapToStrings(Baseline.PROJECT_DELTA);
    }

    /**
     * Returns the statistics for the coverage of modified files.
     *
     * @return a mapping of metrics to their values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getModifiedFilesStatistics() {
        return mapToStrings(Baseline.MODIFIED_FILES);
    }

    /**
     * Returns the delta values for the modified files coverage.
     *
     * @return a mapping of metrics to their delta values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getModifiedFilesDelta() {
        return mapToStrings(Baseline.MODIFIED_FILES_DELTA);
    }

    /**
     * Returns the statistics for the coverage of modified lines.
     *
     * @return a mapping of metrics to their values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getModifiedLinesStatistics() {
        return mapToStrings(Baseline.MODIFIED_LINES);
    }

    /**
     * Returns the delta values for the modified lines coverage.
     *
     * @return a mapping of metrics to their delta values (only metrics with a value are included)
     */
    @Exported(inline = true)
    public NavigableMap<String, String> getModifiedLinesDelta() {
        return mapToStrings(Baseline.MODIFIED_LINES_DELTA);
    }

    private NavigableMap<String, String> mapToStrings(final Baseline baseline) {
        var values = new TreeMap<String, String>();

        for (Metric metric : Metric.values()) {
            statistics.getValue(baseline, metric)
                    .ifPresent(value -> values.put(metric.toTagName(), format(baseline, value)));
        }

        return values;
    }

    private String format(final Baseline baseline, final Value value) {
        return baseline.isDelta() ? formatDelta(value) : formatValue(value);
    }

    private String formatValue(final Value value) {
        return FORMATTER.format(value, Locale.ENGLISH);
    }

    private String formatDelta(final Value value) {
        return FORMATTER.formatDelta(value, Locale.ENGLISH);
    }
}
