package com.czispacialviewer.util;

import com.czispacialviewer.CziSpatialViewerSettings;
import com.czispacialviewer.backend.CziReaderBackend;
import com.czispacialviewer.metadata.CziPyramidLevel;
import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSeriesInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LayoutPreviewExporter {

    private static final int MAX_DIMENSION = CziSpatialViewerSettings.getDefault().getMaxPreviewDimension();
    private static final int PRESENTATION_BAND_MAX_HEIGHT = 520;
    private static final int PRESENTATION_BAND_MIN_HEIGHT = 180;

    public static void exportPreview(CziSpatialMetadata metadata, Path outputPath) throws IOException {
        exportPreview(metadata, outputPath, null);
    }

    public static void exportPreview(CziSpatialMetadata metadata, Path outputPath, CziReaderBackend backend) throws IOException {
        Files.createDirectories(outputPath.getParent());

        int canvasWidth = metadata.getGlobalWidth();
        int canvasHeight = metadata.getGlobalHeight();
        double scale = 1.0;
        if (canvasWidth > MAX_DIMENSION || canvasHeight > MAX_DIMENSION) {
            scale = Math.min(
                    MAX_DIMENSION / (double) canvasWidth,
                    MAX_DIMENSION / (double) canvasHeight
            );
        }

        int imageWidth = Math.max(1, (int)Math.ceil(canvasWidth * scale));
        int imageHeight = Math.max(1, (int)Math.ceil(canvasHeight * scale));

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, imageWidth, imageHeight);
        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g.setColor(new Color(30, 30, 30));
        g.drawString(metadata.getInputPath().getFileName().toString(), 10, 16);
        g.drawString(metadata.getScenes().size() + " scenes, canvas " + canvasWidth + " x " + canvasHeight, 10, 30);
        drawScaleBar(g, metadata, imageWidth, imageHeight, scale);

        for (CziSceneInfo scene : metadata.getScenes()) {
            int x = (int)Math.round(scene.getGlobalX() * scale);
            int y = (int)Math.round(scene.getGlobalY() * scale);
            int w = Math.max(1, (int)Math.round(scene.getWidth() * scale));
            int h = Math.max(1, (int)Math.round(scene.getHeight() * scale));

            boolean drewThumbnail = false;
            if (backend != null) {
                try {
                    double thumbnailDownsample = Math.max(1.0, Math.max(scene.getWidth() / (double)Math.max(1, w), scene.getHeight() / (double)Math.max(1, h)));
                    BufferedImage thumbnail = backend.readRegion(metadata.getInputPath(), scene, 0, 0, scene.getWidth(), scene.getHeight(), thumbnailDownsample).getImage();
                    thumbnail = LightBackgroundTransparency.makeTransparent(thumbnail,
                            CziSpatialViewerSettings.getDefault().getTransparentBackgroundThreshold(),
                            CziSpatialViewerSettings.getDefault().getTransparentBackgroundMaxColorSpread());
                    g.drawImage(thumbnail, x, y, w, h, null);
                    drewThumbnail = true;
                } catch (Exception ignored) {
                    drewThumbnail = false;
                }
            }
            if (!drewThumbnail) {
                g.setColor(new Color(180, 200, 230));
                g.fillRect(x, y, w, h);
            }
            g.setColor(drewThumbnail ? new Color(40, 80, 140) : new Color(180, 40, 40));
            g.drawRect(x, y, Math.max(0, w - 1), Math.max(0, h - 1));
            g.drawString("Scene " + scene.getSceneIndex(), x + 3, y + 12);
        }
        g.dispose();

        ImageIO.write(image, "png", outputPath.toFile());
    }

    public static void exportHaloStylePreview(CziSpatialMetadata metadata, Path outputPath, CziReaderBackend backend) throws IOException {
        Files.createDirectories(outputPath.getParent());

        int canvasWidth = metadata.getGlobalWidth();
        int canvasHeight = metadata.getGlobalHeight();
        boolean hasPresentationItems = metadata.getNonSpatialSeries().stream().anyMatch(CziSeriesInfo::isReadable);
        double scale = 1.0;
        if (canvasWidth > MAX_DIMENSION || canvasHeight > MAX_DIMENSION) {
            scale = Math.min(
                    MAX_DIMENSION / (double) canvasWidth,
                    MAX_DIMENSION / (double) canvasHeight
            );
        }

        int tissueWidth = Math.max(1, (int)Math.ceil(canvasWidth * scale));
        int tissueHeight = Math.max(1, (int)Math.ceil(canvasHeight * scale));
        int topBandHeight = hasPresentationItems
                ? Math.min(PRESENTATION_BAND_MAX_HEIGHT, Math.max(PRESENTATION_BAND_MIN_HEIGHT, tissueHeight / 5))
                : 0;
        int imageWidth = tissueWidth;
        int imageHeight = tissueHeight + topBandHeight;

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, imageWidth, imageHeight);
        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (topBandHeight > 0) {
            drawPresentationBand(metadata, backend, g, imageWidth, topBandHeight);
        }

        g.setColor(new Color(30, 30, 30));
        g.drawString(metadata.getInputPath().getFileName().toString(), 10, Math.max(16, topBandHeight + 16));
        g.drawString(metadata.getScenes().size() + " spatial scenes, canvas " + canvasWidth + " x " + canvasHeight,
                10, Math.max(30, topBandHeight + 30));
        drawScaleBar(g, metadata, imageWidth, imageHeight, scale);

        for (CziSceneInfo scene : metadata.getScenes()) {
            int x = (int)Math.round(scene.getGlobalX() * scale);
            int y = topBandHeight + (int)Math.round(scene.getGlobalY() * scale);
            int w = Math.max(1, (int)Math.round(scene.getWidth() * scale));
            int h = Math.max(1, (int)Math.round(scene.getHeight() * scale));

            boolean drewThumbnail = false;
            if (backend != null) {
                try {
                    double thumbnailDownsample = Math.max(1.0, Math.max(scene.getWidth() / (double)Math.max(1, w), scene.getHeight() / (double)Math.max(1, h)));
                    BufferedImage thumbnail = backend.readRegion(metadata.getInputPath(), scene, 0, 0, scene.getWidth(), scene.getHeight(), thumbnailDownsample).getImage();
                    g.drawImage(thumbnail, x, y, w, h, null);
                    drewThumbnail = true;
                } catch (Exception ignored) {
                    drewThumbnail = false;
                }
            }
            if (!drewThumbnail) {
                g.setColor(new Color(180, 200, 230));
                g.fillRect(x, y, w, h);
                g.setColor(new Color(180, 40, 40));
                g.drawRect(x, y, Math.max(0, w - 1), Math.max(0, h - 1));
            }
        }
        g.dispose();

        ImageIO.write(image, "png", outputPath.toFile());
    }

    private static void drawPresentationBand(CziSpatialMetadata metadata, CziReaderBackend backend, Graphics2D g,
                                             int imageWidth, int topBandHeight) {
        int margin = 14;
        int gap = 20;
        int x = margin;
        int usableHeight = Math.max(1, topBandHeight - margin * 2);

        for (CziSeriesInfo series : metadata.getNonSpatialSeries()) {
            if (!series.isReadable()) {
                continue;
            }
            int targetHeight = "label".equals(series.getItemType())
                    ? usableHeight
                    : Math.max(1, Math.min(usableHeight, (int)Math.round(usableHeight * 0.55)));
            int targetWidth = Math.max(1, (int)Math.round(targetHeight * (series.getWidth() / (double)Math.max(1, series.getHeight()))));
            if (x + targetWidth > imageWidth - margin) {
                targetWidth = Math.max(1, imageWidth - margin - x);
                targetHeight = Math.max(1, (int)Math.round(targetWidth * (series.getHeight() / (double)Math.max(1, series.getWidth()))));
            }
            int y = "label".equals(series.getItemType()) ? margin : margin + Math.max(0, (usableHeight - targetHeight) / 6);
            boolean drew = false;
            boolean safeRead = ((long) series.getWidth() * (long) series.getHeight()) <= 3_000_000L;
            if (backend != null && safeRead) {
                try {
                    CziSceneInfo readScene = seriesToScene(series);
                    double downsample = Math.max(1.0, Math.max(series.getWidth() / (double)Math.max(1, targetWidth), series.getHeight() / (double)Math.max(1, targetHeight)));
                    BufferedImage attachment = backend.readRegion(metadata.getInputPath(), readScene, 0, 0, series.getWidth(), series.getHeight(), downsample).getImage();
                    attachment = autoContrastIfDark(attachment);
                    g.drawImage(attachment, x, y, targetWidth, targetHeight, null);
                    drew = true;
                } catch (Throwable ignored) {
                    drew = false;
                }
            }
            if (!drew) {
                g.setColor(new Color(220, 225, 230));
                g.fillRect(x, y, targetWidth, targetHeight);
            }
            g.setColor(new Color(140, 90, 20));
            g.drawRect(x, y, Math.max(0, targetWidth - 1), Math.max(0, targetHeight - 1));
            x += targetWidth + gap;
            if (x >= imageWidth - margin) {
                break;
            }
        }
    }

    public static void exportContactSheet(CziSpatialMetadata metadata, Path outputPath, CziReaderBackend backend) throws IOException {
        Files.createDirectories(outputPath.getParent());
        int thumb = CziSpatialViewerSettings.getDefault().getThumbnailSize();
        int itemCount = metadata.getAllSeries().isEmpty() ? metadata.getScenes().size() : metadata.getAllSeries().size();
        int columns = Math.max(1, (int)Math.ceil(Math.sqrt(Math.max(1, itemCount))));
        int rows = Math.max(1, (int)Math.ceil(itemCount / (double)columns));
        int labelHeight = 34;
        BufferedImage image = new BufferedImage(columns * thumb, rows * (thumb + labelHeight), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        Map<Integer, CziSceneInfo> seriesToSpatialScene = buildSeriesToSpatialScene(metadata);

        int index = 0;
        if (metadata.getAllSeries().isEmpty()) {
            for (CziSceneInfo scene : metadata.getScenes()) {
                drawContactSheetItem(metadata, backend, g, thumb, labelHeight, columns, index++, sceneToSeries(scene), scene);
            }
        } else {
            for (CziSeriesInfo series : metadata.getAllSeries()) {
                CziSceneInfo readScene = seriesToSpatialScene.getOrDefault(series.getSeriesIndex(), seriesToScene(series));
                drawContactSheetItem(metadata, backend, g, thumb, labelHeight, columns, index++, series, readScene);
            }
        }
        g.dispose();
        ImageIO.write(image, "png", outputPath.toFile());
    }

    private static void drawContactSheetItem(CziSpatialMetadata metadata, CziReaderBackend backend, Graphics2D g,
                                             int thumb, int labelHeight, int columns, int index, CziSeriesInfo series, CziSceneInfo scene) {
            int col = index % columns;
            int row = index / columns;
            int x = col * thumb;
            int y = row * (thumb + labelHeight);
            boolean drew = false;
            boolean safeStandaloneRead = series.isSpatial() || ((long) series.getWidth() * (long) series.getHeight()) <= 2_000_000L;
            if (backend != null && series.isReadable() && safeStandaloneRead) {
                try {
                    double downsample = Math.max(1.0, Math.max(series.getWidth(), series.getHeight()) / (double)thumb);
                    BufferedImage region = backend.readRegion(metadata.getInputPath(), scene, 0, 0, scene.getWidth(), scene.getHeight(), downsample).getImage();
                    if (!series.isSpatial()) {
                        region = autoContrastIfDark(region);
                    }
                    int drawW = series.getWidth() >= series.getHeight() ? thumb : Math.max(1, (int)Math.round(thumb * series.getWidth() / (double)series.getHeight()));
                    int drawH = series.getHeight() >= series.getWidth() ? thumb : Math.max(1, (int)Math.round(thumb * series.getHeight() / (double)series.getWidth()));
                    g.drawImage(region, x + (thumb - drawW) / 2, y + (thumb - drawH) / 2, drawW, drawH, null);
                    drew = true;
                } catch (Throwable ignored) {
                    drew = false;
                }
            }
            if (!drew) {
                g.setColor(new Color(180, 200, 230));
                g.fillRect(x, y, thumb, thumb);
                g.setColor(new Color(180, 40, 40));
                g.drawRect(x, y, thumb - 1, thumb - 1);
            }
            g.setColor(series.isSpatial() ? new Color(40, 80, 140) : new Color(140, 90, 20));
            g.drawRect(x, y, thumb - 1, thumb - 1);
            g.setColor(Color.BLACK);
            g.drawString("Series " + series.getSeriesIndex() + " " + series.getItemType(), x + 4, y + thumb + 13);
            String status = series.isSpatial() ? "spatial" : "skipped";
            g.drawString(status + " " + series.getWidth() + "x" + series.getHeight(), x + 4, y + thumb + 27);
    }

    private static Map<Integer, CziSceneInfo> buildSeriesToSpatialScene(CziSpatialMetadata metadata) {
        Map<Integer, CziSceneInfo> result = new HashMap<>();
        for (CziSceneInfo scene : metadata.getScenes()) {
            result.put(scene.getSeriesIndex(), scene);
            for (CziPyramidLevel level : scene.getPyramidLevels()) {
                if (level.getSourceSeriesIndex() >= 0) {
                    result.put(level.getSourceSeriesIndex(), scene);
                }
            }
        }
        return result;
    }

    private static CziSeriesInfo sceneToSeries(CziSceneInfo scene) {
        CziSeriesInfo series = new CziSeriesInfo(scene.getSeriesIndex(), scene.getWidth(), scene.getHeight());
        series.setSpatial(true);
        series.setReadable(true);
        series.setItemType("spatial");
        return series;
    }

    private static CziSceneInfo seriesToScene(CziSeriesInfo series) {
        CziSceneInfo scene = new CziSceneInfo(series.getSeriesIndex(), series.getSeriesIndex(), series.getWidth(), series.getHeight());
        scene.setChannelCount(series.getChannelCount());
        scene.setPixelType(series.getPixelType());
        scene.setRgb(series.isRgb());
        scene.setInterleaved(series.isInterleaved());
        scene.getPyramidLevels().add(new CziPyramidLevel(0, series.getWidth(), series.getHeight(), 1.0, series.getSeriesIndex()));
        return scene;
    }

    private static BufferedImage autoContrastIfDark(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();
        if (width <= 0 || height <= 0) {
            return input;
        }

        int minR = 255;
        int minG = 255;
        int minB = 255;
        int maxR = 0;
        int maxG = 0;
        int maxB = 0;
        long luminanceSum = 0;
        int samples = 0;
        int stepX = Math.max(1, width / 256);
        int stepY = Math.max(1, height / 256);
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int rgb = input.getRGB(x, y);
                int r = (rgb >>> 16) & 0xff;
                int g = (rgb >>> 8) & 0xff;
                int b = rgb & 0xff;
                minR = Math.min(minR, r);
                minG = Math.min(minG, g);
                minB = Math.min(minB, b);
                maxR = Math.max(maxR, r);
                maxG = Math.max(maxG, g);
                maxB = Math.max(maxB, b);
                luminanceSum += (r * 30L + g * 59L + b * 11L) / 100L;
                samples++;
            }
        }

        double averageLuminance = samples == 0 ? 255.0 : luminanceSum / (double)samples;
        int maxRange = Math.max(maxR - minR, Math.max(maxG - minG, maxB - minB));
        if (averageLuminance >= 45.0 || maxRange < 8) {
            return input;
        }

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = input.getRGB(x, y);
                int r = stretch((rgb >>> 16) & 0xff, minR, maxR);
                int g = stretch((rgb >>> 8) & 0xff, minG, maxG);
                int b = stretch(rgb & 0xff, minB, maxB);
                output.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return output;
    }

    private static int stretch(int value, int min, int max) {
        if (max <= min) {
            return value;
        }
        int stretched = 12 + (int)Math.round((value - min) * 232.0 / (max - min));
        return Math.max(0, Math.min(255, stretched));
    }

    private static void drawScaleBar(Graphics2D g, CziSpatialMetadata metadata, int imageWidth, int imageHeight, double scale) {
        if (metadata.getPixelSizeXMicrons() == null || metadata.getPixelSizeXMicrons() <= 0) {
            return;
        }
        double micronsPerPreviewPixel = metadata.getPixelSizeXMicrons() / Math.max(scale, 1.0e-9);
        double targetMicrons = 5_000.0;
        int barPixels = (int)Math.round(targetMicrons / micronsPerPreviewPixel);
        if (barPixels < 20) {
            targetMicrons = 10_000.0;
            barPixels = (int)Math.round(targetMicrons / micronsPerPreviewPixel);
        }
        if (barPixels <= 0 || barPixels > imageWidth / 2) {
            return;
        }
        int x = Math.max(12, imageWidth - barPixels - 24);
        int y = Math.max(36, imageHeight - 28);
        g.setColor(new Color(255, 255, 255, 220));
        g.fillRect(x - 8, y - 18, barPixels + 16, 28);
        g.setColor(Color.BLACK);
        g.fillRect(x, y, barPixels, 4);
        String label = targetMicrons >= 1000.0 ? (int)(targetMicrons / 1000.0) + " mm" : (int)targetMicrons + " um";
        g.drawString(label, x, y - 4);
    }
}
