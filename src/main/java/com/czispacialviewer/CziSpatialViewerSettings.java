package com.czispacialviewer;

import java.util.prefs.Preferences;

public class CziSpatialViewerSettings {

    private static final Preferences PREFS = Preferences.userNodeForPackage(CziSpatialViewerSettings.class);
    private static final CziSpatialViewerSettings DEFAULT = new CziSpatialViewerSettings();

    private int maxCacheMemoryMb = 128;
    private int maxPreviewDimension = 4096;
    private int thumbnailSize = 256;
    private int maxOutputPixels = 67_108_864;
    private int transparentBackgroundThreshold = 178;
    private int transparentBackgroundMaxColorSpread = 52;
    private boolean debugLogging;
    private boolean overlaySceneBoundaries;
    private boolean overlaySceneLabels;
    private boolean overlayTileBoundaries;
    private boolean overlayCoordinateGrid;
    private boolean transparentLightBackgroundInOverlaps = true;

    private CziSpatialViewerSettings() {
        load();
    }

    public static CziSpatialViewerSettings getDefault() {
        return DEFAULT;
    }

    public int getMaxCacheMemoryMb() {
        return maxCacheMemoryMb;
    }

    public long getMaxCacheBytes() {
        return Math.max(1L, maxCacheMemoryMb) * 1024L * 1024L;
    }

    public int getMaxPreviewDimension() {
        return maxPreviewDimension;
    }

    public int getThumbnailSize() {
        return thumbnailSize;
    }

    public int getMaxOutputPixels() {
        return maxOutputPixels;
    }

    public int getTransparentBackgroundThreshold() {
        return transparentBackgroundThreshold;
    }

    public int getTransparentBackgroundMaxColorSpread() {
        return transparentBackgroundMaxColorSpread;
    }

    public boolean isDebugLogging() {
        return debugLogging;
    }

    public boolean isOverlaySceneBoundaries() {
        return overlaySceneBoundaries;
    }

    public boolean isOverlaySceneLabels() {
        return overlaySceneLabels;
    }

    public boolean isOverlayTileBoundaries() {
        return overlayTileBoundaries;
    }

    public boolean isOverlayCoordinateGrid() {
        return overlayCoordinateGrid;
    }

    public boolean isTransparentLightBackgroundInOverlaps() {
        return transparentLightBackgroundInOverlaps;
    }

    public void setMaxCacheMemoryMb(int maxCacheMemoryMb) {
        this.maxCacheMemoryMb = clamp(maxCacheMemoryMb, 64, 4096);
    }

    public void setMaxPreviewDimension(int maxPreviewDimension) {
        this.maxPreviewDimension = clamp(maxPreviewDimension, 512, 16384);
    }

    public void setThumbnailSize(int thumbnailSize) {
        this.thumbnailSize = clamp(thumbnailSize, 64, 1024);
    }

    public void setMaxOutputPixels(int maxOutputPixels) {
        this.maxOutputPixels = clamp(maxOutputPixels, 1_048_576, 268_435_456);
    }

    public void setTransparentBackgroundThreshold(int transparentBackgroundThreshold) {
        this.transparentBackgroundThreshold = clamp(transparentBackgroundThreshold, 120, 245);
    }

    public void setTransparentBackgroundMaxColorSpread(int transparentBackgroundMaxColorSpread) {
        this.transparentBackgroundMaxColorSpread = clamp(transparentBackgroundMaxColorSpread, 0, 120);
    }

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    public void setOverlaySceneBoundaries(boolean overlaySceneBoundaries) {
        this.overlaySceneBoundaries = overlaySceneBoundaries;
    }

    public void setOverlaySceneLabels(boolean overlaySceneLabels) {
        this.overlaySceneLabels = overlaySceneLabels;
    }

    public void setOverlayTileBoundaries(boolean overlayTileBoundaries) {
        this.overlayTileBoundaries = overlayTileBoundaries;
    }

