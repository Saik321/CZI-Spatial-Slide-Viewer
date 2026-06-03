package com.czispacialviewer.metadata;

public class CziPyramidLevel {

    private final int levelIndex;
    private final int width;
    private final int height;
    private final double downsample;
    private final int sourceSeriesIndex;

    public CziPyramidLevel(int levelIndex, int width, int height, double downsample) {
        this(levelIndex, width, height, downsample, -1);
    }

    public CziPyramidLevel(int levelIndex, int width, int height, double downsample, int sourceSeriesIndex) {
        this.levelIndex = levelIndex;
        this.width = width;
        this.height = height;
        this.downsample = downsample;
        this.sourceSeriesIndex = sourceSeriesIndex;
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
}
