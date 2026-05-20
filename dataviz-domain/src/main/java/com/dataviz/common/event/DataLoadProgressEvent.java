package com.dataviz.common.event;

import java.util.Objects;

public final class DataLoadProgressEvent {

    private final String jobId;
    private final double progress;
    private final long   bytesRead;
    private final long   totalBytes;

    public DataLoadProgressEvent(String jobId,
                                 double progress,
                                 long bytesRead,
                                 long totalBytes) {
        this.jobId      = Objects.requireNonNull(jobId, "jobId must not be null");
        this.progress   = Math.clamp(progress, 0.0, 1.0);
        this.bytesRead  = bytesRead;
        this.totalBytes = totalBytes;
    }

    public static DataLoadProgressEvent of(String jobId, long bytesRead, long totalBytes) {
        double p = totalBytes > 0 ? (double) bytesRead / totalBytes : 0.0;
        return new DataLoadProgressEvent(jobId, p, bytesRead, totalBytes);
    }

    public static DataLoadProgressEvent ofUnknownSize(String jobId, double progress) {
        return new DataLoadProgressEvent(jobId, progress, -1, -1);
    }

    public String getJobId()      { return jobId; }

    public double getProgress()   { return progress; }

    public long getBytesRead()    { return bytesRead; }

    public long getTotalBytes()   { return totalBytes; }

    public boolean isSizeKnown()  { return totalBytes >= 0; }

    public boolean isCompleted()  { return progress >= 1.0; }

    @Override
    public String toString() {
        return "DataLoadProgressEvent{jobId='%s', progress=%.1f%%, bytes=%d/%d}"
                .formatted(jobId, progress * 100,
                        bytesRead < 0 ? 0 : bytesRead,
                        totalBytes < 0 ? 0 : totalBytes);
    }
}