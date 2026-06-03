package com.czispacialviewer.backend;

import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;

import java.nio.file.Path;

public class LibCziReaderBackend implements CziReaderBackend {

    @Override
    public String getName() {
        return "libCZI (TODO)";
    }

    @Override
    public boolean canOpen(Path path) {
        return false;
    }

    @Override
    public CziSpatialMetadata readMetadata(Path path) {
        throw new UnsupportedOperationException("libCZI backend is not implemented yet.");
    }

    @Override
    public CziTileReadResult readRegion(Path path, CziSceneInfo scene, int x, int y, int width, int height, double downsample) {
        throw new UnsupportedOperationException("libCZI backend is not implemented yet.");
    }

    @Override
    public void close() {
        // No-op for placeholder backend.
    }
}
