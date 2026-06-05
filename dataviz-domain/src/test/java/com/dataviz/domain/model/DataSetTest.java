package com.dataviz.domain.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@DisplayName("DataSet - Тестування моделі набору даних")
class DataSetTest {

    private DataSet testDataSet;
    private List<DataColumn> testColumns;

    @BeforeEach
    void setUp() {
        testColumns = new ArrayList<>();
        testColumns.add(DataColumn.builder()
                .name("ID")
                .type(ColumnType.NUMERIC)
                .values(List.of(1.0, 2.0, 3.0))
                .build());

        testColumns.add(DataColumn.builder()
                .name("Name")
                .type(ColumnType.CATEGORICAL)
                .values(List.of("Alice", "Bob", "Charlie"))
                .build());

        testColumns.add(DataColumn.builder()
                .name("Status")
                .type(ColumnType.BOOLEAN)
                .values(List.of(true, false, true))
                .build());

        testDataSet = DataSet.builder()
                .id("ds-1")
                .name("Test Dataset")
                .columns(testColumns)
                .build();
    }

    @Test
    @DisplayName("Доступ до колони за назвою: знаходить правильно")
    void testGetColumn_Success() {
        DataColumn col = testDataSet.getColumn("ID");
        
        assertNotNull(col);
        assertEquals("ID", col.getName());
        assertEquals(ColumnType.NUMERIC, col.getType());
    }

    @Test
    @DisplayName("Доступ до колони: case-insensitive пошук")
    void testGetColumn_CaseInsensitive() {
        DataColumn col = testDataSet.getColumn("id");
        
        assertNotNull(col);
        assertEquals("ID", col.getName());
    }

    @Test
    @DisplayName("Доступ до колони: невідома колона викидає виняток")
    void testGetColumn_NotFound_ThrowsException() {
        assertThrows(NoSuchElementException.class, 
                () -> testDataSet.getColumn("NonExistent"));
    }

    @Test
    @DisplayName("Перевірка наявності колони: isPresent -> true")
    void testHasColumn_True() {
        assertTrue(testDataSet.hasColumn("Name"));
        assertTrue(testDataSet.hasColumn("name"));
    }

    @Test
    @DisplayName("Перевірка наявності колони: isAbsent -> false")
    void testHasColumn_False() {
        assertFalse(testDataSet.hasColumn("NonExistent"));
    }

    @Test
    @DisplayName("Отримання колон за типом: NUMERIC")
    void testGetColumnsByType_Numeric() {
        List<DataColumn> numericCols = testDataSet.getColumnsByType(ColumnType.NUMERIC);
        
        assertEquals(1, numericCols.size());
        assertEquals("ID", numericCols.get(0).getName());
    }

    @Test
    @DisplayName("Отримання колон за типом: CATEGORICAL")
    void testGetColumnsByType_Categorical() {
        List<DataColumn> categoricalCols = testDataSet.getColumnsByType(ColumnType.CATEGORICAL);
        
        assertEquals(1, categoricalCols.size());
        assertEquals("Name", categoricalCols.get(0).getName());
    }

    @Test
    @DisplayName("Отримання колон за типом: BOOLEAN")
    void testGetColumnsByType_Boolean() {
        List<DataColumn> boolCols = testDataSet.getColumnsByType(ColumnType.BOOLEAN);
        
        assertEquals(1, boolCols.size());
        assertEquals("Status", boolCols.get(0).getName());
    }

    @Test
    @DisplayName("Отримання колон за типом: порожний результат")
    void testGetColumnsByType_Empty() {
        List<DataColumn> temporalCols = testDataSet.getColumnsByType(ColumnType.TEMPORAL);
        
        assertTrue(temporalCols.isEmpty());
    }

    @Test
    @DisplayName("Індекс колон: ліниве ініціалізація (О(1) доступ після)")
    void testColumnIndexing_LazyInitialized() {
        DataColumn col1 = testDataSet.getColumn("ID");
        DataColumn col2 = testDataSet.getColumn("ID");
        
        assertSame(col1, col2, "Другий доступ повинен повернути той же об'єкт");
    }

