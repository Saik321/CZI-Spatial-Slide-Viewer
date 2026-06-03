package com.czispacialviewer.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class PerformanceReportExporter {

    public static void writeReport(PerformanceStats stats, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        StringBuilder sb = new StringBuilder();
        sb.append("CZI Spatial Viewer Performance Report").append(System.lineSeparator());
        sb.append("Tile requests: ").append(stats.getTileRequests()).append(System.lineSeparator());
        sb.append("Average read time ms: ").append(stats.getAverageReadMillis()).append(System.lineSeparator());
        sb.append("Slowest read time ms: ").append(stats.getSlowestReadMillis()).append(System.lineSeparator());
        sb.append("Cache hits: ").append(stats.getCacheHits()).append(System.lineSeparator());
        sb.append("Cache misses: ").append(stats.getCacheMisses()).append(System.lineSeparator());
        sb.append("Cache hit rate: ").append(stats.getCacheHitRate()).append(System.lineSeparator());
        sb.append("Bio-Formats opens/reopens: ").append(stats.getBioFormatsOpens()).append(System.lineSeparator());
        sb.append("Peak estimated cache bytes: ").append(stats.getPeakEstimatedCacheBytes()).append(System.lineSeparator());
        sb.append("Requested pyramid/downsample levels:").append(System.lineSeparator());
        for (Map.Entry<String, Long> entry : stats.getPyramidLevelCounts().entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append(System.lineSeparator());
        }
        Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
    }
}
