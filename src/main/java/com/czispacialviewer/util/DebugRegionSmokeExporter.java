package com.czispacialviewer.util;

import com.czispacialviewer.CziSpatialImageServer;
import com.czispacialviewer.metadata.CziSceneInfo;
import qupath.lib.regions.RegionRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

public class DebugRegionSmokeExporter {

    public static final Path DEFAULT_REGION_SMOKE_PATH = DebugJsonExporter.OUTPUT_DIR.resolve("czi_region_smoke.png");

    public static void exportSmokeRegion(Path inputPath, Path outputPath) throws Exception {
        Files.createDirectories(outputPath.getParent());
        try (var server = new CziSpatialImageServer(inputPath.toUri())) {
            OutputPaths paths = new OutputPaths(DebugJsonExporter.OUTPUT_DIR, inputPath);
            CziSceneInfo scene = server.getSpatialMetadata().getScenes().get(0);
            int x = (int)Math.round(scene.getGlobalX());
            int y = (int)Math.round(scene.getGlobalY());
            int width = Math.min(4096, scene.getWidth());
            int height = Math.min(4096, scene.getHeight());
            RegionRequest request = RegionRequest.createInstance(server.getPath(), 16.0, x, y, width, height);
            BufferedImage image = server.readRegion(request);
            ImageIO.write(image, "png", outputPath.toFile());
            PerformanceReportExporter.writeReport(server.getPerformanceStats(), paths.performanceReport());
        }
    }

    public static void main(String[] args) {
        try {
            exportSmokeRegion(DebugJsonExporter.DEFAULT_INPUT_PATH, DEFAULT_REGION_SMOKE_PATH);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
