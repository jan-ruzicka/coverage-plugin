package io.jenkins.plugins.coverage.metrics.model;

import java.util.function.BiFunction;

import org.jvnet.localizer.Localizable;

import io.jenkins.plugins.coverage.metrics.color.ColorProvider;
import io.jenkins.plugins.coverage.metrics.color.ColorProvider.DisplayColors;
import io.jenkins.plugins.coverage.metrics.color.CoverageChangeTendency;
import io.jenkins.plugins.coverage.metrics.color.CoverageLevel;

/**
 * The baseline for the code coverage computation.
 */
public enum Baseline {
    /**
     * Coverage of the whole project. This is an absolute value that might not change much from build to build.
     */
    PROJECT(Messages._Baseline_PROJECT(), "overview",
            CoverageLevel::getDisplayColorsOfCoverageLevel, false),
    /**
     * Difference between the project coverages of the current build and the reference build. Teams can use this delta
     * value to ensure that the coverage will not decrease.
     */
    PROJECT_DELTA(Messages._Baseline_PROJECT_DELTA(), "overview",
            CoverageChangeTendency::getDisplayColorsForTendency, true),
    /**
     * Coverage of the modified lines (e.g., within the modified lines of a pull or merge request) will focus on new or
     * modified code only.
     */
    MODIFIED_LINES(Messages._Baseline_MODIFIED_LINES(), "modifiedLinesCoverage",
            CoverageLevel::getDisplayColorsOfCoverageLevel, false),
    /**
     * Difference between the project coverage and the modified lines coverage of the current build. Teams can use this
     * delta value to ensure that the coverage of pull requests is better than the whole project coverage.
     */
    MODIFIED_LINES_DELTA(Messages._Baseline_MODIFIED_LINES_DELTA(), "modifiedLinesCoverage",
            CoverageChangeTendency::getDisplayColorsForTendency, true),
    /**
     * Coverage of the modified files (e.g., within the files that have been touched in a pull or merge request) will
     * focus on new or modified code only.
     */
    MODIFIED_FILES(Messages._Baseline_MODIFIED_FILES(), "modifiedFilesCoverage",
            CoverageLevel::getDisplayColorsOfCoverageLevel, false),
    /**
     * Difference between the project coverage and the modified file coverage of the current build. Teams can use this
     * delta value to ensure that the coverage of pull requests is better than the whole project coverage.
     */
    MODIFIED_FILES_DELTA(Messages._Baseline_MODIFIED_FILES_DELTA(), "modifiedFilesCoverage",
            CoverageChangeTendency::getDisplayColorsForTendency, true),
    /**
     * Indirect changes of the overall code coverage that are not part of the changed code. These changes might occur
     * if new tests are added without touching the underlying code under test.
     */
    INDIRECT(Messages._Baseline_INDIRECT(), "indirectCoverage",
            CoverageLevel::getDisplayColorsOfCoverageLevel, false);

    private final Localizable title;
    private final String url;
    private final BiFunction<Double, ColorProvider, DisplayColors> colorMapper;
    private final boolean delta;

    Baseline(final Localizable title, final String url,
            final BiFunction<Double, ColorProvider, DisplayColors> colorMapper,
            final boolean delta) {
        this.title = title;
        this.url = url;
        this.colorMapper = colorMapper;
        this.delta = delta;
    }

    public String getTitle() {
        return title.toString();
    }

    public String getUrl() {
        return "#" + url;
    }

    /**
     * Returns the display colors to use render a value of this baseline.
     *
     * @param value
     *         the value to render
     * @param colorProvider
     *         the color provider to use
     *
     * @return the display colors to use
     */
    public DisplayColors getDisplayColors(final double value, final ColorProvider colorProvider) {
        return colorMapper.apply(value, colorProvider);
    }

    public boolean isDelta() {
        return delta;
    }
}
