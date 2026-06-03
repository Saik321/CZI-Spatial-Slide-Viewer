package com.czispacialviewer.util;

import com.czispacialviewer.CziSpatialSlideViewerInfo;
import com.czispacialviewer.backend.BioFormatsCziReaderBackend;
import com.czispacialviewer.metadata.CziSpatialMetadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SupportBundleExporter {

    public static Path exportSupportBundle(Path inputPath) throws Exception {
        OutputPaths paths = new OutputPaths(DebugJsonExporter.OUTPUT_DIR, inputPath);
        try (var backend = new BioFormatsCziReaderBackend()) {
            CziSpatialMetadata metadata = backend.readMetadata(inputPath);
            DebugJsonExporter.writeManifest(metadata, paths.manifest());
            DebugJsonExporter.writeDebugReport(metadata, backend.getName(), paths.debugReport());
            LayoutPreviewExporter.exportPreview(metadata, paths.layoutPreview(), backend);
            LayoutPreviewExporter.exportHaloStylePreview(metadata, paths.haloStylePreview(), backend);
            LayoutPreviewExporter.exportContactSheet(metadata, paths.contactSheet(), backend);
            HaloValidationReportExporter.writeReport(metadata, paths.haloValidationReport(), backend);
            writeSystemInfo(paths.outputDir().resolve(paths.getSafeBaseName() + "_system_info.txt"));
        }
        writeZip(paths);
        return paths.supportBundle();
    }

    private static void writeSystemInfo(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        String text = "Plugin: " + CziSpatialSlideViewerInfo.PLUGIN_NAME + " " + CziSpatialSlideViewerInfo.VERSION + System.lineSeparator()
                + "Java: " + System.getProperty("java.version") + System.lineSeparator()
                + "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + System.lineSeparator()
                + "Tested QuPath: " + CziSpatialSlideViewerInfo.TESTED_QUPATH_VERSION + System.lineSeparator();
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }

    private static void writeZip(OutputPaths paths) throws IOException {
        Files.createDirectories(paths.supportBundle().getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(paths.supportBundle()))) {
            addIfExists(zip, paths.manifest());
            addIfExists(zip, paths.debugReport());
            addIfExists(zip, paths.layoutPreview());
            addIfExists(zip, paths.haloStylePreview());
            addIfExists(zip, paths.contactSheet());
            addIfExists(zip, paths.haloValidationReport());
            addIfExists(zip, paths.performanceReport());
            addIfExists(zip, paths.outputDir().resolve(paths.getSafeBaseName() + "_system_info.txt"));
        }
    }

    private static void addIfExists(ZipOutputStream zip, Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        zip.putNextEntry(new ZipEntry(path.getFileName().toString()));
        Files.copy(path, zip);
        zip.closeEntry();
    }
}
