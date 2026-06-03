package com.czispacialviewer.metadata;

public class CziTileInfo {

    private final int sceneIndex;
    private final int tileIndex;
    private final int pyramidLevel;
    private final double downsample;
    private final int width;
    private final int height;

    private double globalX;
    private double globalY;

    public CziTileInfo(int sceneIndex, int tileIndex, int pyramidLevel, double downsample, int width, int height) {
        this.sceneIndex = sceneIndex;
        this.tileIndex = tileIndex;
        this.pyramidLevel = pyramidLevel;
        this.downsample = downsample;
        this.width = width;
        this.height = height;
    }

    public int getSceneIndex() {
        return sceneIndex;
    }

    public int getTileIndex() {
        return tileIndex;
    }

    public int getPyramidLevel() {
        return pyramidLevel;
    }

    public double getDownsample() {
        return downsample;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
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
}
