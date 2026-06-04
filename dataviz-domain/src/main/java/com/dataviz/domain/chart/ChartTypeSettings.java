package com.dataviz.domain.chart;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ChartTypeSettings {
    public enum SettingType {
        SHOW_GRID,

        LINE_WIDTH,
        SMOOTHING,

        POINT_SIZE,
        POINT_SHAPE,
        SHOW_TREND_LINE,
        SERIES_TRANSPARENCY,

        BAR_WIDTH,
        BAR_ORIENTATION,
        STACKING_MODE,

        SLICE_LABELS,
        LEGEND_POSITION,
        DONUT_MODE,
        INNER_RADIUS,

        COLOR_SCALE,
        COLOR_RANGE_MIN,
        COLOR_RANGE_MAX,
        SHOW_AXIS_LABELS,
    }

    private static final Map<ChartConfig.ChartType, Set<SettingType>> APPLICABLE =
            new EnumMap<>(ChartConfig.ChartType.class);

    static {
        APPLICABLE.put(ChartConfig.ChartType.LINE, EnumSet.of(
                SettingType.SHOW_GRID,
                SettingType.SMOOTHING,
                SettingType.POINT_SIZE
        ));

        APPLICABLE.put(ChartConfig.ChartType.AREA, EnumSet.of(
                SettingType.SHOW_GRID,
                SettingType.STACKING_MODE
        ));

        APPLICABLE.put(ChartConfig.ChartType.BAR, EnumSet.of(
                SettingType.SHOW_GRID,
                SettingType.BAR_WIDTH,
                SettingType.STACKING_MODE
        ));

        APPLICABLE.put(ChartConfig.ChartType.SCATTER, EnumSet.of(
                SettingType.SHOW_GRID,
                SettingType.POINT_SIZE,
                SettingType.POINT_SHAPE,
                SettingType.SHOW_TREND_LINE,
                SettingType.SERIES_TRANSPARENCY
        ));

        APPLICABLE.put(ChartConfig.ChartType.PIE, EnumSet.of(
                SettingType.SLICE_LABELS,
                SettingType.LEGEND_POSITION
        ));

        APPLICABLE.put(ChartConfig.ChartType.HEATMAP, EnumSet.of(
                SettingType.COLOR_SCALE,
                SettingType.COLOR_RANGE_MIN,
                SettingType.COLOR_RANGE_MAX,
                SettingType.SHOW_AXIS_LABELS
        ));
    }

    private ChartTypeSettings() {}

    public static boolean isApplicable(ChartConfig.ChartType type, SettingType setting) {
        if (type == null || setting == null) return false;
        Set<SettingType> set = APPLICABLE.get(type);
        return set != null && set.contains(setting);
    }

    public static Set<SettingType> applicableFor(ChartConfig.ChartType type) {
        Set<SettingType> set = APPLICABLE.get(type);
        return set != null ? Set.copyOf(set) : Set.of();
    }
}