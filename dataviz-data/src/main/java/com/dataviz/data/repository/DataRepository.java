package com.dataviz.data.repository;

import com.dataviz.domain.model.DataSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import com.dataviz.di.annotation.*;

@Repository
@Singleton
public final class DataRepository {

    private static final Logger LOG = Logger.getLogger(DataRepository.class.getName());

    private final Map<String, DataSet> store = new ConcurrentHashMap<>();

    private final List<String> loadOrder = Collections.synchronizedList(new ArrayList<>());

    public void save(DataSet dataSet) {
        Objects.requireNonNull(dataSet, "dataSet must not be null");
        boolean isNew = !store.containsKey(dataSet.getId());
        store.put(dataSet.getId(), dataSet);
        if (isNew) loadOrder.add(dataSet.getId());
        LOG.fine(() -> "Saved DataSet: %s (total: %d)".formatted(dataSet.getId(), store.size()));
    }

    public DataSet getById(String id) {
        DataSet ds = store.get(Objects.requireNonNull(id));
        if (ds == null) throw new NoSuchElementException("DataSet not found: " + id);
        return ds;
    }

    public Optional<DataSet> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<DataSet> findAll() {
        return loadOrder.stream()
                .map(store::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public boolean remove(String id) {
        boolean removed = store.remove(id) != null;
        loadOrder.remove(id);
        if (removed) LOG.fine(() -> "Removed DataSet: " + id);
        return removed;
    }

    public void clear() {
        store.clear();
        loadOrder.clear();
        LOG.info("DataRepository cleared");
    }

    public int count() { return store.size(); }

    public boolean contains(String id) { return store.containsKey(id); }
}