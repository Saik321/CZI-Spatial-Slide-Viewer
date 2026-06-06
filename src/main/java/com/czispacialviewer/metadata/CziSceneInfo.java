package com.czispacialviewer.metadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CziSceneInfo {

    private final int sceneIndex;
    private final int seriesIndex;
    private final int width;
    private final int height;

    private int channelCount;
    private String seriesName;
    private String format;
    private String compression;
    private String coordinateSource;
    private String pixelType;
    private boolean rgb;
    private boolean interleaved;
    private String acquisitionDate;
    private String units;
    private Double stageZMicrons;
    private Double objectiveMagnification;
    private final List<String> channelNames = new ArrayList<>();
    private final List<Integer> channelColors = new ArrayList<>();

    private Double stageXMicrons;
    private Double stageYMicrons;
    private Double pixelSizeXMicrons;
    private Double pixelSizeYMicrons;

    private double globalX;
    private double globalY;

    private final List<CziTileInfo> tiles = new ArrayList<>();
    private final List<CziPyramidLevel> pyramidLevels = new ArrayList<>();
    private final Map<String, String> rawMetadata = new LinkedHashMap<>();

    public CziSceneInfo(int sceneIndex, int seriesIndex, int width, int height) {
        this.sceneIndex = sceneIndex;
        this.seriesIndex = seriesIndex;
        this.width = width;
        this.height = height;
    }

    public int getSceneIndex() {
        return sceneIndex;
    }

    public int getSeriesIndex() {
        return seriesIndex;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public String getCoordinateSource() {
        return coordinateSource;
    }

    public void setCoordinateSource(String coordinateSource) {
        this.coordinateSource = coordinateSource;
    }

    public String getPixelType() {
        return pixelType;
    }

    public void setPixelType(String pixelType) {
        this.pixelType = pixelType;
    }

    public boolean isRgb() {
        return rgb;
    }

    public void setRgb(boolean rgb) {
        this.rgb = rgb;
    }

    public boolean isInterleaved() {
        return interleaved;
    }

    public void setInterleaved(boolean interleaved) {
        this.interleaved = interleaved;
    }

    public String getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(String acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public Double getStageZMicrons() {
        return stageZMicrons;
    }

    public void setStageZMicrons(Double stageZMicrons) {
        this.stageZMicrons = stageZMicrons;
    }

    public Double getObjectiveMagnification() {
        return objectiveMagnification;
    }

    public void setObjectiveMagnification(Double objectiveMagnification) {
        this.objectiveMagnification = objectiveMagnification;
    }

    public List<String> getChannelNames() {
        return channelNames;
    }

    public List<Integer> getChannelColors() {
        return channelColors;
    }

    public Double getStageXMicrons() {
        return stageXMicrons;
    }

    public void setStageXMicrons(Double stageXMicrons) {
        this.stageXMicrons = stageXMicrons;
    }

    public Double getStageYMicrons() {
        return stageYMicrons;
    }

    public void setStageYMicrons(Double stageYMicrons) {
        this.stageYMicrons = stageYMicrons;
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

    public double getGlobalX() {
        return globalX;
    }

    public void setGlobalX(double globalX) {
        this.globalX = globalX;
    }

    public double getGlobalY() {
        return globalY;
    }

    public void setGlobalY(double globalY) {
        this.globalY = globalY;
    }

    public List<CziTileInfo> getTiles() {
        return tiles;
    }

    public List<CziPyramidLevel> getPyramidLevels() {
        return pyramidLevels;
    }

    public Map<String, String> getRawMetadata() {
        return rawMetadata;
    }
}
