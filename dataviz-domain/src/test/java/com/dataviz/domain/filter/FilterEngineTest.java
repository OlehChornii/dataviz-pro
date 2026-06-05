package com.dataviz.domain.filter;

import com.dataviz.domain.model.DataSet;
import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.ColumnType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FilterEngine - Тестування фільтрації даних")
class FilterEngineTest {

    private DataSet testDataSet;

    @BeforeEach
    void setUp() {
        testDataSet = createTestDataSet();
    }

    private DataSet createTestDataSet() {
        List<DataColumn> columns = new ArrayList<>();

        List<Object> numericValues = new ArrayList<>();
        for (int i = 1; i <= 25; i++) numericValues.add((double) i);
        columns.add(DataColumn.builder()
                .name("temperature")
                .type(ColumnType.NUMERIC)
                .values(numericValues)
                .build());

        List<Object> categoricalValues = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            categoricalValues.add(switch (i % 3) {
                case 0 -> "Red";
                case 1 -> "Green";
                default -> "Blue";
            });
        }
        columns.add(DataColumn.builder()
                .name("color")
                .type(ColumnType.CATEGORICAL)
                .values(categoricalValues)
                .build());

        List<Object> boolValues = new ArrayList<>();
        for (int i = 0; i < 25; i++) boolValues.add(i % 2 == 0);
        columns.add(DataColumn.builder()
                .name("active")
                .type(ColumnType.BOOLEAN)
                .values(boolValues)
                .build());

