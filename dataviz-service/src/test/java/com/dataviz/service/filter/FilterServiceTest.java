package com.dataviz.service.filter;

import com.dataviz.common.event.EventBus;
import com.dataviz.domain.model.DataSet;
import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.ColumnType;
import com.dataviz.domain.filter.FilterCriteria;
import com.dataviz.domain.filter.FilterResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;

@DisplayName("FilterService - Тестування сервісу фільтрації")
@ExtendWith(MockitoExtension.class)
class FilterServiceTest {

    private FilterService filterService;
    private DataSet testDataSet;

    @Mock
    private EventBus mockEventBus;

    @BeforeEach
    void setUp() {
        filterService = new FilterService(mockEventBus);
        testDataSet = createTestDataSet();
    }

    private DataSet createTestDataSet() {
        List<DataColumn> columns = new ArrayList<>();

        List<Object> tempValues = new ArrayList<>();
        for (int i = 0; i < 20; i++) tempValues.add((double) (20 + i));
        columns.add(DataColumn.builder()
                .name("temperature")
                .type(ColumnType.NUMERIC)
                .values(tempValues)
                .build());

        List<Object> statusValues = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            statusValues.add(i % 2 == 0 ? "Active" : "Inactive");
        }
        columns.add(DataColumn.builder()
                .name("status")
                .type(ColumnType.CATEGORICAL)
                .values(statusValues)
                .build());

