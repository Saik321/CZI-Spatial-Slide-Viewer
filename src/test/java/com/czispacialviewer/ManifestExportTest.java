package com.czispacialviewer;

import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;
import com.czispacialviewer.metadata.GlobalCoordinateMapper;
import com.czispacialviewer.util.DebugJsonExporter;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestExportTest {

    @Test
    void testManifestHasExpectedFields() {
        CziSceneInfo scene = new CziSceneInfo(0, 0, 100, 100);
        scene.setStageXMicrons(0.0);
        scene.setStageYMicrons(0.0);
        scene.setPixelSizeXMicrons(1.0);
        scene.setPixelSizeYMicrons(1.0);
        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "test.czi"), List.of(scene));
        new GlobalCoordinateMapper().apply(metadata);

        JsonObject json = DebugJsonExporter.buildManifestJson(metadata);
        assertEquals(1, json.get("sceneCount").getAsInt());
        assertEquals(1, json.get("tileCount").getAsInt());
        assertTrue(json.has("globalCanvasWidth"));
        assertTrue(json.has("pyramidLevels"));
        assertTrue(json.has("warnings"));
        assertEquals(1, json.getAsJsonArray("scenes")
                .get(0).getAsJsonObject()
                .getAsJsonArray("tiles").size());
    }
}
