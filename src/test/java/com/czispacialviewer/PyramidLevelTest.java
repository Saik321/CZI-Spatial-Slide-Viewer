package com.czispacialviewer;

import com.czispacialviewer.metadata.CziPyramidLevel;
import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;
import com.czispacialviewer.metadata.GlobalCoordinateMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PyramidLevelTest {

    @Test
    void testRegularPyramidLevelsArePropagatedGlobally() {
        CziSceneInfo scene = sceneWithPosition();
        scene.getPyramidLevels().add(new CziPyramidLevel(0, 1000, 1000, 1.0, 0));
        scene.getPyramidLevels().add(new CziPyramidLevel(1, 500, 500, 2.0, 1));
        scene.getPyramidLevels().add(new CziPyramidLevel(2, 250, 250, 4.0, 2));
        scene.getPyramidLevels().add(new CziPyramidLevel(3, 125, 125, 8.0, 3));
        scene.getPyramidLevels().add(new CziPyramidLevel(4, 62, 62, 16.0, 4));

        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "pyramid.czi"), List.of(scene));
        new GlobalCoordinateMapper().apply(metadata);

        assertEquals(5, metadata.getPyramidLevels().size());
        assertEquals(16.0, metadata.getPyramidLevels().get(4).getDownsample(), 0.001);
    }

    @Test
    void testIrregularPyramidLevelsAreKept() {
        CziSceneInfo scene = sceneWithPosition();
        scene.getPyramidLevels().add(new CziPyramidLevel(0, 1000, 1000, 1.0, 0));
        scene.getPyramidLevels().add(new CziPyramidLevel(1, 256, 256, 3.9, 1));
        scene.getPyramidLevels().add(new CziPyramidLevel(2, 128, 128, 7.8, 2));

        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "irregular.czi"), List.of(scene));
        new GlobalCoordinateMapper().apply(metadata);

        assertTrue(metadata.getPyramidLevels().stream().anyMatch(level -> Math.abs(level.getDownsample() - 3.9) < 0.001));
        assertTrue(metadata.getPyramidLevels().stream().anyMatch(level -> Math.abs(level.getDownsample() - 7.8) < 0.001));
    }

    @Test
    void testMissingPyramidLevelFallsBackToOne() {
        CziSceneInfo scene = sceneWithPosition();
        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "nopyramid.czi"), List.of(scene));
        new GlobalCoordinateMapper().apply(metadata);

        assertEquals(1, metadata.getPyramidLevels().size());
        assertEquals(1.0, metadata.getPyramidLevels().get(0).getDownsample(), 0.001);
    }

    private CziSceneInfo sceneWithPosition() {
        CziSceneInfo scene = new CziSceneInfo(0, 0, 1000, 1000);
        scene.setStageXMicrons(0.0);
        scene.setStageYMicrons(0.0);
        scene.setPixelSizeXMicrons(1.0);
        scene.setPixelSizeYMicrons(1.0);
        return scene;
    }
}
