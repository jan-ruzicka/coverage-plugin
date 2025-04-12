package io.jenkins.plugins.coverage.metrics.color;

import org.junit.jupiter.api.Test;

import java.awt.*;

import io.jenkins.plugins.coverage.metrics.color.ColorProvider.DisplayColors;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageChangeLevel}.
 *
 * @author Florian Orendi
 */
class CoverageChangeLevelTest {
    private static final ColorProvider COLOR_PROVIDER = ColorProviderFactory.createDefaultColorProvider();

    @Test
    void shouldHaveWorkingGetters() {
        var coverageChangeLevel = CoverageChangeLevel.INCREASE_2;
        assertThat(coverageChangeLevel.getChange()).isEqualTo(2.0);
        assertThat(coverageChangeLevel.getColorizationId()).isEqualTo(ColorId.VERY_GOOD);
    }

    @Test
    void shouldGetDisplayColorsOfCoveragePercentage() {
        var blendedLineColor = COLOR_PROVIDER.getDisplayColorsOf(ColorId.BLACK).getFillColor();
        Color blendedColorIncreased = ColorProvider.blendColors(
                COLOR_PROVIDER.getDisplayColorsOf(CoverageChangeLevel.INCREASE_2.getColorizationId()).getFillColor(),
                COLOR_PROVIDER.getDisplayColorsOf(CoverageChangeLevel.EQUALS.getColorizationId()).getFillColor());
        Color blendedColorDecreased = ColorProvider.blendColors(
                COLOR_PROVIDER.getDisplayColorsOf(CoverageChangeLevel.DECREASE_2.getColorizationId()).getFillColor(),
                COLOR_PROVIDER.getDisplayColorsOf(CoverageChangeLevel.EQUALS.getColorizationId()).getFillColor());

        assertThat(CoverageChangeLevel.getDisplayColorsOfCoverageChange(1.0, COLOR_PROVIDER))
                .isEqualTo(new DisplayColors(blendedLineColor, blendedColorIncreased));
        assertThat(CoverageChangeLevel.getDisplayColorsOfCoverageChange(-1.0, COLOR_PROVIDER))
                .isEqualTo(new DisplayColors(blendedLineColor, blendedColorDecreased));
        assertThat(CoverageChangeLevel.getDisplayColorsOfCoverageChange(7.0, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.EXCELLENT));
        assertThat(CoverageChangeLevel.getDisplayColorsOfCoverageChange(-2.0, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.INADEQUATE));
        assertThat(CoverageChangeLevel.getDisplayColorsOfCoverageChange(-110.0, COLOR_PROVIDER))
                .isEqualTo(COLOR_PROVIDER.getDisplayColorsOf(ColorId.WHITE));
    }
}
