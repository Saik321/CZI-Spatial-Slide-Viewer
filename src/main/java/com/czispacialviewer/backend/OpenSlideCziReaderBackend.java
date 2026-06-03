package com.czispacialviewer.backend;

import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;

import java.nio.file.Path;

public class OpenSlideCziReaderBackend implements CziReaderBackend {

    @Override
    public String getName() {
        return "OpenSlide (TODO)";
    }

    @Override
    public CziSpatialMetadata readMetadata(Path path) {
        // TODO: Add OpenSlide/libCZI metadata reader implementation
        throw new UnsupportedOperationException("OpenSlide CZI backend is not implemented yet.");
    }

    @Override
    public CziTileReadResult readRegion(Path path, CziSceneInfo scene, int x, int y, int width, int height, double downsample) {
        // TODO: Add OpenSlide/libCZI pixel reader implementation
        throw new UnsupportedOperationException("OpenSlide CZI backend is not implemented yet.");
    }

    @Override
    public void close() {
        // No-op for now
    }
}