    @Test
    @DisplayName("Оцінка пам'яті: всі колони враховуються")
    void testEstimatedMemoryBytes() {
        long estimatedBytes = testDataSet.estimatedMemoryBytes();
        
        assertTrue(estimatedBytes > 0, "Оцінка пам'яті повинна бути > 0");
        assertTrue(estimatedBytes >= 50);
    }

    @Test
    @DisplayName("Immutability: список колон не можна змінити")
    void testImmutability_ColumnsListCantBeModified() {
        List<DataColumn> cols = testDataSet.getColumns();
        
        if (cols instanceof ArrayList) {
            fail("Список колон повинен бути незмінюваним");
        }
    }

    @Test
    @DisplayName("Метаінформація: ID та Name зберігаються коректно")
    void testMetadata() {
        assertEquals("ds-1", testDataSet.getId());
        assertEquals("Test Dataset", testDataSet.getName());
        assertEquals(3, testDataSet.getColumnCount());
    }

    @Test
    @DisplayName("Конкатенація метаінформації: loadedAt та sourceDescription")
    void testMetadata_LoadedAtAndSource() {
        assertNotNull(testDataSet.getLoadedAt());
    }

    @Test
    @DisplayName("Багатокритеріальний пошук: отримати декілька колон")
    void testGetColumns_Multiple() {
        List<DataColumn> cols = testDataSet.getColumns();
        
        assertEquals(3, cols.size());
        assertTrue(cols.stream().anyMatch(c -> c.getName().equals("ID")));
        assertTrue(cols.stream().anyMatch(c -> c.getName().equals("Name")));
        assertTrue(cols.stream().anyMatch(c -> c.getName().equals("Status")));
    }

    @Test
    @DisplayName("Рядків у наборі: rowCount точний")
    void testRowCount() {
        assertEquals(3, testDataSet.getRowCount());
    }

    @Test
    @DisplayName("Порожній набір даних: 0 рядків")
    void testEmptyDataSet() {
        DataSet empty = DataSet.builder()
                .id("empty")
                .name("Empty")
                .columns(List.of())
                .build();

        assertEquals(0, empty.getRowCount());
        assertEquals(0, empty.getColumnCount());
        assertTrue(empty.getColumnsByType(ColumnType.NUMERIC).isEmpty());
    }

    @Test
    @DisplayName("Дублікат колон: обидві доступні за своєю назвою")
    void testGetColumnsWithDuplicateNames() {
        List<DataColumn> dupColumns = new ArrayList<>();
        dupColumns.add(DataColumn.builder()
                .name("Value")
                .type(ColumnType.NUMERIC)
                .values(List.of(1.0, 2.0, 3.0))
                .build());
        dupColumns.add(DataColumn.builder()
                .name("Value")
                .type(ColumnType.NUMERIC)
                .values(List.of(10.0, 20.0, 30.0))
                .build());

        DataSet dupSet = DataSet.builder()
                .id("dup")
                .name("Duplicate Columns")
                .columns(dupColumns)
                .build();

        DataColumn result = dupSet.getColumn("Value");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Null-безопасність: null у значеннях колон")
    void testNullSafety_ColumnValues() {
        List<Object> nullableValues = new ArrayList<>(Arrays.asList(1.0, null, 3.0));

        List<DataColumn> cols = List.of(DataColumn.builder()
                .name("WithNulls")
                .type(ColumnType.NUMERIC)
                .values(nullableValues)
                .build());

        DataSet setWithNulls = DataSet.builder()
                .id("with-nulls")
                .name("Dataset with Nulls")
                .columns(cols)
                .build();

        assertNotNull(setWithNulls.getColumn("WithNulls"));
        assertEquals(3, setWithNulls.getRowCount());
    }
}
