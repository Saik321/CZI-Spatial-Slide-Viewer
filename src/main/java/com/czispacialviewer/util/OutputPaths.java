package com.czispacialviewer.util;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;

public class OutputPaths {

    private final Path outputDir;
    private final String safeBaseName;

    public OutputPaths(Path outputDir, Path inputPath) {
        this.outputDir = outputDir;
        this.safeBaseName = safeBaseName(inputPath);
    }

    public Path manifest() {
        return outputDir.resolve(safeBaseName + "_manifest.json");
    }

    public Path layoutPreview() {
        return outputDir.resolve(safeBaseName + "_layout_preview.png");
    }

    public Path haloStylePreview() {
        return outputDir.resolve(safeBaseName + "_halo_style_preview.png");
    }

    public Path debugReport() {
        return outputDir.resolve(safeBaseName + "_debug_report.txt");
    }

    public Path contactSheet() {
        return outputDir.resolve(safeBaseName + "_contact_sheet.png");
    }

    public Path performanceReport() {
        return outputDir.resolve(safeBaseName + "_performance_report.txt");
    }

    public Path supportBundle() {
        return outputDir.resolve(safeBaseName + "_support_bundle.zip");
    }

    public Path haloValidationReport() {
        return outputDir.resolve(safeBaseName + "_halo_validation_report.txt");
    }

    public String getSafeBaseName() {
        return safeBaseName;
    }

    public Path outputDir() {
        return outputDir;
    }

    public static String safeBaseName(Path inputPath) {
        String fileName = inputPath == null || inputPath.getFileName() == null ? "czi_spatial" : inputPath.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            fileName = fileName.substring(0, dot);
        }
        String normalized = Normalizer.normalize(fileName, Normalizer.Form.NFKD)
                .replaceAll("[^A-Za-z0-9._-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            normalized = "czi_spatial";
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
