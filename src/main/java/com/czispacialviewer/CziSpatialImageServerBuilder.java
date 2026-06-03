package com.czispacialviewer;

import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;

import java.awt.image.BufferedImage;
import java.net.URI;
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
        return UriImageSupport.createInstance(getClass(), 3f, builder);
    }

    @Override
    public ImageServer<BufferedImage> buildServer(URI uri, String... args) throws Exception {
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
