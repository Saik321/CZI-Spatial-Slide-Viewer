package com.czispacialviewer.backend;

import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;

import java.nio.file.Path;

public interface CziReaderBackend extends AutoCloseable {

    String getName();

    default String getBackendVersion() {
        return "unknown";
    }

    default boolean canOpen(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String lower = path.getFileName().toString().toLowerCase();
        return lower.endsWith(".czi") || lower.endsWith(".czis");
    }

    default void open(Path path) throws Exception {
        readMetadata(path);
    }

    CziSpatialMetadata readMetadata(Path path) throws Exception;

    CziTileReadResult readRegion(Path path, CziSceneInfo scene, int x, int y, int width, int height, double downsample) throws Exception;

    @Override
    void close() throws Exception;
}
