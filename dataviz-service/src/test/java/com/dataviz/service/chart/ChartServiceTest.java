package com.dataviz.service.chart;

import com.dataviz.domain.model.DataSet;
import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.ColumnType;
import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.chart.ChartConfig.ChartType;
import com.dataviz.domain.filter.FilterResult;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@DisplayName("ChartService - Тестування будування графіків")
class ChartServiceTest {

    private ChartService chartService;
    private DataSet testDataSet;

    @BeforeEach
    void setUp() {
        chartService = new ChartService();
        testDataSet = createTestDataSet();
    }

    private DataSet createTestDataSet() {
        List<DataColumn> columns = new ArrayList<>();

        List<Object> categories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            categories.add("Month_" + (i + 1));
        }
        columns.add(DataColumn.builder()
                .name("Month")
                .type(ColumnType.CATEGORICAL)
                .values(categories)
                .build());

        List<Object> sales = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sales.add((double) (Math.random() * 100000 + 50000));
        }
        columns.add(DataColumn.builder()
                .name("Sales")
                .type(ColumnType.NUMERIC)
                .values(sales)
                .build());

        List<Object> revenue = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            revenue.add((double) (Math.random() * 200000 + 100000));
        }
        columns.add(DataColumn.builder()
                .name("Revenue")
                .type(ColumnType.NUMERIC)
                .values(revenue)
                .build());

        return DataSet.builder()
                .id("chart-test-ds")
                .name("Chart Test Dataset")
                .columns(columns)
                .build();
    }

    @Test
    @DisplayName("Тип LINE: будування графіка з X та Y колонками")
    void testBuildChart_LINE() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-1")
                .chartType(ChartType.LINE)
                .title("Sales Over Time")
                .xColumn("Month")
                .yColumns(List.of("Sales", "Revenue"))
                .build();

        ChartService.ChartRenderResult result = chartService.buildChart(testDataSet, config);

        assertNotNull(result);
        assertEquals(testDataSet, result.getDataSet());
        ChartConfig resultConfig = result.getConfig();
        assertEquals(config.getId(), resultConfig.getId());
        assertEquals(config.getChartType(), resultConfig.getChartType());
        assertEquals(config.getTitle(), resultConfig.getTitle());
        assertEquals(config.getXColumn(), resultConfig.getXColumn());
        assertEquals(config.getYColumns(), resultConfig.getYColumns());
        assertEquals(config.getYColumns().size(), resultConfig.getStyle().getSeriesColors().size());
        assertFalse(result.hasFilter());
    }

    @Test
    @DisplayName("Тип BAR: будування графіка")
    void testBuildChart_BAR() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-2")
                .chartType(ChartType.BAR)
                .title("Revenue by Month")
                .xColumn("Month")
                .yColumns(List.of("Revenue"))
                .build();

        ChartService.ChartRenderResult result = chartService.buildChart(testDataSet, config);

        assertNotNull(result);
        assertEquals(ChartConfig.ChartType.BAR, result.getConfig().getChartType());
    }

    @Test
    @DisplayName("Невідома X колона: IllegalArgumentException")
    void testBuildChart_UnknownXColumn() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-3")
                .chartType(ChartType.LINE)
                .title("Error Chart")
                .xColumn("NonexistentX")
                .yColumns(List.of("Sales"))
                .build();

        assertThrows(IllegalArgumentException.class, 
                () -> chartService.buildChart(testDataSet, config));
    }

    @Test
    @DisplayName("Невідома Y колона: IllegalArgumentException")
    void testBuildChart_UnknownYColumn() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-4")
                .chartType(ChartType.LINE)
                .title("Error Chart")
                .xColumn("Month")
                .yColumns(List.of("NonexistentY"))
                .build();

        assertThrows(IllegalArgumentException.class, 
                () -> chartService.buildChart(testDataSet, config));
    }

    @Test
    @DisplayName("Порожна Y колона: IllegalArgumentException")
    void testBuildChart_EmptyYColumns() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-5")
                .chartType(ChartType.LINE)
                .title("Error Chart")
                .xColumn("Month")
                .yColumns(List.of())
                .build();

        assertThrows(IllegalArgumentException.class, 
                () -> chartService.buildChart(testDataSet, config));
    }

    @Test
    @DisplayName("З фільтром: buildChartFromFilter з відфільтрованими індексами")
    void testBuildChartFromFilter() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-6")
                .chartType(ChartType.LINE)
                .title("Filtered Sales")
                .xColumn("Month")
                .yColumns(List.of("Sales"))
                .build();

        List<Integer> matchedIndices = List.of(0, 1, 2, 3, 4);
        FilterResult filterResult = new FilterResult(testDataSet, matchedIndices);

        ChartService.ChartRenderResult result = chartService.buildChartFromFilter(filterResult, config);

        assertNotNull(result);
        assertTrue(result.hasFilter());
        assertEquals(matchedIndices, result.getActiveIndices());
    }

    @Test
    @DisplayName("З фільтром: hasFilter() = true при наявності фільтру")
    void testChartRenderResult_HasFilter() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-7")
                .chartType(ChartType.BAR)
                .title("Test")
                .xColumn("Month")
                .yColumns(List.of("Sales"))
                .build();

        List<Integer> indices = List.of(0, 1, 2);
        FilterResult filterResult = new FilterResult(testDataSet, indices);
        ChartService.ChartRenderResult result = chartService.buildChartFromFilter(filterResult, config);

        assertTrue(result.hasFilter());
    }

    @Test
    @DisplayName("Без фільтру: hasFilter() = false при відсутності фільтру")
    void testChartRenderResult_NoFilter() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-8")
                .chartType(ChartType.LINE)
                .title("No Filter")
                .xColumn("Month")
                .yColumns(List.of("Revenue"))
                .build();

        ChartService.ChartRenderResult result = chartService.buildChart(testDataSet, config);

        assertFalse(result.hasFilter());
        assertNull(result.getActiveIndices());
    }

    @Test
    @DisplayName("Кілька Y колон: всі включені у графік")
    void testBuildChart_MultipleYColumns() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-9")
                .chartType(ChartType.LINE)
                .title("Multi-Series")
                .xColumn("Month")
                .yColumns(List.of("Sales", "Revenue"))
                .build();

        ChartService.ChartRenderResult result = chartService.buildChart(testDataSet, config);

        assertEquals(2, result.getConfig().getYColumns().size());
        assertTrue(result.getConfig().getYColumns().contains("Sales"));
        assertTrue(result.getConfig().getYColumns().contains("Revenue"));
    }

    @Test
    @DisplayName("PIE діаграма: одна Y колона")
    void testBuildChart_PIE() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-10")
                .chartType(ChartType.PIE)
                .title("Sales Distribution")
                .xColumn("Month")
                .yColumns(List.of("Sales"))
                .build();

        ChartService.ChartRenderResult result = chartService.buildChart(testDataSet, config);

        assertNotNull(result);
        assertEquals(ChartConfig.ChartType.PIE, result.getConfig().getChartType());
    }

    @Test
    @DisplayName("SCATTER діаграма: числові колони")
    void testBuildChart_SCATTER() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-11")
                .chartType(ChartType.SCATTER)
                .title("Sales vs Revenue")
                .xColumn("Sales")
                .yColumns(List.of("Revenue"))
                .build();

        ChartService.ChartRenderResult result = chartService.buildChart(testDataSet, config);

        assertNotNull(result);
        assertEquals(ChartConfig.ChartType.SCATTER, result.getConfig().getChartType());
    }

    @Test
    @DisplayName("Орієнтація на дані: заголовок та мітки збережені")
    void testBuildChart_PreservesConfigDetails() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-12")
                .chartType(ChartType.BAR)
                .title("Custom Title")
                .xColumn("Month")
                .xLabel("Periods")
                .yLabel("Amount")
                .yColumns(List.of("Sales"))
                .build();

        ChartService.ChartRenderResult result = chartService.buildChart(testDataSet, config);

        assertEquals("Custom Title", result.getConfig().getTitle());
        assertEquals("Periods", result.getConfig().getXLabel());
        assertEquals("Amount", result.getConfig().getYLabel());
    }

    @Test
    @DisplayName("Фільтровані індекси: коректно розташовані в результаті")
    void testBuildChartFromFilter_CorrectIndices() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-13")
                .chartType(ChartType.LINE)
                .title("Test")
                .xColumn("Month")
                .yColumns(List.of("Sales"))
                .build();

        List<Integer> selectedIndices = List.of(1, 3, 5, 7, 9);
        FilterResult filterResult = new FilterResult(testDataSet, selectedIndices);
        ChartService.ChartRenderResult result = chartService.buildChartFromFilter(filterResult, config);

        assertEquals(selectedIndices, result.getActiveIndices());
    }

    @Test
    @DisplayName("Порожні фільтровані дані: activeIndices = пуста колекція")
    void testBuildChartFromFilter_EmptyResult() {
        ChartConfig config = ChartConfig.builder()
                .id("chart-14")
                .chartType(ChartType.LINE)
                .title("Empty")
                .xColumn("Month")
                .yColumns(List.of("Sales"))
                .build();

        List<Integer> emptyIndices = List.of();
        FilterResult filterResult = new FilterResult(testDataSet, emptyIndices);
        ChartService.ChartRenderResult result = chartService.buildChartFromFilter(filterResult, config);

        assertTrue(result.getActiveIndices().isEmpty());
    }
}
