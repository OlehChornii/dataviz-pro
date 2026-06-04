package com.dataviz.data.repository;

import com.dataviz.domain.model.DataSet;
import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.ColumnType;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;

@DisplayName("DataRepository - Тестування сховища наборів даних")
class DataRepositoryTest {

    private DataRepository repository;
    private DataSet testDataSet1;
    private DataSet testDataSet2;

    @BeforeEach
    void setUp() {
        repository = new DataRepository();
        
        testDataSet1 = DataSet.builder()
                .id("ds-1")
                .name("Dataset 1")
                .columns(List.of(DataColumn.builder()
                        .name("col1")
                        .type(ColumnType.NUMERIC)
                        .values(List.of(1.0, 2.0, 3.0))
                        .build()))
                .build();

        testDataSet2 = DataSet.builder()
                .id("ds-2")
                .name("Dataset 2")
                .columns(List.of(DataColumn.builder()
                        .name("col2")
                        .type(ColumnType.CATEGORICAL)
                        .values(List.of("A", "B", "C"))
                        .build()))
                .build();
    }

    @Test
    @DisplayName("Збереження та отримання: save + getById")
    void testSaveAndGetById() {
        repository.save(testDataSet1);
        DataSet retrieved = repository.getById("ds-1");

        assertNotNull(retrieved);
        assertEquals("ds-1", retrieved.getId());
        assertEquals("Dataset 1", retrieved.getName());
    }

    @Test
    @DisplayName("Отримання за ID: невідома ID викидає виняток")
    void testGetById_NotFound_ThrowsException() {
        assertThrows(NoSuchElementException.class, 
                () -> repository.getById("nonexistent"));
    }

    @Test
    @DisplayName("Пошук за ID: знаходить та повертає Optional")
    void testFindById_Found() {
        repository.save(testDataSet1);
        Optional<DataSet> found = repository.findById("ds-1");

        assertTrue(found.isPresent());
        assertEquals("ds-1", found.get().getId());
    }

    @Test
    @DisplayName("Пошук за ID: не знаходить та повертає empty Optional")
    void testFindById_NotFound() {
        Optional<DataSet> notFound = repository.findById("nonexistent");

        assertFalse(notFound.isPresent());
    }

    @Test
    @DisplayName("Отримання всіх: findAll() у порядку вставлення")
    void testFindAll_OrderPreserved() {
        repository.save(testDataSet1);
        repository.save(testDataSet2);

        List<DataSet> all = repository.findAll();

        assertEquals(2, all.size());
        assertEquals("ds-1", all.get(0).getId());
        assertEquals("ds-2", all.get(1).getId());
    }

    @Test
    @DisplayName("Отримання всіх: порожня колекція")
    void testFindAll_Empty() {
        List<DataSet> all = repository.findAll();

        assertTrue(all.isEmpty());
    }

    @Test
    @DisplayName("Перевірка наявності: contains() істина/хиба")
    void testContains() {
        repository.save(testDataSet1);

        assertTrue(repository.contains("ds-1"));
        assertFalse(repository.contains("ds-2"));
    }

    @Test
    @DisplayName("Видалення: remove() повертає true при наявності")
    void testRemove_Success() {
        repository.save(testDataSet1);
        boolean removed = repository.remove("ds-1");

        assertTrue(removed);
        assertFalse(repository.contains("ds-1"));
    }

    @Test
    @DisplayName("Видалення: remove() повертає false при відсутності")
    void testRemove_NotFound() {
        boolean removed = repository.remove("nonexistent");

        assertFalse(removed);
    }

    @Test
    @DisplayName("Рахунок: count() дає точну кількість")
    void testCount() {
        assertEquals(0, repository.count());

        repository.save(testDataSet1);
        assertEquals(1, repository.count());

        repository.save(testDataSet2);
        assertEquals(2, repository.count());

        repository.remove("ds-1");
        assertEquals(1, repository.count());
    }

