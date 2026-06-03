package com.czispacialviewer.backend;

import java.awt.image.BufferedImage;

public class CziTileReadResult {

    private final BufferedImage image;
    private final int x;
    private final int y;
    private final double downsample;

    public CziTileReadResult(BufferedImage image, int x, int y, double downsample) {
        this.image = image;
        this.x = x;
        this.y = y;
        this.downsample = downsample;
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getDownsample() {
        return downsample;
    }
}
