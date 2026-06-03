package com.czispacialviewer.metadata;

public class CziSeriesInfo {

    private final int seriesIndex;
    private final int width;
    private final int height;

    private String seriesName;
    private String itemType = "unknown";
    private String skipReason;
    private boolean spatial;
    private boolean readable = true;
    private int channelCount;
    private String pixelType;
    private boolean rgb;
    private boolean interleaved;
    private String coordinateSource;
    private Double stageXMicrons;
    private Double stageYMicrons;

    public CziSeriesInfo(int seriesIndex, int width, int height) {
        this.seriesIndex = seriesIndex;
        this.width = width;
        this.height = height;
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

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public void setSkipReason(String skipReason) {
        this.skipReason = skipReason;
    }

    public boolean isSpatial() {
        return spatial;
    }

    public void setSpatial(boolean spatial) {
        this.spatial = spatial;
    }

    public boolean isReadable() {
        return readable;
    }

    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
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

    public String getCoordinateSource() {
        return coordinateSource;
    }

    public void setCoordinateSource(String coordinateSource) {
        this.coordinateSource = coordinateSource;
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
}
