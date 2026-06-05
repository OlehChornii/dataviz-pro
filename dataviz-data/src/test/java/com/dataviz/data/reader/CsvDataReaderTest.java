package com.dataviz.data.reader;

import com.dataviz.domain.model.DataSet;
import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.ColumnType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@DisplayName("CsvDataReader - Тестування читання CSV файлів")
class CsvDataReaderTest {

    private CsvDataReader reader;

    @BeforeEach
    void setUp() {
        reader = new CsvDataReader();
    }

    @Test
    @DisplayName("Підтримка форматів: розпізнає .csv")
    void testSupports_CSV() {
        assertTrue(reader.supports("data.csv"));
        assertTrue(reader.supports("DATA.CSV"));
    }

    @Test
    @DisplayName("Підтримка форматів: розпізнає .tsv")
    void testSupports_TSV() {
        assertTrue(reader.supports("data.tsv"));
        assertTrue(reader.supports("DATA.TSV"));
    }

    @Test
    @DisplayName("Підтримка форматів: не розпізнає інші розширення")
    void testSupports_Other() {
        assertFalse(reader.supports("data.json"));
        assertFalse(reader.supports("data.xml"));
        assertFalse(reader.supports("data.txt"));
    }

    @Test
    @DisplayName("Читання CSV: базовий формат з заголовком")
    void testRead_BasicCSV(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("data.csv");
        Files.writeString(csvFile, 
                "Name,Age,Salary\n" +
                "Alice,30,50000\n" +
                "Bob,25,40000\n" +
                "Charlie,35,60000", StandardCharsets.UTF_8);

        DataSet result = reader.read(csvFile);

        assertEquals(3, result.getRowCount());
        assertEquals(3, result.getColumnCount());
        assertTrue(result.hasColumn("Name"));
        assertTrue(result.hasColumn("Age"));
        assertTrue(result.hasColumn("Salary"));
    }

    @Test
    @DisplayName("Читання CSV: TSV формат (tab-separated)")
    void testRead_TSVFormat(@TempDir Path tempDir) throws IOException {
        Path tsvFile = tempDir.resolve("data.tsv");
        Files.writeString(tsvFile, 
                "Name\tAge\tCity\n" +
                "Alice\t30\tKyiv\n" +
                "Bob\t25\tLviv", StandardCharsets.UTF_8);

        DataSet result = reader.read(tsvFile);

        assertEquals(2, result.getRowCount());
        assertEquals(3, result.getColumnCount());
    }

    @Test
    @DisplayName("Читання CSV: пустий файл (тільки заголовок)")
    void testRead_EmptyCSV(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("empty.csv");
        Files.writeString(csvFile, "Name,Age,City", StandardCharsets.UTF_8);

        DataSet result = reader.read(csvFile);

        assertEquals(0, result.getRowCount());
        assertEquals(3, result.getColumnCount());
    }

    @Test
    @DisplayName("Читання CSV: спеціальні символи в лапках")
    void testRead_QuotedValues(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("quoted.csv");
        Files.writeString(csvFile, 
                "Name,Description\n" +
                "\"Smith, John\",\"Works at, LLC\"\n" +
                "Jane,\"Has \"\"quotes\"\" inside\"", StandardCharsets.UTF_8);

        DataSet result = reader.read(csvFile);

        assertEquals(2, result.getRowCount());
        assertEquals(2, result.getColumnCount());
    }

    @Test
    @DisplayName("Читання CSV: вмісту в одинарних лапках")
    void testRead_SingleQuotes(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("single.csv");
        Files.writeString(csvFile, 
                "Field1,Field2\n" +
                "'Value1','Value2'\n" +
                "'Value3','Value4'", StandardCharsets.UTF_8);

        DataSet result = reader.read(csvFile);

        assertEquals(2, result.getRowCount());
    }

