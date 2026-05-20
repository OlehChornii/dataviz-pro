package com.dataviz.service.project;

import com.dataviz.di.annotation.*;

import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

@Service
@Singleton
public final class ProjectService {

    private static final Logger LOG         = Logger.getLogger(ProjectService.class.getName());
    private static final int    MAX_RECENT  = 10;
    private static final String RECENT_KEY  = "recent.project.";
    private static final String EXT         = ".dvp";

    private final Preferences prefs = Preferences.userNodeForPackage(ProjectService.class);

    @Inject
    public ProjectService() {}

    public void save(ProjectState state, Path path) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(path, "path must not be null");

        Path target = ensureExtension(path);
        LOG.info(() -> "Saving project to: " + target);

        try {
            String xml = state.toXml();
            Files.writeString(target, xml);
            addToRecent(target);
            LOG.info("Project saved successfully");
        } catch (Exception e) {
            throw new ProjectException("Failed to save project: " + e.getMessage(), e);
        }
    }

    public ProjectState open(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        LOG.info(() -> "Opening project from: " + path);

        if (!Files.exists(path)) {
            throw new ProjectException("Project file not found: " + path);
        }

        try {
            String xml = Files.readString(path);
            ProjectState state = ProjectState.fromXml(xml);
            addToRecent(path);
            return state;
        } catch (Exception e) {
            throw new ProjectException("Failed to open project: " + e.getMessage(), e);
        }
    }

    public List<Path> getRecentProjects() {
        List<Path> recent = new ArrayList<>();
        for (int i = 0; i < MAX_RECENT; i++) {
            String val = prefs.get(RECENT_KEY + i, null);
            if (val != null) recent.add(Path.of(val));
        }
        return Collections.unmodifiableList(recent);
    }

    public void cleanRecentProjects() {
        getRecentProjects().stream()
                .filter(p -> !Files.exists(p))
                .forEach(p -> removeFromRecent(p));
    }

    private void addToRecent(Path path) {
        List<Path> current = new ArrayList<>(getRecentProjects());
        current.remove(path);
        current.add(0, path);
        if (current.size() > MAX_RECENT) current = current.subList(0, MAX_RECENT);
        for (int i = 0; i < current.size(); i++) {
            prefs.put(RECENT_KEY + i, current.get(i).toString());
        }
    }

    private void removeFromRecent(Path path) {
        List<Path> current = new ArrayList<>(getRecentProjects());
        current.remove(path);
        for (int i = 0; i < MAX_RECENT; i++) {
            if (i < current.size()) prefs.put(RECENT_KEY + i, current.get(i).toString());
            else prefs.remove(RECENT_KEY + i);
        }
    }

    private Path ensureExtension(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(EXT) ? path : path.resolveSibling(name + EXT);
    }

    public static final class ProjectException extends RuntimeException {
        public ProjectException(String message) { super(message); }
        public ProjectException(String message, Throwable cause) { super(message, cause); }
    }
}