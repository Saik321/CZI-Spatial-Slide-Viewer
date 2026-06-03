package com.czispacialviewer.util;

import com.czispacialviewer.backend.CziReaderBackend;
import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSeriesInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;

public class HaloValidationReportExporter {

    public static void writeReport(CziSpatialMetadata metadata, Path outputPath, CziReaderBackend backend) throws IOException {
        Files.createDirectories(outputPath.getParent());
        ThumbnailCounts thumbnailCounts = countReadableThumbnails(metadata, backend);

        StringBuilder sb = new StringBuilder();
        sb.append("CZI Spatial Viewer HALO Validation Report").append(System.lineSeparator());
        sb.append("File: ").append(metadata.getInputPath()).append(System.lineSeparator());
        sb.append("Global canvas: ").append(metadata.getGlobalWidth()).append(" x ")
                .append(metadata.getGlobalHeight()).append(System.lineSeparator());
        double aspect = metadata.getGlobalHeight() <= 0 ? 0.0 : metadata.getGlobalWidth() / (double) metadata.getGlobalHeight();
        sb.append("Global canvas aspect ratio: ").append(String.format(Locale.ROOT, "%.4f", aspect)).append(System.lineSeparator());
        sb.append("Spatial scenes rendered: ").append(metadata.getScenes().size()).append(System.lineSeparator());
        sb.append("Detected Bio-Formats series/items: ").append(metadata.getAllSeries().size()).append(System.lineSeparator());
        sb.append("Non-spatial/attachment-like items: ").append(metadata.getNonSpatialSeries().size()).append(System.lineSeparator());
        sb.append("Skipped items: ").append(metadata.getSkippedSeriesCount()).append(System.lineSeparator());
        sb.append("Bounds: min=(").append(metadata.getMinX()).append(", ").append(metadata.getMinY())
                .append(") max=(").append(metadata.getMaxX()).append(", ").append(metadata.getMaxY()).append(")")
                .append(System.lineSeparator());
        appendMinScene(sb, "Leftmost scene", metadata, Comparator.comparingDouble(CziSceneInfo::getGlobalX));
        appendMaxScene(sb, "Rightmost scene", metadata,
                Comparator.comparingDouble(scene -> scene.getGlobalX() + scene.getWidth()));
        appendMinScene(sb, "Topmost scene", metadata, Comparator.comparingDouble(CziSceneInfo::getGlobalY));
        appendMaxScene(sb, "Bottommost scene", metadata,
                Comparator.comparingDouble(scene -> scene.getGlobalY() + scene.getHeight()));
        sb.append("Largest estimated horizontal gap: ").append(String.format(Locale.ROOT, "%.1f", estimateLargestHorizontalGap(metadata)))
                .append(" px").append(System.lineSeparator());
        sb.append("Readable thumbnails: ").append(thumbnailCounts.readable).append(System.lineSeparator());
        sb.append("Failed thumbnails: ").append(thumbnailCounts.failed).append(System.lineSeparator());
        sb.append("Label/macro/thumbnail-like items detected: ")
                .append(hasTopLikeItems(metadata) ? "yes" : "no").append(System.lineSeparator());
        if (!metadata.getNonSpatialSeries().isEmpty()) {
            sb.append("HALO-style preview: non-spatial items are drawn in an inferred top presentation band only;")
                    .append(" they are not inserted into the QuPath analysis coordinate canvas without X/Y metadata.")
                    .append(System.lineSeparator());
        }
        if (!hasTopLikeItems(metadata)) {
            sb.append("WARNING: No series was classified as label/macro/thumbnail/attachment. If HALO shows top extra regions,")
                    .append(" Bio-Formats may expose them only as regular spatial series or may not expose their attachment metadata.")
                    .append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
        sb.append("Manual HALO comparison checklist:").append(System.lineSeparator());
        sb.append("[ ] Wide canvas shape matches HALO.").append(System.lineSeparator());
        sb.append("[ ] Tissue sections form similar left/middle/right groups.").append(System.lineSeparator());
        sb.append("[ ] Far-right group exists.").append(System.lineSeparator());
        sb.append("[ ] Left cluster exists.").append(System.lineSeparator());
        sb.append("[ ] Middle clusters exist.").append(System.lineSeparator());
        sb.append("[ ] Large blank gaps are preserved.").append(System.lineSeparator());
        sb.append("[ ] Visible tissue scene count is close.").append(System.lineSeparator());
        sb.append("[ ] Extra top label/macro/grid-like regions are detected or explained below.").append(System.lineSeparator());
        sb.append("[ ] No scenes are stacked incorrectly.").append(System.lineSeparator());
        sb.append("[ ] No scenes are missing without a warning or skipped-item explanation.").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("Non-spatial / skipped item details:").append(System.lineSeparator());
        if (metadata.getNonSpatialSeries().isEmpty()) {
            sb.append("None.").append(System.lineSeparator());
        } else {
            for (CziSeriesInfo series : metadata.getNonSpatialSeries()) {
                sb.append("- Series ").append(series.getSeriesIndex())
                        .append(" type=").append(series.getItemType())
                        .append(" size=").append(series.getWidth()).append("x").append(series.getHeight())
                        .append(" reason=").append(series.getSkipReason())
                        .append(System.lineSeparator());
            }
        }
        Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void appendMinScene(StringBuilder sb, String label, CziSpatialMetadata metadata,
                                       Comparator<CziSceneInfo> comparator) {
        metadata.getScenes().stream().min(comparator).ifPresent(scene -> sb.append(label).append(": scene ")
                .append(scene.getSceneIndex()).append(" series ").append(scene.getSeriesIndex())
                .append(" global=(").append(scene.getGlobalX()).append(", ").append(scene.getGlobalY()).append(")")
                .append(" size=").append(scene.getWidth()).append("x").append(scene.getHeight())
                .append(System.lineSeparator()));
    }

    private static void appendMaxScene(StringBuilder sb, String label, CziSpatialMetadata metadata,
                                       Comparator<CziSceneInfo> comparator) {
        metadata.getScenes().stream().max(comparator).ifPresent(scene -> sb.append(label).append(": scene ")
                .append(scene.getSceneIndex()).append(" series ").append(scene.getSeriesIndex())
                .append(" global=(").append(scene.getGlobalX()).append(", ").append(scene.getGlobalY()).append(")")
                .append(" size=").append(scene.getWidth()).append("x").append(scene.getHeight())
                .append(System.lineSeparator()));
    }

    private static double estimateLargestHorizontalGap(CziSpatialMetadata metadata) {
        var scenes = metadata.getScenes().stream()
                .sorted(Comparator.comparingDouble(CziSceneInfo::getGlobalX))
                .toList();
        double maxGap = 0.0;
        double rightEdge = Double.NaN;
        for (CziSceneInfo scene : scenes) {
            if (!Double.isNaN(rightEdge)) {
                maxGap = Math.max(maxGap, scene.getGlobalX() - rightEdge);
            }
            rightEdge = Double.isNaN(rightEdge)
                    ? scene.getGlobalX() + scene.getWidth()
                    : Math.max(rightEdge, scene.getGlobalX() + scene.getWidth());
        }
        return Math.max(0.0, maxGap);
    }

    private static boolean hasTopLikeItems(CziSpatialMetadata metadata) {
        return metadata.getAllSeries().stream().anyMatch(series -> {
            String type = series.getItemType() == null ? "" : series.getItemType();
            return type.contains("label") || type.contains("macro") || type.contains("thumbnail") || type.contains("attachment");
        });
    }

    private static ThumbnailCounts countReadableThumbnails(CziSpatialMetadata metadata, CziReaderBackend backend) {
        if (backend == null) {
            return new ThumbnailCounts(0, metadata.getScenes().size());
        }
        int readable = 0;
        int failed = 0;
        for (CziSceneInfo scene : metadata.getScenes()) {
            try {
                double downsample = Math.max(1.0, Math.max(scene.getWidth(), scene.getHeight()) / 128.0);
                backend.readRegion(metadata.getInputPath(), scene, 0, 0, scene.getWidth(), scene.getHeight(), downsample);
                readable++;
            } catch (Exception e) {
                failed++;
            }
        }
        return new ThumbnailCounts(readable, failed);
    }

    private record ThumbnailCounts(int readable, int failed) {
    }
}
