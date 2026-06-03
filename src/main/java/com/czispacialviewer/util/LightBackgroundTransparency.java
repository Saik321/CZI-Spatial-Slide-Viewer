package com.czispacialviewer.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class LightBackgroundTransparency {

    private LightBackgroundTransparency() {
    }

    public static BufferedImage scaleAndMakeTransparent(BufferedImage input, int targetWidth, int targetHeight,
                                                        int threshold, int maxColorSpread) {
        int width = Math.max(1, targetWidth);
        int height = Math.max(1, targetHeight);
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(input, 0, 0, width, height, null);
        g.dispose();
        return makeTransparent(scaled, threshold, maxColorSpread);
    }

    public static BufferedImage makeTransparent(BufferedImage input, int threshold, int maxColorSpread) {
        int width = input.getWidth();
        int height = input.getHeight();
        if (width <= 0 || height <= 0) {
            return input;
        }

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = input.getRGB(x, y);
                int r = (rgb >>> 16) & 0xff;
                int g = (rgb >>> 8) & 0xff;
                int b = rgb & 0xff;
                int alpha = isLightNeutralBackground(r, g, b, threshold, maxColorSpread) ? 0 : 255;
                output.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return output;
    }

    public static boolean isLightNeutralBackground(int r, int g, int b, int threshold, int maxColorSpread) {
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return min >= threshold && max - min <= maxColorSpread;
    }
}
