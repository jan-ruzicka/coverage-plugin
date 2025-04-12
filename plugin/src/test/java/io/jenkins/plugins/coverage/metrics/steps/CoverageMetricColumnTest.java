package io.jenkins.plugins.coverage.metrics.steps;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.Difference;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;

import io.jenkins.plugins.coverage.metrics.AbstractCoverageTest;
import io.jenkins.plugins.coverage.metrics.color.ColorProvider;
import io.jenkins.plugins.coverage.metrics.color.ColorProviderFactory;
import io.jenkins.plugins.coverage.metrics.color.CoverageChangeTendency;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.steps.CoverageMetricColumn.CoverageMetricColumnDescriptor;
import io.jenkins.plugins.util.JenkinsFacade;
import io.jenkins.plugins.util.QualityGateResult;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link CoverageMetricColumn}.
 *
 * @author Florian Orendi
 */
@DefaultLocale("en")
class CoverageMetricColumnTest extends AbstractCoverageTest {
    private static final String COLUMN_NAME = "Test Column";
    private static final Metric COVERAGE_METRIC = Metric.BRANCH;

    private static final ColorProvider COLOR_PROVIDER = ColorProviderFactory.createDefaultColorProvider();

    @Test
    void shouldAllowEnums() {
        var jenkins = mock(JenkinsFacade.class);
        var descriptor = new CoverageMetricColumnDescriptor(jenkins);

        assertThat(descriptor.doFillMetricItems()).isEmpty();
        assertThat(descriptor.doFillBaselineItems()).isEmpty();

        when(jenkins.hasPermission(Jenkins.READ)).thenReturn(true);
        assertThat(descriptor.doFillMetricItems()).isNotEmpty();
        assertThat(descriptor.doFillBaselineItems()).isNotEmpty();
    }

    /**
     * Creates a stub for a {@link Job} that has the specified actions attached.
     *
     * @param actions
     *         The actions to attach, might be empty
     *
     * @return the created stub
     */
    @VisibleForTesting
    public static Job<?, ?> createJobWithActions(final CoverageBuildAction... actions) {
        Job<?, ?> job = mock(Job.class);
        Run<?, ?> build = createBuildWithActions(actions);
        when(job.getLastCompletedBuild()).thenAnswer(a -> build);
        return job;
    }

    /**
     * Creates a stub for a {@link Run} that has the specified actions attached.
     *
     * @param actions
     *         the actions to attach, might be empty
     *
     * @return the created stub
     */
    @VisibleForTesting
    public static Run<?, ?> createBuildWithActions(final CoverageBuildAction... actions) {
        Run<?, ?> build = mock(Run.class);
        when(build.getActions(CoverageBuildAction.class)).thenReturn(Arrays.asList(actions));
        if (actions.length > 0) {
            when(build.getAction(CoverageBuildAction.class)).thenReturn(actions[0]);
        }
        return build;
    }

    @Test
    void shouldHaveWorkingDataGetters() {
        var column = createColumn();

        assertThat(column.getColumnName()).isEqualTo(COLUMN_NAME);
        assertThat(column.getBaseline()).isEqualTo(Baseline.PROJECT);
        assertThat(column.getMetric()).isEqualTo(COVERAGE_METRIC);
        assertThat(column.getRelativeCoverageUrl(mock(Job.class))).isEmpty();
    }

    @Test
    void shouldProvideSelectedColumn() {
        var column = createColumn();
        Job<?, ?> job = createJobWithCoverageAction();

        column.setBaseline(Baseline.PROJECT);
        assertThat(column.getBaseline()).isEqualTo(Baseline.PROJECT);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#overview");

        column.setBaseline(Baseline.PROJECT_DELTA);
        assertThat(column.getBaseline()).isEqualTo(Baseline.PROJECT_DELTA);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#overview");

        column.setBaseline(Baseline.MODIFIED_LINES);
        assertThat(column.getBaseline()).isEqualTo(Baseline.MODIFIED_LINES);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#modifiedLinesCoverage");

        column.setBaseline(Baseline.MODIFIED_LINES_DELTA);
        assertThat(column.getBaseline()).isEqualTo(Baseline.MODIFIED_LINES_DELTA);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#modifiedLinesCoverage");

        column.setBaseline(Baseline.MODIFIED_FILES);
        assertThat(column.getBaseline()).isEqualTo(Baseline.MODIFIED_FILES);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#modifiedFilesCoverage");

        column.setBaseline(Baseline.MODIFIED_FILES_DELTA);
        assertThat(column.getBaseline()).isEqualTo(Baseline.MODIFIED_FILES_DELTA);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#modifiedFilesCoverage");

        column.setBaseline(Baseline.INDIRECT);
        assertThat(column.getBaseline()).isEqualTo(Baseline.INDIRECT);
        assertThat(column.getRelativeCoverageUrl(job)).isEqualTo("coverage/#indirectCoverage");
    }

