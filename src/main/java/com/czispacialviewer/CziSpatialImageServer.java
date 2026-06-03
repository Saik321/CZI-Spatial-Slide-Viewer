package com.czispacialviewer;

import com.czispacialviewer.backend.BioFormatsCziReaderBackend;
import com.czispacialviewer.backend.CziReaderBackend;
import com.czispacialviewer.metadata.CziPyramidLevel;
import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;
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
import java.net.URI;
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
        this.path = java.nio.file.Paths.get(uri);
        this.backend = new BioFormatsCziReaderBackend();
        performanceStats.recordBackendOpen();
        this.metadata = backend.readMetadata(path);
        this.originalMetadata = buildImageServerMetadata(metadata);
        this.tileCache = new BufferedImageLruCache(settings.getMaxCacheBytes());
    }

    public CziSpatialMetadata getSpatialMetadata() {
        return metadata;
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
        return new CziSpatialImageServerBuilder.CziSpatialServerBuilder(uri, args, originalMetadata);
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

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
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
                drawSceneRegion(g, sceneRegion, drawX, drawY, drawW, drawH);
            } catch (Exception e) {
                g.setColor(new Color(220, 235, 250));
                g.fillRect(drawX, drawY, drawW, drawH);
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

    private ImageServerMetadata buildImageServerMetadata(CziSpatialMetadata spatialMetadata) {
        ImageServerMetadata.Builder builder = new ImageServerMetadata.Builder(
                getClass(),
                uri.toString(),
                spatialMetadata.getGlobalWidth(),
                spatialMetadata.getGlobalHeight());

        builder.rgb(true).pixelType(PixelType.UINT8);
        builder.channels(defaultRgbChannels());
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

    private List<ImageChannel> defaultRgbChannels() {
        List<ImageChannel> channels = new ArrayList<>();
        channels.add(ImageChannel.getInstance("Red", ColorTools.packRGB(255, 0, 0)));
        channels.add(ImageChannel.getInstance("Green", ColorTools.packRGB(0, 255, 0)));
        channels.add(ImageChannel.getInstance("Blue", ColorTools.packRGB(0, 0, 255)));
        return channels;
    }

    private String buildCacheKey(RegionRequest request) {
        return request.getX() + ":" + request.getY() + ":" + request.getWidth() + ":" + request.getHeight()
                + ":" + request.getDownsample() + ":" + request.getZ() + ":" + request.getT()
                + ":" + settings.cacheSignature();
    }
}