    @Test
    @DisplayName("Читання CSV: Кирилиця (UTF-8)")
    void testRead_Cyrillic(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("cyrillic.csv");
        Files.writeString(csvFile, 
                "Ім'я,Місто,Посада\n" +
                "Іван,Київ,Інженер\n" +
                "Ольга,Львів,Менеджер\n" +
                "Марія,Одеса,Аналітик", StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        DataSet result = reader.read(csvFile);

        assertEquals(3, result.getRowCount());
        assertEquals(3, result.getColumnCount());
        assertTrue(result.hasColumn("Ім'я"));
    }

    @Test
    @DisplayName("Читання CSV: прогрес-колбек (progress 0.0 -> 1.0)")
    void testRead_ProgressCallback(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("large.csv");
        StringBuilder csv = new StringBuilder("Name,Value\n");
        for (int i = 0; i < 25000; i++) {
            csv.append("Item").append(i).append(",").append(i * 10).append("\n");
        }
        Files.writeString(csvFile, csv.toString(), StandardCharsets.UTF_8);

        AtomicReference<Double> maxProgress = new AtomicReference<>(0.0);
        List<Double> progressValues = Collections.synchronizedList(new ArrayList<>());

        reader.setProgressCallback(progress -> {
            progressValues.add(progress);
            maxProgress.accumulateAndGet(progress, Math::max);
        });

        DataSet result = reader.read(csvFile);

        assertTrue(maxProgress.get() >= 0.9, "Progress мав досягти ~1.0, отримано: " + maxProgress.get());
        assertTrue(progressValues.size() > 0, "Прогрес мав бути повідомлений");

        assertEquals(25000, result.getRowCount());
    }

    @Test
    @DisplayName("Читання CSV: усі колони спочатку CATEGORICAL")
    void testRead_AllColumnsCategorical(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("mixed.csv");
        Files.writeString(csvFile, 
                "Name,Count,Active\n" +
                "Alice,100,true\n" +
                "Bob,200,false", StandardCharsets.UTF_8);

        DataSet result = reader.read(csvFile);

        DataColumn nameCol = result.getColumn("Name");
        DataColumn countCol = result.getColumn("Count");
        DataColumn activeCol = result.getColumn("Active");

        assertEquals(ColumnType.CATEGORICAL, nameCol.getType());
        assertEquals(ColumnType.CATEGORICAL, countCol.getType());
        assertEquals(ColumnType.CATEGORICAL, activeCol.getType());
    }

    @Test
    @DisplayName("Читання CSV: невидалений файл")
    void testRead_FileNotFound() {
        Path nonexistent = Paths.get("/nonexistent/path/file.csv");

        assertThrows(Exception.class, () -> reader.read(nonexistent));
    }

    @Test
    @DisplayName("Читання CSV: розколена лінія (незбалансована кількість полів)")
    void testRead_MalformedCSV(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("malformed.csv");
        Files.writeString(csvFile, 
                "Col1,Col2,Col3\n" +
                "Val1,Val2\n" +
                "Val3,Val4,Val5,Val6", StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> reader.read(csvFile));
    }

    @Test
    @DisplayName("Читання CSV: комі у значеннях (в лапках)")
    void testRead_CommaInValues(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("comma.csv");
        Files.writeString(csvFile, 
                "Name,Address\n" +
                "\"Smith, John\",\"123 Main St, City\"\n" +
                "\"Doe, Jane\",\"456 Oak Ave, Town\"", StandardCharsets.UTF_8);

        DataSet result = reader.read(csvFile);

        assertEquals(2, result.getRowCount());
    }

    @Test
    @DisplayName("Читання CSV: новий рядок у значеннях (в лапках)")
    void testRead_NewlineInValues(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("newline.csv");
        Files.writeString(csvFile, 
                "Name,Description\n" +
                "\"Product A\",\"Line 1\nLine 2\"\n" +
                "\"Product B\",\"Single line\"", StandardCharsets.UTF_8);

        DataSet result = reader.read(csvFile);

        assertEquals(2, result.getRowCount());
    }

    @Test
    @DisplayName("Читання CSV: пустих значень (порожні поля)")
    void testRead_EmptyFields(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("empty_fields.csv");
        Files.writeString(csvFile, 
                "Name,Email,Phone\n" +
                "Alice,,555-1234\n" +
                "Bob,bob@example.com,\n" +
                ",,,\n" +
                "Charlie,charlie@example.com,555-5678", StandardCharsets.UTF_8);

        DataSet result = reader.read(csvFile);

        assertEquals(4, result.getRowCount());
    }

    @Test
    @DisplayName("Читання CSV: різні розділювачі (за бажанням комі/точка/крапка з комою)")
    void testRead_CommaSeparated(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("comma_sep.csv");
        Files.writeString(csvFile, "A,B,C\n1,2,3\n4,5,6", StandardCharsets.UTF_8);

        DataSet result = reader.read(csvFile);

        assertEquals(2, result.getRowCount());
        assertEquals(3, result.getColumnCount());
    }

    @Test
    @DisplayName("Читання CSV: BOM (Byte Order Mark) обробляється")
    void testRead_BOM(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("bom.csv");
        byte[] content = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] data = "Name,Value\nAlice,100".getBytes();
        byte[] combined = new byte[content.length + data.length];
        System.arraycopy(content, 0, combined, 0, content.length);
        System.arraycopy(data, 0, combined, content.length, data.length);

        Files.write(csvFile, combined);

        DataSet result = reader.read(csvFile);
        assertEquals(1, result.getRowCount());
    }

    @Test
    @DisplayName("Читання CSV: ID датасету генерується")
    void testRead_DataSetIdGenerated(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test.csv");
        Files.write(csvFile, "Col1,Col2\nVal1,Val2".getBytes());

        DataSet result = reader.read(csvFile);

        assertNotNull(result.getId());
        assertFalse(result.getId().isEmpty());
    }

    @Test
    @DisplayName("Читання CSV: ім'я датасету від імені файла")
    void testRead_DataSetNameFromFileName(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("my_data.csv");
        Files.write(csvFile, "Col1,Col2\nVal1,Val2".getBytes());

        DataSet result = reader.read(csvFile);

        assertNotNull(result.getName());
    }
}
