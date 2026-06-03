package com.czispacialviewer.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceStats {

    private final AtomicLong tileRequests = new AtomicLong();
    private final AtomicLong totalReadNanos = new AtomicLong();
    private final AtomicLong slowestReadNanos = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong bioFormatsOpens = new AtomicLong();
    private final AtomicLong peakEstimatedCacheBytes = new AtomicLong();
    private final Map<String, Long> pyramidLevelCounts = new LinkedHashMap<>();

    public void recordRequest(long nanos, int intersectingScenes) {
        tileRequests.incrementAndGet();
        totalReadNanos.addAndGet(nanos);
        slowestReadNanos.updateAndGet(previous -> Math.max(previous, nanos));
    }

    public synchronized void recordPyramidLevel(double downsample) {
        String key = Double.toString(downsample);
        pyramidLevelCounts.put(key, pyramidLevelCounts.getOrDefault(key, 0L) + 1L);
    }

    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    public void recordBackendOpen() {
        bioFormatsOpens.incrementAndGet();
    }

    public void updatePeakEstimatedCacheBytes(long bytes) {
        peakEstimatedCacheBytes.updateAndGet(previous -> Math.max(previous, bytes));
    }

    public long getTileRequests() {
        return tileRequests.get();
    }

    public double getAverageReadMillis() {
        long requests = tileRequests.get();
        return requests == 0 ? 0.0 : totalReadNanos.get() / 1_000_000.0 / requests;
    }

    public double getSlowestReadMillis() {
        return slowestReadNanos.get() / 1_000_000.0;
    }

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getCacheMisses() {
        return cacheMisses.get();
    }

    public double getCacheHitRate() {
        long total = cacheHits.get() + cacheMisses.get();
        return total == 0 ? 0.0 : cacheHits.get() / (double) total;
    }

    public long getBioFormatsOpens() {
        return bioFormatsOpens.get();
    }

    public long getPeakEstimatedCacheBytes() {
        return peakEstimatedCacheBytes.get();
    }

    public synchronized Map<String, Long> getPyramidLevelCounts() {
        return new LinkedHashMap<>(pyramidLevelCounts);
    }
}
