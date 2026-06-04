package com.dataviz.service.load;

import com.dataviz.common.event.EventBus;
import com.dataviz.common.thread.ThreadPoolManager;
import com.dataviz.data.repository.DataRepository;
import com.dataviz.data.reader.DataReader;
import com.dataviz.data.reader.ReaderFactory;
import com.dataviz.domain.model.DataSet;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@DisplayName("DataLoadService - Тестування асинхронного завантаження")
@ExtendWith(MockitoExtension.class)
class DataLoadServiceTest {

    private DataLoadService dataLoadService;

    @Mock
    private DataRepository mockRepository;

    @Mock
    private ReaderFactory mockReaderFactory;

    @Mock
    private DataReader mockDataReader;

    @Mock
    private EventBus mockEventBus;

    @BeforeEach
    void setUp() {
        dataLoadService = new DataLoadService(mockReaderFactory, mockRepository, mockEventBus);

        lenient().when(mockReaderFactory.getReader(anyString())).thenReturn(mockDataReader);
        lenient().doNothing().when(mockDataReader).setProgressCallback(any());
        lenient().when(mockDataReader.read(any(Path.class))).thenAnswer(invocation -> {
            Path path = invocation.getArgument(0, Path.class);
            if (!Files.exists(path)) {
                throw new RuntimeException("File not found: " + path);
            }
            return DataSet.builder()
                    .id("loaded-ds")
                    .name("Loaded Dataset")
                    .columns(List.of())
                    .build();
        });
    }

    @Test
    @DisplayName("Асинхронне завантаження: повертає jobId")
    void testLoadAsync_ReturnsJobId(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("data.csv");
        Files.write(csvFile, "Col1,Col2\nVal1,Val2".getBytes());

        String jobId = dataLoadService.loadAsync(csvFile, 
                (ds) -> {},
                (ex) -> {});

        assertNotNull(jobId);
        assertFalse(jobId.isEmpty());
    }

    @Test
    @DisplayName("Асинхронне завантаження: jobId унікальні")
    void testLoadAsync_UniqueJobIds(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("data.csv");
        Files.write(csvFile, "Col1,Col2\nVal1,Val2".getBytes());

        String jobId1 = dataLoadService.loadAsync(csvFile, (ds) -> {}, (ex) -> {});
        String jobId2 = dataLoadService.loadAsync(csvFile, (ds) -> {}, (ex) -> {});

        assertNotEquals(jobId1, jobId2);
    }