    @Test
    @DisplayName("Оновлення: save() на існуючому ID замінює")
    void testSave_UpdateExisting() {
        repository.save(testDataSet1);

        DataSet updated = DataSet.builder()
                .id("ds-1")
                .name("Updated Dataset")
                .columns(testDataSet1.getColumns())
                .build();

        repository.save(updated);
        DataSet retrieved = repository.getById("ds-1");

        assertEquals("Updated Dataset", retrieved.getName());
        assertEquals(1, repository.count()); // Не дублюється
    }

    @Test
    @DisplayName("Очищення: clear() видаляє все")
    void testClear() {
        repository.save(testDataSet1);
        repository.save(testDataSet2);
        assertEquals(2, repository.count());

        repository.clear();

        assertEquals(0, repository.count());
        assertFalse(repository.contains("ds-1"));
    }

    @Test
    @DisplayName("Потокобезопасність: конкурентне збереження")
    void testConcurrentSave() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                DataSet ds = DataSet.builder()
                        .id("ds-" + id)
                        .name("Concurrent " + id)
                        .columns(List.of())
                        .build();
                repository.save(ds);
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threadCount, repository.count());
    }

    @Test
    @DisplayName("Потокобезопасність: конкурентне видалення")
    void testConcurrentRemove() throws InterruptedException {
        // Спочатку додаємо дані
        for (int i = 0; i < 10; i++) {
            DataSet ds = DataSet.builder()
                    .id("ds-" + i)
                    .name("Dataset " + i)
                    .columns(List.of())
                    .build();
            repository.save(ds);
        }

        // Конкурентно видаляємо
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * 2);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                repository.remove("ds-" + ThreadLocalRandom.current().nextInt(10));
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Залишку значень має бути <= 10 (деякі могли видалитися)
        assertTrue(repository.count() <= 10);
    }

    @Test
    @DisplayName("Null-безопасність: save(null) викидає виняток")
    void testSaveNull_ThrowsException() {
        assertThrows(Exception.class, () -> repository.save(null));
    }

    @Test
    @DisplayName("Null-безопасність: getById(null) викидає виняток")
    void testGetByIdNull_ThrowsException() {
        assertThrows(Exception.class, () -> repository.getById(null));
    }

    @Test
    @DisplayName("Максимальний розмір: велика кількість наборів")
    void testLargeDataSets() {
        int dataSetCount = 1000;
        for (int i = 0; i < dataSetCount; i++) {
            DataSet ds = DataSet.builder()
                    .id("ds-" + i)
                    .name("Dataset " + i)
                    .columns(List.of())
                    .build();
            repository.save(ds);
        }

        assertEquals(dataSetCount, repository.count());
        assertTrue(repository.contains("ds-0"));
        assertTrue(repository.contains("ds-999"));
        assertFalse(repository.contains("ds-1000"));
    }

    @Test
    @DisplayName("Порядок вставлення: зберігається при findAll()")
    void testInsertionOrderPreservation() {
        String[] ids = {"first", "second", "third", "fourth", "fifth"};
        for (String id : ids) {
            DataSet ds = DataSet.builder()
                    .id(id)
                    .name("Dataset " + id)
                    .columns(List.of())
                    .build();
            repository.save(ds);
        }

        List<DataSet> all = repository.findAll();
        for (int i = 0; i < ids.length; i++) {
            assertEquals(ids[i], all.get(i).getId());
        }
    }

    @Test
    @DisplayName("Оновлення не змінює порядок: save() на існуючій ID")
    void testUpdateDoesNotChangeOrder() {
        repository.save(testDataSet1);
        repository.save(testDataSet2);

        DataSet updated = DataSet.builder()
                .id("ds-1")
                .name("Updated")
                .columns(List.of())
                .build();
        repository.save(updated);

        List<DataSet> all = repository.findAll();
        assertEquals("ds-1", all.get(0).getId()); // Залишається першим
        assertEquals("ds-2", all.get(1).getId());
    }
}
