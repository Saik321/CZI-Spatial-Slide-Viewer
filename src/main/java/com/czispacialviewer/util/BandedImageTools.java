package com.czispacialviewer.util;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

public final class BandedImageTools {

    private BandedImageTools() {
    }

    public static BufferedImage createBandedImage(int width, int height, int bands, int dataType) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        int safeBands = Math.max(1, bands);
        SampleModel sampleModel = new BandedSampleModel(dataType, safeWidth, safeHeight, safeBands);
        var dataBuffer = dataType == DataBuffer.TYPE_USHORT
                ? new DataBufferUShort(safeWidth * safeHeight, safeBands)
                : new DataBufferByte(safeWidth * safeHeight, safeBands);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, null);
        ColorModel colorModel = new ComponentColorModel(
                new MultiBandColorSpace(safeBands),
                false,
                false,
                Transparency.OPAQUE,
                dataType);
        return new BufferedImage(colorModel, raster, false, null);
    }

    public static void copyScaledBands(BufferedImage source, BufferedImage target, int targetX, int targetY, int targetWidth, int targetHeight) {
        int bands = Math.min(source.getRaster().getNumBands(), target.getRaster().getNumBands());
        WritableRaster targetRaster = target.getRaster();
        Raster sourceRaster = source.getRaster();
        for (int ty = 0; ty < targetHeight; ty++) {
            int y = targetY + ty;
            if (y < 0 || y >= target.getHeight()) {
                continue;
            }
            int sy = Math.min(source.getHeight() - 1, Math.max(0, (int)Math.floor(ty * source.getHeight() / (double)Math.max(1, targetHeight))));
            for (int tx = 0; tx < targetWidth; tx++) {
                int x = targetX + tx;
                if (x < 0 || x >= target.getWidth()) {
                    continue;
                }
                int sx = Math.min(source.getWidth() - 1, Math.max(0, (int)Math.floor(tx * source.getWidth() / (double)Math.max(1, targetWidth))));
                for (int b = 0; b < bands; b++) {
                    targetRaster.setSample(x, y, b, sourceRaster.getSample(sx, sy, b));
                }
            }
        }
    }

    private static final class MultiBandColorSpace extends ColorSpace {

        private final int components;

        private MultiBandColorSpace(int components) {
            super(ColorSpace.TYPE_GRAY, components);
            this.components = components;
        }

        @Override
        public float[] toRGB(float[] colorvalue) {
            float value = colorvalue.length == 0 ? 0f : colorvalue[0];
            return new float[]{value, value, value};
        }

        @Override
        public float[] fromRGB(float[] rgbvalue) {
            float value = rgbvalue.length == 0 ? 0f : rgbvalue[0];
            float[] result = new float[components];
            for (int i = 0; i < components; i++) {
                result[i] = value;
            }
            return result;
        }

        @Override
        public float[] toCIEXYZ(float[] colorvalue) {
            return ColorSpace.getInstance(ColorSpace.CS_GRAY).toCIEXYZ(new float[]{colorvalue.length == 0 ? 0f : colorvalue[0]});
        }

        @Override
        public float[] fromCIEXYZ(float[] colorvalue) {
            float gray = ColorSpace.getInstance(ColorSpace.CS_GRAY).fromCIEXYZ(colorvalue)[0];
            float[] result = new float[components];
            for (int i = 0; i < components; i++) {
                result[i] = gray;
            }
            return result;
        }
    }
}