        return DataSet.builder()
                .id("test-ds-1")
                .name("Test Dataset")
                .columns(columns)
                .build();
    }

    @Test
    @DisplayName("Числовий діапазон: мінімальне значення включено")
    void testNumericRangeInclusive_Min() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", 5.0, 10.0);
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertEquals(6, matches.size());
        assertTrue(matches.containsAll(List.of(4, 5, 6, 7, 8, 9)));
    }

    @Test
    @DisplayName("Числовий діапазон: максимальне значення включено")
    void testNumericRangeInclusive_Max() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", 20.0, 25.0);
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertEquals(6, matches.size());
        assertTrue(matches.contains(19));
        assertTrue(matches.contains(24));
    }

    @Test
    @DisplayName("Числовий діапазон: межові випадки (min = max)")
    void testNumericRangeEdge_EqualMinMax() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", 10.0, 10.0);
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertEquals(1, matches.size());
        assertEquals(9, matches.get(0));
    }

    @Test
    @DisplayName("Числовий діапазон: відсутні збіги")
    void testNumericRangeNoMatches() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", 50.0, 100.0);
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertTrue(matches.isEmpty());
    }

    @Test
    @DisplayName("Категоріальний фільтр: множина допустимих значень")
    void testCategoricalIn_MultipleValues() {
        FilterCriteria criteria = FilterCriteria.categoricalIn("color", Set.of("Red", "Blue"));
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertEquals(17, matches.size());
    }

    @Test
    @DisplayName("Категоріальний фільтр: одне значення")
    void testCategoricalIn_SingleValue() {
        FilterCriteria criteria = FilterCriteria.categoricalIn("color", Set.of("Green"));
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertEquals(8, matches.size());
    }

    @Test
    @DisplayName("Категоріальний фільтр: порожня множина = без збігів")
    void testCategoricalIn_EmptySet() {
        FilterCriteria criteria = FilterCriteria.categoricalIn("color", Set.of());
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertTrue(matches.isEmpty());
    }

    @Test
    @DisplayName("Булев фільтр: істинні значення")
    void testBooleanEquals_True() {
        FilterCriteria criteria = FilterCriteria.booleanEquals("active", true);
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertEquals(13, matches.size());
    }

    @Test
    @DisplayName("Булев фільтр: хибні значення")
    void testBooleanEquals_False() {
        FilterCriteria criteria = FilterCriteria.booleanEquals("active", false);
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertEquals(12, matches.size());
    }

    @Test
    @DisplayName("Рядковий фільтр: case-insensitive пошук підстроки")
    void testStringContains_CaseInsensitive() {
        FilterCriteria criteria = FilterCriteria.stringContains("color", "red");
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertEquals(9, matches.size());
    }

    @Test
    @DisplayName("Рядковий фільтр: підстрока не знайдена")
    void testStringContains_NotFound() {
        FilterCriteria criteria = FilterCriteria.stringContains("color", "Yellow");
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertTrue(matches.isEmpty());
    }

    @Test
    @DisplayName("IsNotNull фільтр: всі значення не null")
    void testIsNotNull() {
        FilterCriteria criteria = FilterCriteria.isNotNull("temperature");
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertEquals(25, matches.size());
    }

    @Test
    @DisplayName("Багатокритеріальна фільтрація: AND логіка")
    void testMultipleCriteria_AND() {
        FilterCriteria temp = FilterCriteria.numericRange("temperature", 10.0, 20.0);
        FilterCriteria color = FilterCriteria.categoricalIn("color", Set.of("Red"));
        
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(temp, color));

        for (int idx : matches) {
            double tempVal = (double) testDataSet.getColumn("temperature").getValue(idx);
            String colorVal = (String) testDataSet.getColumn("color").getValue(idx);
            
            assertTrue(tempVal >= 10.0 && tempVal <= 20.0);
            assertEquals("Red", colorVal);
        }
    }

    @Test
    @DisplayName("Негована фільтрація: NOT числовий діапазон")
    void testNegation_NumericRange() {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", 10.0, 15.0).negate();
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        assertEquals(19, matches.size());
    }

    @Test
    @DisplayName("Негована фільтрація: подвійна негація = оригінал")
    void testDoubleNegation() {
        FilterCriteria original = FilterCriteria.numericRange("temperature", 5.0, 10.0);
        FilterCriteria doubleNegated = original.negate().negate();
        
        List<Integer> originalMatches = FilterEngine.apply(testDataSet, List.of(original));
        List<Integer> doubleNegatedMatches = FilterEngine.apply(testDataSet, List.of(doubleNegated));

        assertEquals(originalMatches, doubleNegatedMatches);
    }

    @Test
    @DisplayName("Порожній набір даних: фільтр не знаходить результатів")
    void testEmptyDataSet() {
        DataSet emptySet = DataSet.builder()
                .id("empty")
                .name("Empty")
                .columns(List.of())
                .build();

        FilterCriteria criteria = FilterCriteria.isNotNull("nonexistent");
        List<Integer> matches = FilterEngine.apply(emptySet, List.of(criteria));

        assertTrue(matches.isEmpty());
    }

    @Test
    @DisplayName("Без критеріїв: повернути всі індекси")
    void testNoCriteria_ReturnAll() {
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of());

        assertEquals(25, matches.size());
        for (int i = 0; i < 25; i++) {
            assertTrue(matches.contains(i));
        }
    }

    @Test
    @DisplayName("Великий набір даних: паралельна обробка")
    void testLargeDataSet_ParallelProcessing() {
        List<Object> largeNumeric = new ArrayList<>();
        for (int i = 0; i < 150000; i++) largeNumeric.add((double) (i % 1000));

        List<DataColumn> cols = List.of(DataColumn.builder()
                .name("large_col")
                .type(ColumnType.NUMERIC)
                .values(largeNumeric)
                .build());

        DataSet largeSet = DataSet.builder()
                .id("large-ds")
                .name("Large Dataset")
                .columns(cols)
                .build();

        FilterCriteria criteria = FilterCriteria.numericRange("large_col", 100.0, 200.0);
        long startTime = System.currentTimeMillis();
        List<Integer> matches = FilterEngine.apply(largeSet, List.of(criteria));
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(matches.size() > 0);
        assertTrue(duration < 5000, "Паралельна обробка повинна завершитися < 5 сек");
    }

    @Test
    @DisplayName("Невідома колона: IllegalArgumentException")
    void testUnknownColumn_ThrowsException() {
        FilterCriteria criteria = FilterCriteria.numericRange("unknown_column", 1.0, 10.0);
        
        assertThrows(IllegalArgumentException.class, 
                () -> FilterEngine.apply(testDataSet, List.of(criteria)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1000.0, 0.0, 1.0, 25.0, 1000.0})
    @DisplayName("Числовий діапазон: параметризовані граничні值")
    void testNumericRangeParameterized(double value) {
        FilterCriteria criteria = FilterCriteria.numericRange("temperature", value, value);
        List<Integer> matches = FilterEngine.apply(testDataSet, List.of(criteria));

        if (value >= 1.0 && value <= 25.0) {
            assertEquals(1, matches.size());
        } else {
            assertTrue(matches.isEmpty());
        }
    }
}
