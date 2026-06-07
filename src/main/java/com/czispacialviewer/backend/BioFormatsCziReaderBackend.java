package com.czispacialviewer.backend;

import com.czispacialviewer.metadata.CziPyramidLevel;
import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSeriesInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;
import com.czispacialviewer.metadata.CziTileInfo;
import com.czispacialviewer.metadata.GlobalCoordinateMapper;
import com.czispacialviewer.util.BandedImageTools;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.gui.BufferedImageReader;
import loci.formats.meta.IMetadata;
import ome.units.UNITS;
import ome.units.quantity.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BioFormatsCziReaderBackend implements CziReaderBackend {

    private static final Logger logger = LoggerFactory.getLogger(BioFormatsCziReaderBackend.class);
    private static final Pattern FIRST_NUMBER = Pattern.compile("[-+]?\\d*\\.?\\d+(?:[Ee][-+]?\\d+)?");
    private static final int MAX_READER_POOL_SIZE = 4;

    private ImageReader reader;
    private BufferedImageReader bufferedImageReader;
    private final BlockingQueue<ReaderState> readerPool = new ArrayBlockingQueue<>(MAX_READER_POOL_SIZE);
    private final Object readerPoolLock = new Object();
    private int createdPooledReaders;

    @Override
    public String getName() {
        return "Bio-Formats";
    }

    @Override
    public String getBackendVersion() {
        Package pkg = ImageReader.class.getPackage();
        String version = pkg == null ? null : pkg.getImplementationVersion();
        return version == null ? "unknown" : version;
    }

    @Override
    public CziSpatialMetadata readMetadata(Path path) throws FormatException, IOException {
        IMetadata metadataStore = MetadataTools.createOMEXMLMetadata();
        reader = new ImageReader();
        reader.setMetadataStore(metadataStore);
        reader.setId(path.toString());
        bufferedImageReader = new BufferedImageReader(reader);

        int seriesCount = reader.getSeriesCount();
        List<CziSceneInfo> scenes = new ArrayList<>();
        List<CziSeriesInfo> allSeries = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        logger.info("Opened CZI with {} series", seriesCount);

        for (int series = 0; series < seriesCount; series++) {
            reader.setSeries(series);
            final int seriesIndex = series;
            int sizeX = reader.getSizeX();
            int sizeY = reader.getSizeY();
            if (sizeX <= 0 || sizeY <= 0) {
                CziSeriesInfo seriesInfo = new CziSeriesInfo(series, sizeX, sizeY);
                seriesInfo.setReadable(false);
                seriesInfo.setSkipReason("Invalid Bio-Formats dimensions");
                seriesInfo.setItemType("unreadable");
                allSeries.add(seriesInfo);
                warnings.add("Unreadable series " + series + " (invalid dimensions)");
                continue;
            }

            CziSceneInfo scene = new CziSceneInfo(series, series, sizeX, sizeY);
            scene.setChannelCount(reader.getSizeC());
            scene.setSeriesName(safeMetadataValue("image name", series, -1, warnings,
                    () -> metadataStore.getImageName(seriesIndex)));
            scene.setFormat(reader.getFormat());
            scene.setPixelType(FormatTools.getPixelTypeString(reader.getPixelType()));
            scene.setRgb(reader.isRGB());
            scene.setInterleaved(reader.isInterleaved());
            for (int channel = 0; channel < Math.max(1, reader.getSizeC()); channel++) {
                final int channelIndex = channel;
                String channelName = safeMetadataValue("channel name", series, channel, warnings,
                        () -> metadataStore.getChannelName(seriesIndex, channelIndex));
                scene.getChannelNames().add(channelName == null || channelName.isBlank()
                        ? "Channel " + (channel + 1)
                        : channelName);
                scene.getChannelColors().add(defaultChannelColor(channel));
            }
            var acquisitionDate = safeMetadataValue("acquisition date", series, -1, warnings,
                    () -> metadataStore.getImageAcquisitionDate(seriesIndex));
            if (acquisitionDate != null) {
                scene.setAcquisitionDate(acquisitionDate.getValue());
            }
            copyCoordinateLikeMetadata(reader.getSeriesMetadata(), scene);
            copyCoordinateLikeMetadata(reader.getGlobalMetadata(), scene);
            scene.setCompression(findMetadataValue(scene, "compression"));

            CoordinateValue stageX = findStageCoordinateMicrons(metadataStore, series, true, scene);
            CoordinateValue stageY = findStageCoordinateMicrons(metadataStore, series, false, scene);
            if (stageX == null || stageY == null) {
                warnings.add("Missing X/Y stage metadata for scene " + series);
            }
            scene.setStageXMicrons(stageX == null ? null : stageX.valueMicrons());
            scene.setStageYMicrons(stageY == null ? null : stageY.valueMicrons());
            if (stageX != null && stageY != null) {
                scene.setCoordinateSource(stageX.source() + " / " + stageY.source());
            } else if (stageX != null) {
                scene.setCoordinateSource(stageX.source());
            } else if (stageY != null) {
                scene.setCoordinateSource(stageY.source());
            }

            Length pixelSizeX = safeMetadataValue("physical pixel size X", series, -1, warnings,
                    () -> metadataStore.getPixelsPhysicalSizeX(seriesIndex));
            Length pixelSizeY = safeMetadataValue("physical pixel size Y", series, -1, warnings,
                    () -> metadataStore.getPixelsPhysicalSizeY(seriesIndex));
            Double pixelX = toMicrons(pixelSizeX);
            Double pixelY = toMicrons(pixelSizeY);
            if (pixelX == null || pixelY == null) {
                warnings.add("Missing pixel size metadata for scene " + series);
            }
            scene.setPixelSizeXMicrons(pixelX);
            scene.setPixelSizeYMicrons(pixelY);
            if (pixelSizeX != null && pixelSizeX.unit() != null) {
                scene.setUnits(pixelSizeX.unit().getSymbol());
            }

            String compression = scene.getCompression();
            if (compression != null && !"Uncompressed".equalsIgnoreCase(compression)) {
                warnings.add("Non-uncompressed CZI compression reported for series " + series + ": " + compression
                        + ". Pixel reading will be attempted through Bio-Formats.");
            }
            if (!scene.isRgb() && scene.getChannelCount() > 1) {
                warnings.add("Multichannel non-RGB series " + series
                        + " detected; preserving native channels for QuPath display where supported.");
            }

            int resolutionCount = reader.getResolutionCount();
            if (resolutionCount <= 0) {
                warnings.add("Missing pyramid levels for scene " + series);
                resolutionCount = 1;
            }

            int baseWidth = sizeX;
            int baseHeight = sizeY;
            for (int r = 0; r < resolutionCount; r++) {
                reader.setResolution(r);
                int levelWidth = reader.getSizeX();
                int levelHeight = reader.getSizeY();
                double downsample = levelWidth > 0 ? baseWidth / (double) levelWidth : 1.0;
                scene.getPyramidLevels().add(new CziPyramidLevel(r, levelWidth, levelHeight, downsample, series, r));
            }
            reader.setResolution(0);

            CziTileInfo tile = new CziTileInfo(series, 0, 0, 1.0, sizeX, sizeY);
            scene.getTiles().add(tile);

            scenes.add(scene);
            allSeries.add(toSeriesInfo(scene));

            logger.info("Series {}: size={}x{} channels={} planes={} stage=({}, {}) source={}",
                    series, sizeX, sizeY, scene.getChannelCount(), reader.getImageCount(),
                    scene.getStageXMicrons(), scene.getStageYMicrons(), scene.getCoordinateSource());
        }

        List<CziSceneInfo> groupedScenes = groupPyramidSeries(scenes, warnings);
        if (groupedScenes.size() < scenes.size()) {
            warnings.add("Bio-Formats exposed " + scenes.size() + " series; grouped same-stage resolution series into "
                    + groupedScenes.size() + " scenes.");
        }

        CziSpatialMetadata metadata = new CziSpatialMetadata(path, groupedScenes);
        metadata.setSourceSeriesCount(seriesCount);
        metadata.getAllSeries().addAll(allSeries);
        allSeries.stream()
                .filter(series -> !series.isSpatial())
                .forEach(metadata.getNonSpatialSeries()::add);
        applyRepresentativeImageMetadata(metadata, warnings);
        warnings.forEach(metadata::addWarning);
        new GlobalCoordinateMapper().apply(metadata);
        return metadata;
    }

    @Override
    public CziTileReadResult readRegion(Path path, CziSceneInfo scene, int x, int y, int width, int height, double downsample) throws FormatException, IOException {
        ReaderState state = borrowReader(path);
        try {
        CziPyramidLevel level = selectPyramidLevel(scene, downsample);
        int sourceSeries = level.getSourceSeriesIndex() >= 0 ? level.getSourceSeriesIndex() : scene.getSeriesIndex();
        double levelDownsample = level.getDownsample() > 0 ? level.getDownsample() : 1.0;

        int levelX = Math.max(0, (int)Math.floor(x / levelDownsample));
        int levelY = Math.max(0, (int)Math.floor(y / levelDownsample));
        int levelMaxX = Math.min(level.getWidth(), (int)Math.ceil((x + width) / levelDownsample));
        int levelMaxY = Math.min(level.getHeight(), (int)Math.ceil((y + height) / levelDownsample));
        int levelWidth = Math.max(1, levelMaxX - levelX);
        int levelHeight = Math.max(1, levelMaxY - levelY);

            state.reader().setSeries(sourceSeries);
            try {
                int resolution = Math.min(level.getSourceResolutionIndex(), Math.max(0, state.reader().getResolutionCount() - 1));
                state.reader().setResolution(resolution);
                if (logger.isDebugEnabled()) {
                    logger.debug("Reading CZI scene {} from series {} resolution {} for requested downsample {} (native {})",
                            scene.getSceneIndex(), sourceSeries, resolution, downsample, levelDownsample);
                }
            } catch (IllegalArgumentException ignored) {
                // Some Bio-Formats readers expose pyramid levels as separate series only.
            }

            BufferedImage image = readDisplayImage(state, scene, levelX, levelY, levelWidth, levelHeight);
        return new CziTileReadResult(image, x, y, levelDownsample);
        } finally {
            releaseReader(state);
        }
    }

    private BufferedImage readDisplayImage(ReaderState state, CziSceneInfo scene, int x, int y, int width, int height) throws FormatException, IOException {
        ImageReader activeReader = state.reader();
        BufferedImageReader activeBufferedReader = state.bufferedImageReader();
        if (activeReader.isRGB()) {
            return activeBufferedReader.openImage(0, x, y, width, height);
        }

        int pixelType = activeReader.getPixelType();
        if (pixelType != FormatTools.UINT8 && pixelType != FormatTools.UINT16) {
            logger.warn("Falling back to Bio-Formats BufferedImage conversion for unsupported multichannel pixel type {}", scene.getPixelType());
            return activeBufferedReader.openImage(0, x, y, width, height);
        }

        int channels = Math.max(1, activeReader.getSizeC());
        int dataType = pixelType == FormatTools.UINT16 ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE;
        BufferedImage image = BandedImageTools.createBandedImage(width, height, channels, dataType);
        WritableRaster raster = image.getRaster();
        for (int c = 0; c < channels; c++) {
            int plane = activeReader.getIndex(0, c, 0);
            copyChannelBytes(activeReader.openBytes(plane, x, y, width, height), raster, c, width, height, pixelType, activeReader.isLittleEndian());
        }
        return image;
    }

    private ReaderState borrowReader(Path path) throws FormatException, IOException {
        ReaderState state = readerPool.poll();
        if (state != null) {
            return state;
        }
        synchronized (readerPoolLock) {
            if (createdPooledReaders < MAX_READER_POOL_SIZE) {
                createdPooledReaders++;
                try {
                    return openReader(path);
                } catch (FormatException | IOException e) {
                    createdPooledReaders--;
                    throw e;
                }
            }
        }
        try {
            return readerPool.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for an available Bio-Formats reader.", e);
        }
    }

    private ReaderState openReader(Path path) throws FormatException, IOException {
        IMetadata metadataStore = MetadataTools.createOMEXMLMetadata();
        ImageReader imageReader = new ImageReader();
        imageReader.setMetadataStore(metadataStore);
        imageReader.setId(path.toString());
        return new ReaderState(imageReader, new BufferedImageReader(imageReader));
    }

    private void releaseReader(ReaderState state) throws IOException {
        if (state == null) {
            return;
        }
        if (!readerPool.offer(state)) {
            try {
                state.reader().close();
            } finally {
                synchronized (readerPoolLock) {
                    createdPooledReaders--;
                }
            }
        }
    }

    private void copyChannelBytes(byte[] bytes, WritableRaster raster, int channel, int width, int height, int pixelType, boolean littleEndian) {
        int bytesPerPixel = FormatTools.getBytesPerPixel(pixelType);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
            int value;
            if (bytesPerPixel == 1) {
                value = bytes[i] & 0xff;
            } else {
                int offset = i * 2;
                int b0 = bytes[offset] & 0xff;
                int b1 = bytes[offset + 1] & 0xff;
                value = littleEndian ? (b0 | (b1 << 8)) : ((b0 << 8) | b1);
            }
                raster.setSample(x, y, channel, value);
        }
        }
    }

    @Override
    public void close() throws Exception {
        if (reader != null) {
            reader.close();
        }
        ReaderState state;
        while ((state = readerPool.poll()) != null) {
            state.reader().close();
        }
    }

    CziPyramidLevel selectPyramidLevel(CziSceneInfo scene, double requestedDownsample) {
        return scene.getPyramidLevels().stream()
                .min(Comparator.comparingDouble(level -> Math.abs(level.getDownsample() - requestedDownsample)))
                .orElse(new CziPyramidLevel(0, scene.getWidth(), scene.getHeight(), 1.0, scene.getSeriesIndex()));
    }

    private Double toMicrons(Length length) {
        if (length == null) {
            return null;
        }
        try {
            return length.value(UNITS.MICROMETER).doubleValue();
        } catch (Exception e) {
            return length.value().doubleValue();
        }
    }

    private CoordinateValue findStageCoordinateMicrons(IMetadata metadataStore, int series, boolean xAxis, CziSceneInfo scene) {
        for (int plane = 0; plane < Math.max(1, reader.getImageCount()); plane++) {
            Length length;
            try {
                length = xAxis
                        ? metadataStore.getPlanePositionX(series, plane)
                        : metadataStore.getPlanePositionY(series, plane);
            } catch (RuntimeException e) {
                logger.debug("OME plane {} coordinate lookup failed for series {} plane {}: {}",
                        xAxis ? "X" : "Y", series, plane, e.getMessage());
                continue;
            }
            Double value = toMicrons(length);
            if (value != null) {
                return new CoordinateValue(value, "OME plane " + plane + " " + (xAxis ? "PositionX" : "PositionY"));
            }
        }

        Optional<Map.Entry<String, String>> candidate = scene.getRawMetadata().entrySet().stream()
                .filter(entry -> isAxisCoordinateKey(entry.getKey(), xAxis))
                .findFirst();
        return candidate
                .flatMap(entry -> parseCoordinateValue(entry.getValue())
                        .map(value -> new CoordinateValue(value, "Bio-Formats metadata: " + entry.getKey())))
                .orElse(null);
    }

    <T> T safeMetadataValue(String field, int series, int index, List<String> warnings, Supplier<T> lookup) {
        try {
            return lookup.get();
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            String indexedField = index >= 0 ? field + " " + index : field;
            warnings.add("OME metadata " + indexedField + " unavailable for series " + series
                    + " (" + e.getMessage() + "); using fallback/default value.");
            return null;
        }
    }

    private void copyCoordinateLikeMetadata(Hashtable<String, Object> source, CziSceneInfo scene) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || value == null) {
                continue;
            }
            String lower = key.toLowerCase(Locale.ROOT);
            if (lower.contains("positionx") || lower.contains("positiony")
                    || lower.contains("stagex") || lower.contains("stagey")
                    || lower.contains("compression") || lower.startsWith("positions|series")) {
                scene.getRawMetadata().putIfAbsent(key, String.valueOf(value));
            }
        }
    }

    private String findMetadataValue(CziSceneInfo scene, String keyPart) {
        String lowerKeyPart = keyPart.toLowerCase(Locale.ROOT);
        return scene.getRawMetadata().entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).contains(lowerKeyPart))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean isAxisCoordinateKey(String key, boolean xAxis) {
        String lower = key.toLowerCase(Locale.ROOT);
        String axis = xAxis ? "x" : "y";
        if (lower.contains("focus") || lower.contains("wait") || lower.contains("limit")) {
            return false;
        }
        if (!(lower.contains("position") || lower.contains("stage"))) {
            return false;
        }
        return lower.endsWith(axis)
                || lower.contains(axis + " position")
                || lower.contains("position " + axis)
                || lower.contains("position|" + axis)
                || lower.contains("position" + axis)
                || lower.contains("stage" + axis);
    }

    private Optional<Double> parseCoordinateValue(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        Matcher matcher = FIRST_NUMBER.matcher(raw);
        if (!matcher.find()) {
            return Optional.empty();
        }
        double value = Double.parseDouble(matcher.group());
        String lower = raw.toLowerCase(Locale.ROOT);
        String trimmedLower = lower.trim();
        if (lower.contains("micron") || lower.contains("micrometer") || lower.contains("um") || lower.contains("µm")) {
            return Optional.of(value);
        }
        if (lower.contains("mm")) {
            value *= 1000.0;
        } else if (lower.contains(" meter") || trimmedLower.endsWith(" m") || Math.abs(value) > 0.0 && Math.abs(value) < 1.0) {
            value *= 1_000_000.0;
        }
        return Optional.of(value);
    }

    List<CziSceneInfo> groupPyramidSeries(List<CziSceneInfo> seriesScenes, List<String> warnings) {
        Map<String, List<CziSceneInfo>> groups = new LinkedHashMap<>();
        for (CziSceneInfo scene : seriesScenes) {
            if (scene.getStageXMicrons() == null || scene.getStageYMicrons() == null) {
                warnings.add("Series " + scene.getSeriesIndex()
                        + " has no usable stage X/Y metadata and was not included in the global layout.");
                continue;
            }
            groups.computeIfAbsent(stageKey(scene), ignored -> new ArrayList<>()).add(scene);
        }

        List<CziSceneInfo> grouped = new ArrayList<>();
        int sceneIndex = 0;
        for (List<CziSceneInfo> group : groups.values()) {
            group.sort(Comparator.comparingLong(this::area).reversed());
            CziSceneInfo base = group.get(0);
            CziSceneInfo scene = copyBaseScene(sceneIndex++, base);
            scene.getPyramidLevels().clear();
            int levelIndex = 0;
            for (CziSceneInfo levelScene : group) {
                if (levelScene.getPyramidLevels().isEmpty()) {
                    double downsample = levelScene.getWidth() > 0 ? base.getWidth() / (double) levelScene.getWidth() : 1.0;
                    scene.getPyramidLevels().add(new CziPyramidLevel(levelIndex++, levelScene.getWidth(), levelScene.getHeight(),
                            downsample, levelScene.getSeriesIndex(), 0));
                    continue;
                }
                for (CziPyramidLevel sourceLevel : levelScene.getPyramidLevels()) {
                    double downsample = sourceLevel.getWidth() > 0 ? base.getWidth() / (double) sourceLevel.getWidth() : 1.0;
                    if (hasSimilarLevel(scene.getPyramidLevels(), downsample, sourceLevel.getWidth(), sourceLevel.getHeight())) {
                        continue;
                    }
                    scene.getPyramidLevels().add(new CziPyramidLevel(levelIndex++, sourceLevel.getWidth(), sourceLevel.getHeight(),
                            downsample, levelScene.getSeriesIndex(), sourceLevel.getSourceResolutionIndex()));
                }
            }
            scene.getPyramidLevels().sort(Comparator.comparingDouble(CziPyramidLevel::getDownsample));
            scene.getTiles().add(new CziTileInfo(scene.getSceneIndex(), 0, 0, 1.0, scene.getWidth(), scene.getHeight()));
            grouped.add(scene);
        }

        return grouped;
    }

    private boolean hasSimilarLevel(List<CziPyramidLevel> levels, double downsample, int width, int height) {
        return levels.stream().anyMatch(level ->
                Math.abs(level.getDownsample() - downsample) < 0.001
                        || level.getWidth() == width && level.getHeight() == height);
    }

    private CziSeriesInfo toSeriesInfo(CziSceneInfo scene) {
        CziSeriesInfo series = new CziSeriesInfo(scene.getSeriesIndex(), scene.getWidth(), scene.getHeight());
        series.setSeriesName(scene.getSeriesName());
        series.setChannelCount(scene.getChannelCount());
        series.setPixelType(scene.getPixelType());
        series.setRgb(scene.isRgb());
        series.setInterleaved(scene.isInterleaved());
        series.setCoordinateSource(scene.getCoordinateSource());
        series.setStageXMicrons(scene.getStageXMicrons());
        series.setStageYMicrons(scene.getStageYMicrons());
        boolean hasSpatialCoordinates = scene.getStageXMicrons() != null && scene.getStageYMicrons() != null;
        series.setSpatial(hasSpatialCoordinates);
        series.setItemType(classifySeries(scene, hasSpatialCoordinates));
        if (!hasSpatialCoordinates) {
            series.setSkipReason("No usable stage X/Y metadata; listed for support/contact sheet but excluded from global analysis canvas");
        }
        return series;
    }

    private void applyRepresentativeImageMetadata(CziSpatialMetadata metadata, List<String> warnings) {
        if (metadata.getScenes().isEmpty()) {
            metadata.setRgb(true);
            metadata.setPixelType("uint8");
            metadata.setChannelCount(3);
            metadata.getChannelNames().addAll(List.of("Red", "Green", "Blue"));
            metadata.getChannelColors().addAll(List.of(defaultChannelColor(0), defaultChannelColor(1), defaultChannelColor(2)));
            return;
        }

        CziSceneInfo representative = metadata.getScenes().get(0);
        metadata.setRgb(representative.isRgb());
        metadata.setPixelType(representative.getPixelType());
        metadata.setChannelCount(Math.max(1, representative.getChannelCount()));
        metadata.getChannelNames().clear();
        metadata.getChannelColors().clear();
        if (representative.getChannelNames().isEmpty()) {
            for (int i = 0; i < metadata.getChannelCount(); i++) {
                metadata.getChannelNames().add(metadata.isRgb() && i < 3 ? List.of("Red", "Green", "Blue").get(i) : "Channel " + (i + 1));
                metadata.getChannelColors().add(defaultChannelColor(i));
            }
        } else {
            metadata.getChannelNames().addAll(representative.getChannelNames());
            metadata.getChannelColors().addAll(representative.getChannelColors());
        }

        for (CziSceneInfo scene : metadata.getScenes()) {
            if (scene.isRgb() != representative.isRgb()
                    || scene.getChannelCount() != representative.getChannelCount()
                    || !String.valueOf(scene.getPixelType()).equals(String.valueOf(representative.getPixelType()))) {
                warnings.add("Mixed image types detected across spatial scenes; QuPath metadata uses scene "
                        + representative.getSceneIndex() + " as the representative display model.");
                break;
            }
        }
    }

    private String classifySeries(CziSceneInfo scene, boolean hasSpatialCoordinates) {
        String name = scene.getSeriesName() == null ? "" : scene.getSeriesName().toLowerCase(Locale.ROOT);
        String combined = name + " " + scene.getRawMetadata().keySet().toString().toLowerCase(Locale.ROOT);
        if (combined.contains("label")) {
            return "label";
        }
        if (combined.contains("macro") || combined.contains("overview")) {
            return "macro";
        }
        if (combined.contains("thumbnail") || combined.contains("thumb")) {
            return "thumbnail";
        }
        if (combined.contains("attachment")) {
            return "attachment";
        }
        if (hasSpatialCoordinates) {
            return "spatial";
        }
        long area = area(scene);
        if (area < 2_000_000L) {
            return "non_spatial_small_image";
        }
        return "non_spatial";
    }

    private CziSceneInfo copyBaseScene(int sceneIndex, CziSceneInfo source) {
        CziSceneInfo scene = new CziSceneInfo(sceneIndex, source.getSeriesIndex(), source.getWidth(), source.getHeight());
        scene.setChannelCount(source.getChannelCount());
        scene.setSeriesName(source.getSeriesName());
        scene.setFormat(source.getFormat());
        scene.setCompression(source.getCompression());
        scene.setCoordinateSource(source.getCoordinateSource());
        scene.setPixelType(source.getPixelType());
        scene.setRgb(source.isRgb());
        scene.setInterleaved(source.isInterleaved());
        scene.setAcquisitionDate(source.getAcquisitionDate());
        scene.setUnits(source.getUnits());
        scene.setStageZMicrons(source.getStageZMicrons());
        scene.setObjectiveMagnification(source.getObjectiveMagnification());
        scene.setStageXMicrons(source.getStageXMicrons());
        scene.setStageYMicrons(source.getStageYMicrons());
        scene.setPixelSizeXMicrons(source.getPixelSizeXMicrons());
        scene.setPixelSizeYMicrons(source.getPixelSizeYMicrons());
        scene.getChannelNames().addAll(source.getChannelNames());
        scene.getChannelColors().addAll(source.getChannelColors());
        scene.getRawMetadata().putAll(source.getRawMetadata());
        return scene;
    }

    private int defaultChannelColor(int channel) {
        return switch (channel % 6) {
            case 0 -> 0x00ff0000;
            case 1 -> 0x0000ff00;
            case 2 -> 0x000000ff;
            case 3 -> 0x00ffff00;
            case 4 -> 0x00ff00ff;
            default -> 0x0000ffff;
        };
    }

    private String stageKey(CziSceneInfo scene) {
        long x = Math.round(scene.getStageXMicrons() * 1000.0);
        long y = Math.round(scene.getStageYMicrons() * 1000.0);
        return x + ":" + y;
    }

    private long area(CziSceneInfo scene) {
        return (long) scene.getWidth() * (long) scene.getHeight();
    }

    private record CoordinateValue(double valueMicrons, String source) {
    }

    private record ReaderState(ImageReader reader, BufferedImageReader bufferedImageReader) {
    }
}
