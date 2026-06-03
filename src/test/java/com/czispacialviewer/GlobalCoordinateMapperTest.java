package com.czispacialviewer;

import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;
import com.czispacialviewer.metadata.GlobalCoordinateMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalCoordinateMapperTest {

    @Test
    void testNormalizationAndBoundsPreserveGaps() {
        CziSceneInfo scene1 = new CziSceneInfo(0, 0, 100, 80);
        scene1.setStageXMicrons(0.0);
        scene1.setStageYMicrons(0.0);
        scene1.setPixelSizeXMicrons(1.0);
        scene1.setPixelSizeYMicrons(1.0);

        CziSceneInfo scene2 = new CziSceneInfo(1, 1, 50, 80);
        scene2.setStageXMicrons(150.0);
        scene2.setStageYMicrons(0.0);
        scene2.setPixelSizeXMicrons(1.0);
        scene2.setPixelSizeYMicrons(1.0);

        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "test.czi"), List.of(scene1, scene2));
        new GlobalCoordinateMapper().apply(metadata);

        assertEquals(0.0, scene1.getGlobalX(), 0.001);
        assertEquals(150.0, scene2.getGlobalX(), 0.001);
        assertEquals(200, metadata.getGlobalWidth());
        assertEquals(80, metadata.getGlobalHeight());

        double gap = scene2.getGlobalX() - (scene1.getGlobalX() + scene1.getWidth());
        assertEquals(50.0, gap, 0.001);
    }

    @Test
    void testOverlapWarning() {
        CziSceneInfo scene1 = new CziSceneInfo(0, 0, 100, 100);
        scene1.setStageXMicrons(0.0);
        scene1.setStageYMicrons(0.0);
        scene1.setPixelSizeXMicrons(1.0);
        scene1.setPixelSizeYMicrons(1.0);

        CziSceneInfo scene2 = new CziSceneInfo(1, 1, 100, 100);
        scene2.setStageXMicrons(50.0);
        scene2.setStageYMicrons(0.0);
        scene2.setPixelSizeXMicrons(1.0);
        scene2.setPixelSizeYMicrons(1.0);

        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "test.czi"), List.of(scene1, scene2));
        new GlobalCoordinateMapper().apply(metadata);

        assertTrue(metadata.getWarnings().stream().anyMatch(w -> w.contains("Overlapping scenes")));
    }

    @Test
    void testPhysicalCoordinatesConvertToGlobalPixelsUsingCalibration() {
        CziSceneInfo scene1 = new CziSceneInfo(0, 0, 100, 100);
        scene1.setStageXMicrons(1000.0);
        scene1.setStageYMicrons(2000.0);
        scene1.setPixelSizeXMicrons(0.5);
        scene1.setPixelSizeYMicrons(0.25);

        CziSceneInfo scene2 = new CziSceneInfo(1, 1, 100, 100);
        scene2.setStageXMicrons(1050.0);
        scene2.setStageYMicrons(2050.0);
        scene2.setPixelSizeXMicrons(0.5);
        scene2.setPixelSizeYMicrons(0.25);

        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "test.czi"), List.of(scene1, scene2));
        new GlobalCoordinateMapper().apply(metadata);

        assertEquals(0.0, scene1.getGlobalX(), 0.001);
        assertEquals(0.0, scene1.getGlobalY(), 0.001);
        assertEquals(100.0, scene2.getGlobalX(), 0.001);
        assertEquals(200.0, scene2.getGlobalY(), 0.001);
        assertEquals(200, metadata.getGlobalWidth());
        assertEquals(300, metadata.getGlobalHeight());
    }

    @Test
    void testNegativeAndFractionalCoordinatesNormalizeToZeroOrigin() {
        CziSceneInfo scene1 = new CziSceneInfo(0, 0, 10, 10);
        scene1.setStageXMicrons(-10.5);
        scene1.setStageYMicrons(-20.25);
        scene1.setPixelSizeXMicrons(0.5);
        scene1.setPixelSizeYMicrons(0.25);

        CziSceneInfo scene2 = new CziSceneInfo(1, 1, 10, 10);
        scene2.setStageXMicrons(-5.5);
        scene2.setStageYMicrons(-19.25);
        scene2.setPixelSizeXMicrons(0.5);
        scene2.setPixelSizeYMicrons(0.25);

        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "test.czi"), List.of(scene1, scene2));
        new GlobalCoordinateMapper().apply(metadata);

        assertEquals(0.0, scene1.getGlobalX(), 0.001);
        assertEquals(0.0, scene1.getGlobalY(), 0.001);
        assertEquals(10.0, scene2.getGlobalX(), 0.001);
        assertEquals(4.0, scene2.getGlobalY(), 0.001);
    }

    @Test
    void testDuplicateCoordinateWarning() {
        CziSceneInfo scene1 = new CziSceneInfo(0, 0, 100, 100);
        scene1.setStageXMicrons(1_000_000.25);
        scene1.setStageYMicrons(2_000_000.75);
        scene1.setPixelSizeXMicrons(1.0);
        scene1.setPixelSizeYMicrons(1.0);

        CziSceneInfo scene2 = new CziSceneInfo(1, 1, 100, 100);
        scene2.setStageXMicrons(1_000_000.25);
        scene2.setStageYMicrons(2_000_000.75);
        scene2.setPixelSizeXMicrons(1.0);
        scene2.setPixelSizeYMicrons(1.0);

        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "test.czi"), List.of(scene1, scene2));
        new GlobalCoordinateMapper().apply(metadata);

        assertTrue(metadata.getWarnings().stream().anyMatch(w -> w.contains("Duplicate stage coordinates")));
    }

    @Test
    void testSingleSceneCziMetadata() {
        CziSceneInfo scene = new CziSceneInfo(0, 0, 321, 123);
        scene.setStageXMicrons(42.0);
        scene.setStageYMicrons(24.0);
        scene.setPixelSizeXMicrons(0.5);
        scene.setPixelSizeYMicrons(0.5);

        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "single.czi"), List.of(scene));
        new GlobalCoordinateMapper().apply(metadata);

        assertEquals(321, metadata.getGlobalWidth());
        assertEquals(123, metadata.getGlobalHeight());
        assertEquals(1, metadata.getTotalTiles());
    }

    @Test
    void testManySceneCziMetadata() {
        java.util.ArrayList<CziSceneInfo> scenes = new java.util.ArrayList<>();
        for (int i = 0; i < 120; i++) {
            CziSceneInfo scene = new CziSceneInfo(i, i, 10, 10);
            scene.setStageXMicrons(i * 20.0);
            scene.setStageYMicrons(0.0);
            scene.setPixelSizeXMicrons(1.0);
            scene.setPixelSizeYMicrons(1.0);
            scenes.add(scene);
        }

        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "many.czi"), scenes);
        new GlobalCoordinateMapper().apply(metadata);

        assertEquals(2390, metadata.getGlobalWidth());
        assertEquals(120, metadata.getScenes().size());
    }
}
