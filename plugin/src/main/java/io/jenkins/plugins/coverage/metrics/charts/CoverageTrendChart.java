package io.jenkins.plugins.coverage.metrics.charts;

import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.JacksonFacade;
import edu.hm.hafner.echarts.line.LineSeries;
import edu.hm.hafner.echarts.line.LineSeries.FilledMode;
import edu.hm.hafner.echarts.line.LineSeries.StackedMode;
import edu.hm.hafner.echarts.line.LinesChartModel;
import edu.hm.hafner.echarts.line.LinesDataSet;

import io.jenkins.plugins.coverage.metrics.model.CoverageStatistics;
import io.jenkins.plugins.coverage.metrics.model.Messages;
import io.jenkins.plugins.echarts.JenkinsPalette;

/**
 * Builds the Java side model for a trend chart showing the line and branch coverage of a project. The number of builds
 * to consider is controlled by a {@link ChartModelConfiguration} instance. The created model object can be serialized
 * to JSON (e.g., using the {@link JacksonFacade}) and can be used 1:1 as ECharts configuration object in the
 * corresponding JS file.
 *
 * @author Ullrich Hafner
 * @see JacksonFacade
 */
public class CoverageTrendChart {
    /**
     * Creates the chart for the specified results.
     *
     * @param results
     *         the forensics results to render - these results must be provided in descending order, i.e. the current *
     *         build is the head of the list, then the previous builds, and so on
     * @param configuration
     *         the chart configuration to be used
     * @param metrics
     *         if the metrics or the coverage should be charted
     *
     * @return the chart model, ready to be serialized to JSON
     */
    public LinesChartModel create(final Iterable<BuildResult<CoverageStatistics>> results,
            final ChartModelConfiguration configuration, final boolean metrics) {
        CoverageSeriesBuilder builder = new CoverageSeriesBuilder();
        LinesDataSet dataSet = builder.createDataSet(configuration, results);

        var filledMode = metrics ? FilledMode.LINES : computeFilledMode(dataSet);

        LinesChartModel model = new LinesChartModel(dataSet);
        if (dataSet.isNotEmpty()) {
            model.useContinuousRangeAxis();
            model.setRangeMax(metrics ? dataSet.getMaximumValue() : 100);
            model.setRangeMin(dataSet.getMinimumValue());

            if (metrics) {
                addSeries(dataSet, model, Messages.Metric_COMPLEXITY(), CoverageSeriesBuilder.CYCLOMATIC_COMPLEXITY,
                        JenkinsPalette.ORANGE.normal(), filledMode);
                addSeries(dataSet, model, Messages.Metric_COGNITIVE_COMPLEXITY(), CoverageSeriesBuilder.COGNITIVE_COMPLEXITY,
                        JenkinsPalette.ORANGE.normal(), filledMode);
                addSeries(dataSet, model, Messages.Metric_NPATH(), CoverageSeriesBuilder.NPATH_COMPLEXITY,
                        JenkinsPalette.ORANGE.normal(), filledMode);
                addSeries(dataSet, model, Messages.Metric_NCSS(), CoverageSeriesBuilder.NCSS,
                        JenkinsPalette.ORANGE.normal(), filledMode);
            }
            else {
                addSeries(dataSet, model, Messages.Metric_LINE(), CoverageSeriesBuilder.LINE_COVERAGE,
                        JenkinsPalette.GREEN.normal(), filledMode);
                addSeries(dataSet, model, Messages.Metric_BRANCH(), CoverageSeriesBuilder.BRANCH_COVERAGE,
                        JenkinsPalette.GREEN.dark(), filledMode);
                addSeries(dataSet, model, Messages.Metric_MUTATION(), CoverageSeriesBuilder.MUTATION_COVERAGE,
                        JenkinsPalette.GREEN.dark(), filledMode);
                addSeries(dataSet, model, Messages.Metric_TEST_STRENGTH(), CoverageSeriesBuilder.TEST_STRENGTH,
                        JenkinsPalette.GREEN.light(), filledMode);

                addSeries(dataSet, model, Messages.Metric_MCDC_PAIR(), CoverageSeriesBuilder.MCDC_PAIR_COVERAGE,
                        JenkinsPalette.RED.light(), filledMode);
                addSeries(dataSet, model, Messages.Metric_METHOD(), CoverageSeriesBuilder.METHOD_COVERAGE,
                        JenkinsPalette.RED.normal(), filledMode);
                addSeries(dataSet, model, Messages.Metric_FUNCTION_CALL(), CoverageSeriesBuilder.FUNCTION_CALL_COVERAGE,
                        JenkinsPalette.RED.dark(), filledMode);
            }
        }
        return model;
    }

    /**
     * Returns the filled mode based on the contained coverage values. If the dataset contains MCDC or Function Call
     * coverage, then the filled mode is set to LINES, otherwise FILLED.
     *
     * @param dataSet
     *         the dataset to check
     *
     * @return the filled mode
     */
    private FilledMode computeFilledMode(final LinesDataSet dataSet) {
        if (dataSet.containsSeries(CoverageSeriesBuilder.MCDC_PAIR_COVERAGE)
                || dataSet.containsSeries(CoverageSeriesBuilder.FUNCTION_CALL_COVERAGE)) {
            return FilledMode.LINES;
        }
        return FilledMode.FILLED;
    }

    private void addSeries(final LinesDataSet dataSet, final LinesChartModel model,
            final String name, final String seriesId, final String color, final FilledMode filledMode) {
        if (dataSet.containsSeries(seriesId)) {
            LineSeries branchSeries = new LineSeries(name,
                    color, StackedMode.SEPARATE_LINES, filledMode, dataSet.getSeries(seriesId));

            model.addSeries(branchSeries);
        }
    }
}
