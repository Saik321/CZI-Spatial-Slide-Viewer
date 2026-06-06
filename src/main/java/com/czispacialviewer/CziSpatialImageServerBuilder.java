package com.czispacialviewer;

import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class CziSpatialImageServerBuilder implements ImageServerBuilder<BufferedImage> {

    @Override
    public UriImageSupport<BufferedImage> checkImageSupport(URI uri, String... args) {
        if (!isCzi(uri)) {
            return UriImageSupport.createInstance(getClass(), 0f, Collections.emptyList());
        }
        var builder = new CziSpatialServerBuilder(uri, args, null);
        return UriImageSupport.createInstance(getClass(), 4.5f, builder);
    }

    @Override
    public ImageServer<BufferedImage> buildServer(URI uri, String... args) throws Exception {
        validateUri(uri);
        return new CziSpatialImageServer(uri, args);
    }

    @Override
    public String getName() {
        return "CZI Spatial Viewer";
    }

    @Override
    public String getDescription() {
        return "Coordinate-correct CZI layout viewer (Bio-Formats metadata)";
    }

    @Override
    public Class<BufferedImage> getImageType() {
        return BufferedImage.class;
    }

    private boolean isCzi(URI uri) {
        String path = uri.getPath();
        if (path == null) {
            return false;
        }
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".czi") || lower.endsWith(".czis");
    }

    private void validateUri(URI uri) throws IOException {
        if (uri == null) {
            throw new IOException("CZI Spatial Viewer cannot open a null URI.");
        }
        if (!isCzi(uri)) {
            throw new IOException("CZI Spatial Viewer only supports .czi/.czis files, not: " + uri);
        }
        if (uri.getScheme() != null && !"file".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("CZI Spatial Viewer currently supports local file URIs only: " + uri);
        }
        try {
            Path path = Path.of(uri);
            if (!Files.exists(path)) {
                throw new IOException("CZI file does not exist: " + path);
            }
            if (!Files.isReadable(path)) {
                throw new IOException("CZI file is not readable: " + path);
            }
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid CZI file URI: " + uri, e);
        }
    }

    public static class CziSpatialServerBuilder implements ImageServerBuilder.ServerBuilder<BufferedImage> {

        private final URI uri;
        private final String[] args;

        public CziSpatialServerBuilder(URI uri, String[] args, ImageServerMetadata metadata) {
            this.uri = uri;
            this.args = args == null ? new String[0] : args.clone();
        }

        @Override
        public ImageServer<BufferedImage> build() throws Exception {
            return new CziSpatialImageServer(uri, args);
        }

        @Override
        public Collection<URI> getURIs() {
            return Collections.singletonList(uri);
        }

        @Override
        public ServerBuilder<BufferedImage> updateURIs(Map<URI, URI> updateMap) {
            URI updated = updateMap.getOrDefault(uri, uri);
            return new CziSpatialServerBuilder(updated, args, null);
        }
    }
}
