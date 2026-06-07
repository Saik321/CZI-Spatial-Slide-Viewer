package com.czispacialviewer.util;

import com.czispacialviewer.backend.BioFormatsCziReaderBackend;
import com.czispacialviewer.CziSpatialSlideViewerInfo;
import com.czispacialviewer.metadata.DiagnosticMessage;
import com.czispacialviewer.metadata.CziPyramidLevel;
import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSeriesInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;
import com.czispacialviewer.metadata.CziTileInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class DebugJsonExporter {

    public static final Path DEFAULT_INPUT_PATH = Path.of(
            "data",
            "example_validation.czi");
    public static final Path OUTPUT_DIR = Path.of("outputs");
    public static final Path DEFAULT_MANIFEST_PATH = OUTPUT_DIR.resolve("czi_spatial_manifest.json");
    public static final Path DEFAULT_DEBUG_REPORT_PATH = OUTPUT_DIR.resolve("czi_debug_report.txt");
    public static final Path DEFAULT_PREVIEW_PATH = OUTPUT_DIR.resolve("czi_layout_preview.png");

    public static void exportAll(Path inputPath) throws Exception {
        try (var backend = new BioFormatsCziReaderBackend()) {
            CziSpatialMetadata metadata = backend.readMetadata(inputPath);
            OutputPaths paths = new OutputPaths(OUTPUT_DIR, inputPath);
            writeManifest(metadata, paths.manifest());
            writeDebugReport(metadata, backend.getName(), paths.debugReport());
            LayoutPreviewExporter.exportPreview(metadata, paths.layoutPreview(), backend);
            LayoutPreviewExporter.exportHaloStylePreview(metadata, paths.haloStylePreview(), backend);
            LayoutPreviewExporter.exportContactSheet(metadata, paths.contactSheet(), backend);
            HaloValidationReportExporter.writeReport(metadata, paths.haloValidationReport(), backend);
        }
    }

    public static void writeManifest(CziSpatialMetadata metadata, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(buildManifestJson(metadata));
        Files.writeString(outputPath, json, StandardCharsets.UTF_8);
    }

    public static void writeDebugReport(CziSpatialMetadata metadata, String backendName, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        StringBuilder sb = new StringBuilder();
        sb.append("CZI Spatial Debug Report").append(System.lineSeparator());
        sb.append("Plugin: ").append(CziSpatialSlideViewerInfo.PLUGIN_NAME).append(" ")
                .append(CziSpatialSlideViewerInfo.VERSION).append(System.lineSeparator());
        sb.append("File: ").append(metadata.getInputPath()).append(System.lineSeparator());
        sb.append("Backend: ").append(backendName).append(System.lineSeparator());
        sb.append("Bio-Formats series count: ").append(metadata.getSourceSeriesCount()).append(System.lineSeparator());
        sb.append("Detected scene count: ").append(metadata.getScenes().size()).append(System.lineSeparator());
        sb.append("All detected series/items: ").append(metadata.getAllSeries().size()).append(System.lineSeparator());
        sb.append("Non-spatial/skipped items: ").append(metadata.getNonSpatialSeries().size()).append(System.lineSeparator());
        sb.append("Global canvas: ").append(metadata.getGlobalWidth()).append(" x ")
                .append(metadata.getGlobalHeight()).append(System.lineSeparator());
        if (metadata.getPixelSizeXMicrons() != null && metadata.getPixelSizeYMicrons() != null) {
            sb.append("Pixel size (microns): ").append(metadata.getPixelSizeXMicrons())
                    .append(" x ").append(metadata.getPixelSizeYMicrons()).append(System.lineSeparator());
        } else {
            sb.append("Pixel size (microns): Unknown").append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
        sb.append("Scenes:").append(System.lineSeparator());
        for (CziSceneInfo scene : metadata.getScenes()) {
            sb.append("  Scene ").append(scene.getSceneIndex())
                    .append(" [series ").append(scene.getSeriesIndex()).append("]")
                    .append(" stage=(").append(scene.getStageXMicrons()).append(", ").append(scene.getStageYMicrons()).append(")")
                    .append(" global=(").append(scene.getGlobalX()).append(", ").append(scene.getGlobalY()).append(")")
                    .append(" size=(").append(scene.getWidth()).append(" x ").append(scene.getHeight()).append(")")
                    .append(" coordinateSource=").append(scene.getCoordinateSource())
                    .append(System.lineSeparator());
            if (!scene.getRawMetadata().isEmpty()) {
                sb.append("    Coordinate-like Bio-Formats metadata:").append(System.lineSeparator());
                scene.getRawMetadata().forEach((key, value) -> sb.append("      ")
                        .append(key).append(" = ").append(value).append(System.lineSeparator()));
            }
        }
        sb.append(System.lineSeparator());
        sb.append("Non-spatial / skipped series:").append(System.lineSeparator());
        if (metadata.getNonSpatialSeries().isEmpty()) {
            sb.append("  None").append(System.lineSeparator());
        } else {
            for (CziSeriesInfo series : metadata.getNonSpatialSeries()) {
                sb.append("  Series ").append(series.getSeriesIndex())
                        .append(" type=").append(series.getItemType())
                        .append(" size=(").append(series.getWidth()).append(" x ").append(series.getHeight()).append(")")
                        .append(" readable=").append(series.isReadable())
                        .append(" reason=").append(series.getSkipReason())
                        .append(System.lineSeparator());
            }
        }
        sb.append(System.lineSeparator());
        sb.append("Warnings:").append(System.lineSeparator());
        if (metadata.getWarnings().isEmpty()) {
            sb.append("  None").append(System.lineSeparator());
        } else {
            for (DiagnosticMessage diagnostic : metadata.getDiagnostics()) {
                sb.append("  - [").append(diagnostic.getLevel()).append("] ");
                if (diagnostic.getSource() != null) {
                    sb.append("(").append(diagnostic.getSource()).append(") ");
                }
                sb.append(diagnostic.getMessage()).append(System.lineSeparator());
            }
        }
        sb.append(System.lineSeparator());
        sb.append("HALO layout likely: ").append(metadata.hasCompleteStagePositions() ? "Yes" : "No").append(System.lineSeparator());
        Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
    }

    public static JsonObject buildManifestJson(CziSpatialMetadata metadata) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        root.addProperty("pluginName", CziSpatialSlideViewerInfo.PLUGIN_NAME);
        root.addProperty("pluginVersion", CziSpatialSlideViewerInfo.VERSION);
        root.addProperty("testedQuPathVersion", CziSpatialSlideViewerInfo.TESTED_QUPATH_VERSION);
        root.addProperty("javaVersion", System.getProperty("java.version"));
        root.addProperty("osName", System.getProperty("os.name"));
        root.addProperty("osVersion", System.getProperty("os.version"));
        root.addProperty("generatedAt", Instant.now().toString());
        root.addProperty("inputPath", metadata.getInputPath().toString());
        try {
            root.addProperty("inputFileSizeBytes", Files.size(metadata.getInputPath()));
            root.addProperty("inputLastModifiedTime", Files.getLastModifiedTime(metadata.getInputPath()).toInstant().toString());
        } catch (IOException ignored) {
            root.addProperty("inputFileSizeBytes", -1);
        }
        root.addProperty("bioFormatsSeriesCount", metadata.getSourceSeriesCount());
        root.addProperty("sceneCount", metadata.getScenes().size());
        root.addProperty("spatialSceneCount", metadata.getScenes().size());
        root.addProperty("allDetectedSeriesCount", metadata.getAllSeries().size());
        root.addProperty("nonSpatialItemCount", metadata.getNonSpatialSeries().size());
        root.addProperty("skippedItemCount", metadata.getSkippedSeriesCount());
        root.addProperty("haloStylePreviewIncludesInferredPresentationBand", !metadata.getNonSpatialSeries().isEmpty());
        root.addProperty("tileCount", metadata.getTotalTiles());
        root.addProperty("globalCanvasWidth", metadata.getGlobalWidth());
        root.addProperty("globalCanvasHeight", metadata.getGlobalHeight());
        root.addProperty("minX", metadata.getMinX());
        root.addProperty("minY", metadata.getMinY());
        root.addProperty("maxX", metadata.getMaxX());
        root.addProperty("maxY", metadata.getMaxY());

        JsonObject pixelCal = new JsonObject();
        if (metadata.getPixelSizeXMicrons() != null) {
            pixelCal.addProperty("pixelSizeXMicrons", metadata.getPixelSizeXMicrons());
        }
        if (metadata.getPixelSizeYMicrons() != null) {
            pixelCal.addProperty("pixelSizeYMicrons", metadata.getPixelSizeYMicrons());
        }
        root.add("pixelCalibration", pixelCal);

        JsonArray pyramidLevels = new JsonArray();
        for (CziPyramidLevel level : metadata.getPyramidLevels()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("levelIndex", level.getLevelIndex());
            obj.addProperty("width", level.getWidth());
            obj.addProperty("height", level.getHeight());
            obj.addProperty("downsample", level.getDownsample());
            obj.addProperty("sourceSeriesIndex", level.getSourceSeriesIndex());
            obj.addProperty("sourceResolutionIndex", level.getSourceResolutionIndex());
            pyramidLevels.add(obj);
        }
        root.add("pyramidLevels", pyramidLevels);

        JsonArray scenes = new JsonArray();
        for (CziSceneInfo scene : metadata.getScenes()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("sceneIndex", scene.getSceneIndex());
            obj.addProperty("seriesIndex", scene.getSeriesIndex());
            obj.addProperty("seriesName", scene.getSeriesName());
            obj.addProperty("format", scene.getFormat());
            obj.addProperty("compression", scene.getCompression());
            obj.addProperty("coordinateSource", scene.getCoordinateSource());
            obj.addProperty("pixelType", scene.getPixelType());
            obj.addProperty("rgb", scene.isRgb());
            obj.addProperty("interleaved", scene.isInterleaved());
            obj.addProperty("acquisitionDate", scene.getAcquisitionDate());
            obj.addProperty("units", scene.getUnits());
            obj.addProperty("stageZMicrons", scene.getStageZMicrons());
            obj.addProperty("objectiveMagnification", scene.getObjectiveMagnification());
            obj.addProperty("stageXMicrons", scene.getStageXMicrons());
            obj.addProperty("stageYMicrons", scene.getStageYMicrons());
            obj.addProperty("normalizedX", scene.getGlobalX());
            obj.addProperty("normalizedY", scene.getGlobalY());
            obj.addProperty("width", scene.getWidth());
            obj.addProperty("height", scene.getHeight());
            obj.addProperty("channelCount", scene.getChannelCount());
            obj.addProperty("zCount", scene.getZCount());
            obj.addProperty("timepointCount", scene.getTimepointCount());
            obj.addProperty("displayZIndex", scene.getDisplayZIndex());
            obj.addProperty("displayTIndex", scene.getDisplayTIndex());
            obj.addProperty("pixelSizeXMicrons", scene.getPixelSizeXMicrons());
            obj.addProperty("pixelSizeYMicrons", scene.getPixelSizeYMicrons());
            obj.addProperty("widthSource", "Bio-Formats core metadata");
            obj.addProperty("heightSource", "Bio-Formats core metadata");
            obj.addProperty("pixelSizeSource", "OME metadata");
            obj.addProperty("coordinateInterpretation", "Physical stage coordinates converted to global pixels using pixel calibration");

            JsonArray scenePyramidLevels = new JsonArray();
            for (CziPyramidLevel level : scene.getPyramidLevels()) {
                JsonObject levelObj = new JsonObject();
                levelObj.addProperty("levelIndex", level.getLevelIndex());
                levelObj.addProperty("width", level.getWidth());
                levelObj.addProperty("height", level.getHeight());
                levelObj.addProperty("downsample", level.getDownsample());
                levelObj.addProperty("sourceSeriesIndex", level.getSourceSeriesIndex());
                levelObj.addProperty("sourceResolutionIndex", level.getSourceResolutionIndex());
                scenePyramidLevels.add(levelObj);
            }
            obj.add("pyramidLevels", scenePyramidLevels);

            JsonArray tileArray = new JsonArray();
            for (CziTileInfo tile : scene.getTiles()) {
                JsonObject tileObj = new JsonObject();
                tileObj.addProperty("sceneIndex", tile.getSceneIndex());
                tileObj.addProperty("tileIndex", tile.getTileIndex());
                tileObj.addProperty("pyramidLevel", tile.getPyramidLevel());
                tileObj.addProperty("downsample", tile.getDownsample());
                tileObj.addProperty("x", tile.getGlobalX());
                tileObj.addProperty("y", tile.getGlobalY());
                tileObj.addProperty("width", tile.getWidth());
                tileObj.addProperty("height", tile.getHeight());
                tileArray.add(tileObj);
            }
            obj.add("tiles", tileArray);

            JsonObject rawMetadata = new JsonObject();
            scene.getRawMetadata().forEach(rawMetadata::addProperty);
            obj.add("rawCoordinateMetadata", rawMetadata);

            scenes.add(obj);
        }
        root.add("scenes", scenes);

        JsonArray allSeries = new JsonArray();
        for (CziSeriesInfo series : metadata.getAllSeries()) {
            allSeries.add(seriesToJson(series));
        }
        root.add("allDetectedSeries", allSeries);

        JsonArray nonSpatialItems = new JsonArray();
        for (CziSeriesInfo series : metadata.getNonSpatialSeries()) {
            nonSpatialItems.add(seriesToJson(series));
        }
        root.add("nonSpatialItems", nonSpatialItems);

        JsonArray warnings = new JsonArray();
        for (DiagnosticMessage diagnostic : metadata.getDiagnostics()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("level", diagnostic.getLevel().name());
            obj.addProperty("message", diagnostic.getMessage());
            obj.addProperty("source", diagnostic.getSource());
            obj.addProperty("sceneIndex", diagnostic.getSceneIndex());
            obj.addProperty("seriesIndex", diagnostic.getSeriesIndex());
            obj.addProperty("timestamp", diagnostic.getTimestamp().toString());
            warnings.add(obj);
        }
        root.add("warnings", warnings);

        return root;
    }

    private static JsonObject seriesToJson(CziSeriesInfo series) {
        JsonObject obj = new JsonObject();
        obj.addProperty("seriesIndex", series.getSeriesIndex());
        obj.addProperty("seriesName", series.getSeriesName());
        obj.addProperty("itemType", series.getItemType());
        obj.addProperty("spatial", series.isSpatial());
        obj.addProperty("readable", series.isReadable());
        obj.addProperty("skipReason", series.getSkipReason());
        obj.addProperty("width", series.getWidth());
        obj.addProperty("height", series.getHeight());
        obj.addProperty("channelCount", series.getChannelCount());
        obj.addProperty("zCount", series.getZCount());
        obj.addProperty("timepointCount", series.getTimepointCount());
        obj.addProperty("displayZIndex", series.getDisplayZIndex());
        obj.addProperty("displayTIndex", series.getDisplayTIndex());
        obj.addProperty("pixelType", series.getPixelType());
        obj.addProperty("rgb", series.isRgb());
        obj.addProperty("interleaved", series.isInterleaved());
        obj.addProperty("stageXMicrons", series.getStageXMicrons());
        obj.addProperty("stageYMicrons", series.getStageYMicrons());
        obj.addProperty("coordinateSource", series.getCoordinateSource());
        obj.addProperty("widthSource", "Bio-Formats core metadata");
        obj.addProperty("heightSource", "Bio-Formats core metadata");
        obj.addProperty("classificationSource", "inferred from coordinates, dimensions, name, and available metadata");
        return obj;
    }

    public static void main(String[] args) {
        try {
            exportAll(resolveInputPath(args));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Path resolveInputPath(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return Path.of(args[0]);
        }
        String propertyPath = System.getProperty("czi.input");
        if (propertyPath != null && !propertyPath.isBlank()) {
            return Path.of(propertyPath);
        }
        String envPath = System.getenv("CZI_SPATIAL_VIEWER_INPUT");
        if (envPath != null && !envPath.isBlank()) {
            return Path.of(envPath);
        }
        return DEFAULT_INPUT_PATH;
    }
}