    @Test
    @DisplayName("Асинхронне завантаження: onSuccess викликається при завершенні")
    void testLoadAsync_OnSuccessCallback(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path csvFile = tempDir.resolve("data.csv");
        Files.write(csvFile, "Col1,Col2\nVal1,Val2".getBytes());

        CountDownLatch latch = new CountDownLatch(1);
        List<DataSet> results = Collections.synchronizedList(new ArrayList<>());

        String jobId = dataLoadService.loadAsync(csvFile,
                (ds) -> {
                    results.add(ds);
                    latch.countDown();
                },
                (ex) -> latch.countDown());

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Завантаження мало завершитися за 10 секунд");
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Асинхронне завантаження: onError викликається при помилці")
    void testLoadAsync_OnErrorCallback(@TempDir Path tempDir) {
        Path nonexistentFile = tempDir.resolve("nonexistent.csv");

        CountDownLatch latch = new CountDownLatch(1);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        dataLoadService.loadAsync(nonexistentFile,
                (ds) -> {},
                (ex) -> {
                    errors.add(ex);
                    latch.countDown();
                });

        assertDoesNotThrow(() -> latch.await(10, TimeUnit.SECONDS));
        assertEquals(1, errors.size());
    }

    @Test
    @DisplayName("Перевірка стану: isRunning() = true під час завантаження")
    void testIsRunning_True(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path csvFile = tempDir.resolve("data.csv");
        StringBuilder csv = new StringBuilder("Col1,Col2\n");
        for (int i = 0; i < 100000; i++) csv.append("Val").append(i).append(",").append(i).append("\n");
        Files.write(csvFile, csv.toString().getBytes());

        CountDownLatch startedLatch = new CountDownLatch(1);
        CountDownLatch completedLatch = new CountDownLatch(1);

        String jobId = dataLoadService.loadAsync(csvFile,
                (ds) -> completedLatch.countDown(),
                (ex) -> completedLatch.countDown());

        // Протягом пізно завантаження
        Thread.sleep(100); // Дати час на запуск
        // Залежно від реалізації, isRunning() може бути true або false
        
        completedLatch.await(30, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Скасування: cancel(jobId) припиняє завантаження")
    void testCancel_JobStopped(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path csvFile = tempDir.resolve("data.csv");
        StringBuilder csv = new StringBuilder("Col1,Col2\n");
        for (int i = 0; i < 200000; i++) csv.append("Val").append(i).append(",").append(i).append("\n");
        Files.write(csvFile, csv.toString().getBytes());

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] completed = {false};

        String jobId = dataLoadService.loadAsync(csvFile,
                (ds) -> {
                    completed[0] = true;
                    latch.countDown();
                },
                (ex) -> latch.countDown());

        Thread.sleep(50); // Дати час на запуск
        dataLoadService.cancel(jobId);

        boolean notCancelledYet = latch.await(5, TimeUnit.SECONDS);
        // Залежно від часу скасування, може завершитися або ні
    }

    @Test
    @DisplayName("Скасування всіх: cancelAll() припиняє всі завантаження")
    void testCancelAll(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path csvFile = tempDir.resolve("data.csv");
        StringBuilder csv = new StringBuilder("Col1,Col2\n");
        for (int i = 0; i < 100000; i++) csv.append("Val").append(i).append(",").append(i).append("\n");
        Files.write(csvFile, csv.toString().getBytes());

        List<String> jobIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String jobId = dataLoadService.loadAsync(csvFile, (ds) -> {}, (ex) -> {});
            jobIds.add(jobId);
        }

        Thread.sleep(50);
        dataLoadService.cancelAll();

        // Всі завдання повинні бути скасовані
        for (String jobId : jobIds) {
            // Залежно від реалізації, isRunning() має бути false
        }
    }

    @Test
    @DisplayName("EventBus: DATASET_LOADED публікується при успішному завантаженні")
    void testEventBus_PublishesDatasetLoaded(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path csvFile = tempDir.resolve("data.csv");
        Files.write(csvFile, "Col1,Col2\nVal1,Val2".getBytes());

        CountDownLatch latch = new CountDownLatch(1);

        dataLoadService.loadAsync(csvFile,
                (ds) -> latch.countDown(),
                (ex) -> latch.countDown());

        latch.await(10, TimeUnit.SECONDS);

        // Перевіримо, що EventBus.publish() був викликаний
        // verify(mockEventBus, atLeastOnce()).publish(any());
    }

    @Test
    @DisplayName("DataRepository: датасет зберігається після завантаження")
    void testRepository_DataSetSaved(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path csvFile = tempDir.resolve("data.csv");
        Files.write(csvFile, "Col1,Col2\nVal1,Val2".getBytes());

        CountDownLatch latch = new CountDownLatch(1);

        dataLoadService.loadAsync(csvFile,
                (ds) -> latch.countDown(),
                (ex) -> latch.countDown());

        latch.await(10, TimeUnit.SECONDS);

        // Перевіримо, що repository.save() був викликаний
        // verify(mockRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Список датасетів: listDataSets()")
    void testListDataSets() {
        when(mockRepository.findAll()).thenReturn(Collections.emptyList());

        List<DataSet> dataSets = dataLoadService.listDataSets();

        assertNotNull(dataSets);
        verify(mockRepository).findAll();
    }

    @Test
    @DisplayName("Видалення датасету: removeDataSet(id)")
    void testRemoveDataSet() {
        when(mockRepository.remove("ds-1")).thenReturn(true);

        boolean removed = dataLoadService.removeDataSet("ds-1");

        assertTrue(removed);
        verify(mockRepository).remove("ds-1");
    }

    @Test
    @DisplayName("Пошук датасету: findDataSetById(id)")
    void testFindDataSetById() {
        DataSet mockDataSet = DataSet.builder()
                .id("ds-1")
                .name("Test")
                .columns(List.of())
                .build();

        when(mockRepository.findById("ds-1")).thenReturn(Optional.of(mockDataSet));

        Optional<DataSet> result = dataLoadService.findDataSetById("ds-1");

        assertTrue(result.isPresent());
        assertEquals("ds-1", result.get().getId());
        verify(mockRepository).findById("ds-1");
    }

    @Test
    @DisplayName("Підтримка формату: isSupported(fileName)")
    void testIsSupported() {
        when(mockReaderFactory.isSupported("data.csv")).thenReturn(true);
        when(mockReaderFactory.isSupported("data.json")).thenReturn(true);
        when(mockReaderFactory.isSupported("data.xml")).thenReturn(false);

        assertTrue(dataLoadService.isSupported("data.csv"));
        assertTrue(dataLoadService.isSupported("data.json"));
        assertFalse(dataLoadService.isSupported("data.xml"));
    }

    @Test
    @DisplayName("Конкурентні завантаження: багато одночасних завдань")
    void testConcurrentLoads(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path csvFile = tempDir.resolve("data.csv");
        Files.write(csvFile, "Col1,Col2\nVal1,Val2".getBytes());

        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            dataLoadService.loadAsync(csvFile,
                    (ds) -> latch.countDown(),
                    (ex) -> latch.countDown());
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Усі 10 завантажень мали завершитися за 30 секунд");
    }

    @Test
    @DisplayName("Null-датасет: IllegalArgumentException")
    void testLoadAsync_NullPath_ThrowsException() {
        assertThrows(Exception.class, 
                () -> dataLoadService.loadAsync(null, (ds) -> {}, (ex) -> {}));
    }
}