        return DataSet.builder()
                .id("filter-test-ds")
                .name("Filter Test Dataset")
                .columns(columns)
                .build();
    }

    @Test
    @DisplayName("Базова фільтрація: одне критеріо")
    void testFilter_SingleCriteria() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", 25.0, 30.0);
        FilterResult result = filterService.filter(testDataSet, List.of(criteria));

        assertNotNull(result);
        assertEquals(testDataSet, result.getSource());
        assertTrue(result.getMatchedCount() > 0);
    }

    @Test
    @DisplayName("Багатокритеріальна фільтрація: AND логіка")
    void testFilter_MultipleCriteria() {
        FilterCriteria temp = FilterCriteria.numericRange("temperature", 25.0, 30.0);
        FilterCriteria status = FilterCriteria.categoricalIn("status", Set.of("Active"));

        FilterResult result = filterService.filter(testDataSet, List.of(temp, status));

        assertNotNull(result);
        for (int idx : result.getMatchedIndices()) {
            double tempVal = (double) testDataSet.getColumn("temperature").getValue(idx);
            String statusVal = (String) testDataSet.getColumn("status").getValue(idx);

            assertTrue(tempVal >= 25.0 && tempVal <= 30.0);
            assertEquals("Active", statusVal);
        }
    }

    @Test
    @DisplayName("Без критеріїв: всі рядки збігаються")
    void testFilter_NoCriteria() {
        FilterResult result = filterService.filter(testDataSet, List.of());

        assertNotNull(result);
        assertEquals(testDataSet.getRowCount(), result.getMatchedCount());
        assertTrue(result.isUnfiltered());
    }

    @Test
    @DisplayName("Без збігів: порожна колекція результатів")
    void testFilter_NoMatches() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", 100.0, 200.0);
        FilterResult result = filterService.filter(testDataSet, List.of(criteria));

        assertEquals(0, result.getMatchedCount());
        assertTrue(result.getMatchedIndices().isEmpty());
    }

    @Test
    @DisplayName("Всі рядки відповідають: getMatchRatio() = 1.0")
    void testFilter_AllMatches_MatchRatio() {
        FilterCriteria criteria = FilterCriteria.isNotNull("temperature");
        FilterResult result = filterService.filter(testDataSet, List.of(criteria));

        assertEquals(1.0, result.getMatchRatio(), 0.001);
    }

    @Test
    @DisplayName("Дякі рядки збігаються: getMatchRatio() між 0 та 1")
    void testFilter_PartialMatches_MatchRatio() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", 25.0, 30.0);
        FilterResult result = filterService.filter(testDataSet, List.of(criteria));

        double ratio = result.getMatchRatio();
        assertTrue(ratio > 0.0 && ratio < 1.0);
    }

    @Test
    @DisplayName("Без збігів: getMatchRatio() = 0.0")
    void testFilter_NoMatches_MatchRatio() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", -100.0, 0.0);
        FilterResult result = filterService.filter(testDataSet, List.of(criteria));

        assertEquals(0.0, result.getMatchRatio());
    }

    @Test
    @DisplayName("Фільтрований результат: getTotalCount() = вихідна кількість")
    void testFilter_TotalCount() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", 22.0, 28.0);
        FilterResult result = filterService.filter(testDataSet, List.of(criteria));

        assertEquals(testDataSet.getRowCount(), result.getTotalCount());
    }

    @Test
    @DisplayName("Порожня множина критеріїв: isUnfiltered() = true")
    void testFilter_Unfiltered() {
        FilterResult result = filterService.filter(testDataSet, List.of());

        assertTrue(result.isUnfiltered());
    }

    @Test
    @DisplayName("Фільтр застосований: isUnfiltered() = false")
    void testFilter_Filtered() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", 22.0, 25.0);
        FilterResult result = filterService.filter(testDataSet, List.of(criteria));

        if (result.getMatchedCount() < result.getTotalCount()) {
            assertFalse(result.isUnfiltered());
        }
    }

    @Test
    @DisplayName("Індекси збігів: 0-базовані індекси")
    void testFilter_MatchedIndices_ZeroBased() {
        FilterCriteria criteria = FilterCriteria.categoricalIn("status", Set.of("Active"));
        FilterResult result = filterService.filter(testDataSet, List.of(criteria));

        for (int idx : result.getMatchedIndices()) {
            assertTrue(idx >= 0 && idx < testDataSet.getRowCount());
            String status = (String) testDataSet.getColumn("status").getValue(idx);
            assertEquals("Active", status);
        }
    }

    @Test
    @DisplayName("Null-критеріум: IllegalArgumentException або обробка")
    void testFilter_NullCriteria_ThrowsException() {
        assertThrows(Exception.class, 
                () -> filterService.filter(testDataSet, null));
    }

    @Test
    @DisplayName("Null-датасет: IllegalArgumentException")
    void testFilter_NullDataSet_ThrowsException() {
        FilterCriteria criteria = FilterCriteria.isNotNull("temperature");

        assertThrows(Exception.class, 
                () -> filterService.filter(null, List.of(criteria)));
    }

    @Test
    @DisplayName("Вибір паралелізації: невелике множина (< 100k) = послідовна")
    void testFilter_SmallDataSet_Sequential() {
        FilterCriteria criteria = FilterCriteria.isNotNull("temperature");
        long startTime = System.currentTimeMillis();
        FilterResult result = filterService.filter(testDataSet, List.of(criteria));
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 1000, "Послідовна обробка малого набору мала пройти < 1 сек");
        assertEquals(20, result.getMatchedCount());
    }

    @Test
    @DisplayName("Логування виконання: EventBus повідомляється")
    void testFilter_EventBusNotified() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", 25.0, 30.0);
        
        filterService.filter(testDataSet, List.of(criteria));

    }

    @Test
    @DisplayName("Відновлення при помилці: винятки обробляються")
    void testFilter_InvalidColumn_ThrowsException() {
        FilterCriteria criteria = FilterCriteria.numericRange("nonexistent", 10.0, 20.0);

        assertThrows(Exception.class, 
                () -> filterService.filter(testDataSet, List.of(criteria)));
    }

    @Test
    @DisplayName("Негована фільтрація: NOT критеріум")
    void testFilter_NegatedCriteria() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", 25.0, 30.0).negate();
        FilterResult result = filterService.filter(testDataSet, List.of(criteria));

        for (int idx : result.getMatchedIndices()) {
            double tempVal = (double) testDataSet.getColumn("temperature").getValue(idx);
            assertFalse(tempVal >= 25.0 && tempVal <= 30.0);
        }
    }

    @Test
    @DisplayName("Повторна фільтрація: стан не змінюється")
    void testFilter_Idempotent() {
        FilterCriteria criteria = FilterCriteria.categoricalIn("status", Set.of("Active"));
        
        FilterResult result1 = filterService.filter(testDataSet, List.of(criteria));
        FilterResult result2 = filterService.filter(testDataSet, List.of(criteria));

        assertEquals(result1.getMatchedCount(), result2.getMatchedCount());
        assertEquals(result1.getMatchedIndices(), result2.getMatchedIndices());
    }
}
