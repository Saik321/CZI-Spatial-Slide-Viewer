package com.czispacialviewer;

import com.czispacialviewer.backend.BioFormatsCziReaderBackend;
import com.czispacialviewer.backend.CziReaderBackend;
import com.czispacialviewer.metadata.CziPyramidLevel;
import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;
import com.czispacialviewer.util.BandedImageTools;
import com.czispacialviewer.util.BufferedImageLruCache;
import com.czispacialviewer.util.LightBackgroundTransparency;
import com.czispacialviewer.util.PerformanceStats;
import com.czispacialviewer.util.RegionIntersection;
import qupath.lib.common.ColorTools;
import qupath.lib.images.servers.AbstractImageServer;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;
import qupath.lib.regions.RegionRequest;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CziSpatialImageServer extends AbstractImageServer<BufferedImage> {

    private final URI uri;
    private final String[] args;
    private final java.nio.file.Path path;
    private final CziReaderBackend backend;
    private final CziSpatialMetadata metadata;
    private final ImageServerMetadata originalMetadata;
    private final CziSpatialViewerSettings settings = CziSpatialViewerSettings.getDefault();
    private final BufferedImageLruCache tileCache;
    private final PerformanceStats performanceStats = new PerformanceStats();

    public CziSpatialImageServer(URI uri, String... args) throws Exception {
        super(BufferedImage.class);
        this.uri = uri;
        this.args = args == null ? new String[0] : args.clone();
        this.path = resolveLocalPath(uri);
        this.backend = new BioFormatsCziReaderBackend();
        performanceStats.recordBackendOpen();
        try {
            this.metadata = backend.readMetadata(path);
        } catch (Exception e) {
            throw new IOException("Bio-Formats could not read spatial CZI metadata from " + path
                    + ": " + e.getMessage(), e);
        }
        if (metadata.getScenes().isEmpty()) {
            throw new IOException("No spatial CZI scenes with usable stage coordinates were found in " + path + ".");
        }
        this.originalMetadata = buildImageServerMetadata(uri, metadata);
        this.tileCache = new BufferedImageLruCache(settings.getMaxCacheBytes());
    }

    public CziSpatialMetadata getSpatialMetadata() {
        return metadata;
    }

    private java.nio.file.Path resolveLocalPath(URI uri) throws IOException {
        if (uri == null) {
            throw new IOException("CZI Spatial Viewer cannot open a null URI.");
        }
        if (uri.getScheme() != null && !"file".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("CZI Spatial Viewer currently supports local file URIs only: " + uri);
        }
        try {
            java.nio.file.Path resolved = java.nio.file.Paths.get(uri);
            if (!Files.exists(resolved)) {
                throw new IOException("CZI file does not exist: " + resolved);
            }
            if (!Files.isReadable(resolved)) {
                throw new IOException("CZI file is not readable: " + resolved);
            }
            return resolved;
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid CZI file URI: " + uri, e);
        }
    }

    public PerformanceStats getPerformanceStats() {
        return performanceStats;
    }

    @Override
    public String getServerType() {
        return "CZI Spatial Viewer (Bio-Formats)";
    }

    @Override
    public Collection<URI> getURIs() {
        return Collections.singletonList(uri);
    }

    @Override
    protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
        return CziSpatialImageServerBuilder.createSerializableBuilder(uri, originalMetadata, args);
    }

    @Override
    protected String createID() {
        return getClass().getName() + ":" + uri + ":" + String.join(",", args);
    }

    @Override
    public ImageServerMetadata getOriginalMetadata() {
        return originalMetadata;
    }

    @Override
    public BufferedImage readRegion(RegionRequest request) {
        long outputPixels = (long)Math.ceil(request.getWidth() / request.getDownsample())
                * (long)Math.ceil(request.getHeight() / request.getDownsample());
        if (outputPixels > settings.getMaxOutputPixels()) {
            throw new IllegalArgumentException("Requested output region is too large: " + outputPixels
                    + " pixels. Reduce tile size or increase downsample.");
        }

        String cacheKey = buildCacheKey(request);
        BufferedImage cached = tileCache.get(cacheKey);
        if (cached != null) {
            performanceStats.recordCacheHit();
            return cached;
        }
        performanceStats.recordCacheMiss();
        long start = System.nanoTime();

        int width = Math.max(1, (int)Math.ceil(request.getWidth() / request.getDownsample()));
        int height = Math.max(1, (int)Math.ceil(request.getHeight() / request.getDownsample()));
        double downsample = request.getDownsample();
        performanceStats.recordPyramidLevel(downsample);

        BufferedImage image = createOutputImage(width, height);
        Graphics2D g = image.createGraphics();
        if (metadata.isRgb()) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int intersectingScenes = 0;
        for (CziSceneInfo scene : metadata.getScenes()) {
            var intersection = RegionIntersection.intersection(
                    scene.getGlobalX(), scene.getGlobalY(), scene.getWidth(), scene.getHeight(),
                    request.getX(), request.getY(), request.getWidth(), request.getHeight());
            if (intersection == null) {
                continue;
            }
            intersectingScenes++;
            int sceneX = Math.max(0, (int)Math.floor(intersection.getX() - scene.getGlobalX()));
            int sceneY = Math.max(0, (int)Math.floor(intersection.getY() - scene.getGlobalY()));
            int sceneWidth = Math.min(scene.getWidth() - sceneX, intersection.getWidth());
            int sceneHeight = Math.min(scene.getHeight() - sceneY, intersection.getHeight());
            if (sceneWidth <= 0 || sceneHeight <= 0) {
                continue;
            }

            int drawX = (int)Math.round((intersection.getX() - request.getX()) / downsample);
            int drawY = (int)Math.round((intersection.getY() - request.getY()) / downsample);
            int drawW = Math.max(1, (int)Math.round(sceneWidth / downsample));
            int drawH = Math.max(1, (int)Math.round(sceneHeight / downsample));

            try {
                BufferedImage sceneRegion = backend.readRegion(path, scene, sceneX, sceneY, sceneWidth, sceneHeight, downsample).getImage();
                if (metadata.isRgb()) {
                    drawSceneRegion(g, sceneRegion, drawX, drawY, drawW, drawH);
                } else {
                    BandedImageTools.copyScaledBands(sceneRegion, image, drawX, drawY, drawW, drawH);
                }
            } catch (Exception e) {
                g.setColor(metadata.isRgb() ? new Color(220, 235, 250) : Color.BLACK);
                if (metadata.isRgb()) {
                    g.fillRect(drawX, drawY, drawW, drawH);
                }
                g.setColor(new Color(180, 40, 40));
                g.drawRect(drawX, drawY, Math.max(0, drawW - 1), Math.max(0, drawH - 1));
            }

            if (settings.isOverlaySceneBoundaries()) {
                g.setColor(new Color(40, 80, 140));
                g.drawRect(drawX, drawY, Math.max(0, drawW - 1), Math.max(0, drawH - 1));
            }
            if (settings.isOverlaySceneLabels()) {
                g.setColor(new Color(20, 20, 20));
                g.drawString("Scene " + scene.getSceneIndex(), drawX + 4, drawY + 14);
            }
            if (settings.isOverlayTileBoundaries()) {
                g.setColor(new Color(140, 140, 140));
                g.drawRect(0, 0, Math.max(0, width - 1), Math.max(0, height - 1));
            }
        }
        if (settings.isOverlayCoordinateGrid()) {
            drawCoordinateGrid(g, request, downsample, width, height);
        }
        g.dispose();

        performanceStats.recordRequest(System.nanoTime() - start, intersectingScenes);
        tileCache.put(cacheKey, image);
        performanceStats.updatePeakEstimatedCacheBytes(tileCache.getEstimatedBytes());
        return image;
    }

    private BufferedImage createOutputImage(int width, int height) {
        if (metadata.isRgb()) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }
        int dataType = toDataBufferType(metadata.getPixelType());
        return BandedImageTools.createBandedImage(width, height, metadata.getChannelCount(), dataType);
    }

    private void drawSceneRegion(Graphics2D g, BufferedImage sceneRegion, int drawX, int drawY, int drawW, int drawH) {
        if (!settings.isTransparentLightBackgroundInOverlaps()) {
            g.drawImage(sceneRegion, drawX, drawY, drawW, drawH, null);
            return;
        }

        BufferedImage transparent = LightBackgroundTransparency.scaleAndMakeTransparent(
                sceneRegion,
                drawW,
                drawH,
                settings.getTransparentBackgroundThreshold(),
                settings.getTransparentBackgroundMaxColorSpread());
        g.drawImage(transparent, drawX, drawY, null);
    }

    private void drawCoordinateGrid(Graphics2D g, RegionRequest request, double downsample, int width, int height) {
        int spacing = 10_000;
        g.setColor(new Color(0, 0, 0, 45));
        int firstX = (int)(Math.floor(request.getX() / (double)spacing) * spacing);
        for (int x = firstX; x < request.getX() + request.getWidth(); x += spacing) {
            int drawX = (int)Math.round((x - request.getX()) / downsample);
            if (drawX >= 0 && drawX < width) {
                g.drawLine(drawX, 0, drawX, height);
            }
        }
        int firstY = (int)(Math.floor(request.getY() / (double)spacing) * spacing);
        for (int y = firstY; y < request.getY() + request.getHeight(); y += spacing) {
            int drawY = (int)Math.round((y - request.getY()) / downsample);
            if (drawY >= 0 && drawY < height) {
                g.drawLine(0, drawY, width, drawY);
            }
        }
    }

    @Override
    public void close() throws Exception {
        backend.close();
    }

    static ImageServerMetadata buildImageServerMetadata(URI uri, CziSpatialMetadata spatialMetadata) {
        ImageServerMetadata.Builder builder = new ImageServerMetadata.Builder(
                CziSpatialImageServer.class,
                uri.toString(),
                spatialMetadata.getGlobalWidth(),
                spatialMetadata.getGlobalHeight());

        builder.rgb(spatialMetadata.isRgb()).pixelType(toPixelType(spatialMetadata.getPixelType()));
        builder.channels(spatialMetadata.isRgb() ? defaultRgbChannels() : metadataChannels(spatialMetadata));
        builder.preferredTileSize(256, 256);

        if (spatialMetadata.getPixelSizeXMicrons() != null && spatialMetadata.getPixelSizeYMicrons() != null) {
            builder.pixelSizeMicrons(spatialMetadata.getPixelSizeXMicrons(), spatialMetadata.getPixelSizeYMicrons());
        }

        var levelBuilder = new ImageServerMetadata.ImageResolutionLevel.Builder(
                spatialMetadata.getGlobalWidth(),
                spatialMetadata.getGlobalHeight());
        List<CziPyramidLevel> levels = spatialMetadata.getPyramidLevels();
        if (levels.isEmpty()) {
            levelBuilder.addLevelByDownsample(1.0);
        } else {
            for (CziPyramidLevel level : levels) {
                levelBuilder.addLevelByDownsample(level.getDownsample());
            }
        }
        builder.levels(levelBuilder.build());

        return builder.build();
    }

    private static List<ImageChannel> defaultRgbChannels() {
        List<ImageChannel> channels = new ArrayList<>();
        channels.add(ImageChannel.getInstance("Red", ColorTools.packRGB(255, 0, 0)));
        channels.add(ImageChannel.getInstance("Green", ColorTools.packRGB(0, 255, 0)));
        channels.add(ImageChannel.getInstance("Blue", ColorTools.packRGB(0, 0, 255)));
        return channels;
    }

    private static List<ImageChannel> metadataChannels(CziSpatialMetadata spatialMetadata) {
        List<ImageChannel> channels = new ArrayList<>();
        for (int i = 0; i < Math.max(1, spatialMetadata.getChannelCount()); i++) {
            String name = i < spatialMetadata.getChannelNames().size()
                    ? spatialMetadata.getChannelNames().get(i)
                    : "Channel " + (i + 1);
            Integer color = i < spatialMetadata.getChannelColors().size()
                    ? spatialMetadata.getChannelColors().get(i)
                    : ImageChannel.getDefaultChannelColor(i);
            channels.add(ImageChannel.getInstance(name, color));
        }
        return channels;
    }

    private static PixelType toPixelType(String pixelType) {
        if (pixelType == null) {
            return PixelType.UINT8;
        }
        return switch (pixelType.toLowerCase(java.util.Locale.ROOT)) {
            case "uint16" -> PixelType.UINT16;
            case "int16" -> PixelType.INT16;
            case "uint32" -> PixelType.UINT32;
            case "int32" -> PixelType.INT32;
            case "float" -> PixelType.FLOAT32;
            case "double" -> PixelType.FLOAT64;
            case "int8" -> PixelType.INT8;
            default -> PixelType.UINT8;
        };
    }

    private static int toDataBufferType(String pixelType) {
        PixelType type = toPixelType(pixelType);
        return type == PixelType.UINT16 || type == PixelType.INT16
                ? DataBuffer.TYPE_USHORT
                : DataBuffer.TYPE_BYTE;
    }

    private String buildCacheKey(RegionRequest request) {
        return request.getX() + ":" + request.getY() + ":" + request.getWidth() + ":" + request.getHeight()
                + ":" + request.getDownsample() + ":" + request.getZ() + ":" + request.getT()
                + ":" + settings.cacheSignature();
    }
}