    public void setOverlayCoordinateGrid(boolean overlayCoordinateGrid) {
        this.overlayCoordinateGrid = overlayCoordinateGrid;
    }

    public void setTransparentLightBackgroundInOverlaps(boolean transparentLightBackgroundInOverlaps) {
        this.transparentLightBackgroundInOverlaps = transparentLightBackgroundInOverlaps;
    }

    public void save() {
        PREFS.putInt("maxCacheMemoryMb", maxCacheMemoryMb);
        PREFS.putInt("maxPreviewDimension", maxPreviewDimension);
        PREFS.putInt("thumbnailSize", thumbnailSize);
        PREFS.putInt("maxOutputPixels", maxOutputPixels);
        PREFS.putInt("transparentBackgroundThreshold", transparentBackgroundThreshold);
        PREFS.putInt("transparentBackgroundMaxColorSpread", transparentBackgroundMaxColorSpread);
        PREFS.putBoolean("debugLogging", debugLogging);
        PREFS.putBoolean("overlaySceneBoundaries", overlaySceneBoundaries);
        PREFS.putBoolean("overlaySceneLabels", overlaySceneLabels);
        PREFS.putBoolean("overlayTileBoundaries", overlayTileBoundaries);
        PREFS.putBoolean("overlayCoordinateGrid", overlayCoordinateGrid);
        PREFS.putBoolean("transparentLightBackgroundInOverlaps", transparentLightBackgroundInOverlaps);
    }

    public void resetDefaults() {
        maxCacheMemoryMb = 128;
        maxPreviewDimension = 4096;
        thumbnailSize = 256;
        maxOutputPixels = 67_108_864;
        transparentBackgroundThreshold = 178;
        transparentBackgroundMaxColorSpread = 52;
        debugLogging = false;
        overlaySceneBoundaries = false;
        overlaySceneLabels = false;
        overlayTileBoundaries = false;
        overlayCoordinateGrid = false;
        transparentLightBackgroundInOverlaps = true;
        save();
    }

    public String cacheSignature() {
        return "bg=" + transparentLightBackgroundInOverlaps
                + ",thr=" + transparentBackgroundThreshold
                + ",spread=" + transparentBackgroundMaxColorSpread
                + ",bounds=" + overlaySceneBoundaries
                + ",labels=" + overlaySceneLabels
                + ",tiles=" + overlayTileBoundaries
                + ",grid=" + overlayCoordinateGrid;
    }

    private void load() {
        maxCacheMemoryMb = clamp(PREFS.getInt("maxCacheMemoryMb", maxCacheMemoryMb), 64, 4096);
        maxPreviewDimension = clamp(PREFS.getInt("maxPreviewDimension", maxPreviewDimension), 512, 16384);
        thumbnailSize = clamp(PREFS.getInt("thumbnailSize", thumbnailSize), 64, 1024);
        maxOutputPixels = clamp(PREFS.getInt("maxOutputPixels", maxOutputPixels), 1_048_576, 268_435_456);
        transparentBackgroundThreshold = clamp(PREFS.getInt("transparentBackgroundThreshold", transparentBackgroundThreshold), 120, 245);
        transparentBackgroundMaxColorSpread = clamp(PREFS.getInt("transparentBackgroundMaxColorSpread", transparentBackgroundMaxColorSpread), 0, 120);
        debugLogging = PREFS.getBoolean("debugLogging", debugLogging);
        overlaySceneBoundaries = PREFS.getBoolean("overlaySceneBoundaries", overlaySceneBoundaries);
        overlaySceneLabels = PREFS.getBoolean("overlaySceneLabels", overlaySceneLabels);
        overlayTileBoundaries = PREFS.getBoolean("overlayTileBoundaries", overlayTileBoundaries);
        overlayCoordinateGrid = PREFS.getBoolean("overlayCoordinateGrid", overlayCoordinateGrid);
        transparentLightBackgroundInOverlaps = PREFS.getBoolean("transparentLightBackgroundInOverlaps", transparentLightBackgroundInOverlaps);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
