package com.czispacialviewer.metadata;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CziSpatialMetadata {

    private final Path inputPath;
    private final List<CziSceneInfo> scenes;
    private final List<CziSeriesInfo> allSeries = new ArrayList<>();
    private final List<CziSeriesInfo> nonSpatialSeries = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<DiagnosticMessage> diagnostics = new ArrayList<>();

    private List<CziPyramidLevel> pyramidLevels = new ArrayList<>();
    private Double pixelSizeXMicrons;
    private Double pixelSizeYMicrons;
    private boolean rgb = true;
    private String pixelType = "uint8";
    private int channelCount = 3;
    private final List<String> channelNames = new ArrayList<>();
    private final List<Integer> channelColors = new ArrayList<>();

    private double minX;
    private double minY;
    private double maxX;
    private double maxY;
    private int globalWidth;
    private int globalHeight;
    private int totalTiles;
    private int sourceSeriesCount;

    public CziSpatialMetadata(Path inputPath, List<CziSceneInfo> scenes) {
        this.inputPath = inputPath;
        this.scenes = scenes;
    }

    public Path getInputPath() {
        return inputPath;
    }

    public List<CziSceneInfo> getScenes() {
        return scenes;
    }

    public List<CziSeriesInfo> getAllSeries() {
        return allSeries;
    }

    public List<CziSeriesInfo> getNonSpatialSeries() {
        return nonSpatialSeries;
    }

    public int getSpatialSeriesCount() {
        return (int) allSeries.stream().filter(CziSeriesInfo::isSpatial).count();
    }

    public int getSkippedSeriesCount() {
        return (int) allSeries.stream().filter(series -> series.getSkipReason() != null).count();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void addWarning(String warning) {
        warnings.add(warning);
        diagnostics.add(new DiagnosticMessage(DiagnosticLevel.WARNING, warning, null, null, null));
    }

    public void addDiagnostic(DiagnosticLevel level, String message, String source) {
        diagnostics.add(new DiagnosticMessage(level, message, source, null, null));
        if (level == DiagnosticLevel.WARNING || level == DiagnosticLevel.ERROR) {
            warnings.add(message);
        }
    }

    public void addDiagnostic(DiagnosticLevel level, String message, String source, Integer sceneIndex, Integer seriesIndex) {
        diagnostics.add(new DiagnosticMessage(level, message, source, sceneIndex, seriesIndex));
        if (level == DiagnosticLevel.WARNING || level == DiagnosticLevel.ERROR) {
            warnings.add(message);
        }
    }

    public List<DiagnosticMessage> getDiagnostics() {
        return diagnostics;
    }

    public List<CziPyramidLevel> getPyramidLevels() {
        return pyramidLevels;
    }

    public void setPyramidLevels(List<CziPyramidLevel> pyramidLevels) {
        this.pyramidLevels = pyramidLevels;
    }

    public Double getPixelSizeXMicrons() {
        return pixelSizeXMicrons;
    }

    public void setPixelSizeXMicrons(Double pixelSizeXMicrons) {
        this.pixelSizeXMicrons = pixelSizeXMicrons;
    }

    public Double getPixelSizeYMicrons() {
        return pixelSizeYMicrons;
    }

    public void setPixelSizeYMicrons(Double pixelSizeYMicrons) {
        this.pixelSizeYMicrons = pixelSizeYMicrons;
    }

    public boolean isRgb() {
        return rgb;
    }

    public void setRgb(boolean rgb) {
        this.rgb = rgb;
    }

    public String getPixelType() {
        return pixelType;
    }

    public void setPixelType(String pixelType) {
        this.pixelType = pixelType;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    public List<String> getChannelNames() {
        return channelNames;
    }

    public List<Integer> getChannelColors() {
        return channelColors;
    }

    public double getMinX() {
        return minX;
    }

    public void setMinX(double minX) {
        this.minX = minX;
    }

    public double getMinY() {
        return minY;
    }

    public void setMinY(double minY) {
        this.minY = minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public void setMaxX(double maxX) {
        this.maxX = maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }

    public int getGlobalWidth() {
        return globalWidth;
    }

    public void setGlobalWidth(int globalWidth) {
        this.globalWidth = globalWidth;
    }

    public int getGlobalHeight() {
        return globalHeight;
    }

    public void setGlobalHeight(int globalHeight) {
        this.globalHeight = globalHeight;
    }

    public int getTotalTiles() {
        return totalTiles;
    }

    public void setTotalTiles(int totalTiles) {
        this.totalTiles = totalTiles;
    }

    public int getSourceSeriesCount() {
        return sourceSeriesCount;
    }

    public void setSourceSeriesCount(int sourceSeriesCount) {
        this.sourceSeriesCount = sourceSeriesCount;
    }

    public boolean hasCompleteStagePositions() {
        return scenes.stream().allMatch(s -> s.getStageXMicrons() != null && s.getStageYMicrons() != null);
    }
}
