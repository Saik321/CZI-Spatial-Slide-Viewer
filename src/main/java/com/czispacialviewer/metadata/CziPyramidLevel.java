package com.czispacialviewer.metadata;

public class CziPyramidLevel {

    private final int levelIndex;
    private final int width;
    private final int height;
    private final double downsample;
    private final int sourceSeriesIndex;
    private final int sourceResolutionIndex;

    public CziPyramidLevel(int levelIndex, int width, int height, double downsample) {
        this(levelIndex, width, height, downsample, -1);
    }

    public CziPyramidLevel(int levelIndex, int width, int height, double downsample, int sourceSeriesIndex) {
        this(levelIndex, width, height, downsample, sourceSeriesIndex, 0);
    }

    public CziPyramidLevel(int levelIndex, int width, int height, double downsample, int sourceSeriesIndex, int sourceResolutionIndex) {
        this.levelIndex = levelIndex;
        this.width = width;
        this.height = height;
        this.downsample = downsample;
        this.sourceSeriesIndex = sourceSeriesIndex;
        this.sourceResolutionIndex = Math.max(0, sourceResolutionIndex);
    }

    public int getLevelIndex() {
        return levelIndex;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getDownsample() {
        return downsample;
    }

    public int getSourceSeriesIndex() {
        return sourceSeriesIndex;
    }

    public int getSourceResolutionIndex() {
        return sourceResolutionIndex;
    }
}
