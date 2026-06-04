package com.dataviz.domain.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class DashboardSnapshot {

    private final String                  id;
    private final String                  title;
    private final double                  width;
    private final double                  height;
    private final byte[]                  pngBytes;
    private final List<DashboardSnapshot> childSnapshots;
    private final Instant                 createdAt;

    public DashboardSnapshot(String id, String title,
                             double width, double height,
                             byte[] pngBytes,
                             List<DashboardSnapshot> childSnapshots) {
        this.id             = Objects.requireNonNull(id);
        this.title          = title != null ? title : "";
        this.width          = width;
        this.height         = height;
        this.pngBytes       = pngBytes;
        this.childSnapshots = List.copyOf(childSnapshots);
        this.createdAt      = Instant.now();
    }

    public DashboardSnapshot(String id, String title,
                             double width, double height,
                             List<DashboardSnapshot> childSnapshots) {
        this(id, title, width, height, null, childSnapshots);
    }

    public String                  getId()             { return id; }
    public String                  getTitle()          { return title; }
    public double                  getWidth()          { return width; }
    public double                  getHeight()         { return height; }
    public byte[]                  getPngBytes()       { return pngBytes; }
    public List<DashboardSnapshot> getChildSnapshots() { return childSnapshots; }
    public Instant                 getCreatedAt()      { return createdAt; }
    public boolean                 isLeaf()            { return childSnapshots.isEmpty(); }

    public List<DashboardSnapshot> allLeaves() {
        if (isLeaf()) return List.of(this);
        return childSnapshots.stream()
                .flatMap(c -> c.allLeaves().stream())
                .toList();
    }
}