    @Test
    void shouldProvideBackgroundColorFillPercentage() {
        var column = createColumn();

        assertThat(column.getBackgroundColorFillPercentage("+5,0%")).isEqualTo("100%");
        assertThat(column.getBackgroundColorFillPercentage("+5.0%")).isEqualTo("100%");
        assertThat(column.getBackgroundColorFillPercentage("5,00%")).isEqualTo("5.00%");
        assertThat(column.getBackgroundColorFillPercentage("5.00%")).isEqualTo("5.00%");
    }

    @Test
    void shouldShowNoResultIfBuild() {
        var column = createColumn();

        Job<?, ?> job = mock(Job.class);

        assertThat(column.getCoverageText(job)).isEqualTo(Messages.Coverage_Not_Available());

        Optional<? extends Value> coverageValue = column.getCoverageValue(job);
        assertThat(coverageValue).isEmpty();
        assertThat(column.getDisplayColors(job, Optional.empty())).isEqualTo(ColorProvider.DEFAULT_COLOR);
    }

    @Test
    void shouldShowNoResultIfNoAction() {
        var column = createColumn();

        Job<?, ?> job = createJobWithActions();

        assertThat(column.getCoverageText(job)).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(column.getCoverageValue(job)).isEmpty();
        assertThat(column.getDisplayColors(job, Optional.empty())).isEqualTo(ColorProvider.DEFAULT_COLOR);
    }

    @Test
    void shouldShowNoResultForUnavailableMetric() {
        var column = createColumn();
        column.setMetric(Metric.MUTATION);

        Job<?, ?> job = createJobWithCoverageAction();

        assertThat(column.getCoverageText(job)).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(column.getCoverageValue(job)).isEmpty();

        column.setBaseline(Baseline.PROJECT_DELTA);

        assertThat(column.getCoverageText(job)).isEqualTo(Messages.Coverage_Not_Available());
        assertThat(column.getCoverageValue(job)).isEmpty();
    }

    @Test
    void shouldCalculateProjectCoverage() {
        CoverageMetricColumn column = createColumn();

        Job<?, ?> job = createJobWithCoverageAction();

        assertThat(column.getCoverageText(job)).isEqualTo("93.97%");
        assertThat(column.getCoverageValue(job))
                .isNotEmpty()
                .satisfies(coverage -> {
                    assertThat(coverage).contains(
                            new CoverageBuilder().withMetric(Metric.BRANCH).withCovered(109).withMissed(7).build());
                    assertThat(column.getDisplayColors(job, coverage).getLineColor())
                            .isEqualTo(Color.white);
                });
    }

    @Test
    void shouldCalculateProjectCoverageDelta() {
        CoverageMetricColumn column = createColumn();
        column.setBaseline(Baseline.PROJECT_DELTA);

        Job<?, ?> job = createJobWithCoverageAction();
        assertThat(column.getCoverageText(job)).isEqualTo("+5.00%");

        assertThat(column.getCoverageValue(job))
                .isNotEmpty()
                .satisfies(coverage -> {
                    assertThat(coverage).contains(new Difference(Metric.BRANCH, 5));
                    assertThat(column.getDisplayColors(job, coverage))
                            .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(
                                    CoverageChangeTendency.INCREASED.getColorizationId()));
                });
    }

    private CoverageMetricColumn createColumn() {
        var column = new CoverageMetricColumn();
        column.setColumnName(COLUMN_NAME);
        column.setBaseline(Baseline.PROJECT);
        column.setMetric(COVERAGE_METRIC);
        return column;
    }

    private Job<?, ?> createJobWithCoverageAction() {
        var node = readJacocoResult(JACOCO_CODING_STYLE_FILE);
        var run = mock(Run.class);
        var delta = List.of(new Difference(Metric.BRANCH, 5));
        var coverageBuildAction = new CoverageBuildAction(run, "coverage", "Code Coverage", StringUtils.EMPTY,
                node, new QualityGateResult(), new FilteredLog("Test"),
                "-", delta, List.of(), List.of(), List.of(), List.of(), List.of(), false);
        when(run.getAction(CoverageBuildAction.class)).thenReturn(coverageBuildAction);
        when(run.getActions(CoverageBuildAction.class)).thenReturn(Collections.singletonList(coverageBuildAction));

        var job = mock(Job.class);
        when(job.getLastCompletedBuild()).thenReturn(run);

        return job;
    }
}
