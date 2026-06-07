package com.czispacialviewer.backend;

import com.czispacialviewer.metadata.CziPyramidLevel;
import com.czispacialviewer.metadata.CziSceneInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BioFormatsCziReaderBackendTest {

    @Test
    void shortOmeMetadataArraysBecomeWarningsInsteadOfOpenFailures() {
        BioFormatsCziReaderBackend backend = new BioFormatsCziReaderBackend();
        List<String> warnings = new ArrayList<>();

        String value = backend.safeMetadataValue("channel name", 2, 1, warnings, () -> {
            throw new IndexOutOfBoundsException("Index 1 out of bounds for length 1");
        });

        assertNull(value);
        assertTrue(warnings.stream().anyMatch(warning ->
                warning.contains("OME metadata channel name 1 unavailable for series 2")));
    }

    @Test
    void singleSceneInternalPyramidLevelsKeepSourceResolutionIndexes() {
        BioFormatsCziReaderBackend backend = new BioFormatsCziReaderBackend();
        CziSceneInfo scene = spatialScene(0, 1000, 1000);
        scene.getPyramidLevels().add(new CziPyramidLevel(0, 1000, 1000, 1.0, 0, 0));
        scene.getPyramidLevels().add(new CziPyramidLevel(1, 500, 500, 2.0, 0, 1));
        scene.getPyramidLevels().add(new CziPyramidLevel(2, 250, 250, 4.0, 0, 2));

        List<CziSceneInfo> grouped = backend.groupPyramidSeries(List.of(scene), new ArrayList<>());

        assertEquals(1, grouped.size());
        assertEquals(3, grouped.get(0).getPyramidLevels().size());
        CziPyramidLevel selected = backend.selectPyramidLevel(grouped.get(0), 4.0);
        assertEquals(0, selected.getSourceSeriesIndex());
        assertEquals(2, selected.getSourceResolutionIndex());
        assertEquals(4.0, selected.getDownsample(), 0.001);
    }

    @Test
    void separatePyramidSeriesUseResolutionZeroWithinEachSourceSeries() {
        BioFormatsCziReaderBackend backend = new BioFormatsCziReaderBackend();
        CziSceneInfo base = spatialScene(0, 1000, 1000);
        base.getPyramidLevels().add(new CziPyramidLevel(0, 1000, 1000, 1.0, 0, 0));
        CziSceneInfo reduced = spatialScene(1, 250, 250);
        reduced.getPyramidLevels().add(new CziPyramidLevel(0, 250, 250, 1.0, 1, 0));

        List<CziSceneInfo> grouped = backend.groupPyramidSeries(List.of(base, reduced), new ArrayList<>());

        CziPyramidLevel selected = backend.selectPyramidLevel(grouped.get(0), 4.0);
        assertEquals(1, selected.getSourceSeriesIndex());
        assertEquals(0, selected.getSourceResolutionIndex());
        assertEquals(4.0, selected.getDownsample(), 0.001);
    }

    private CziSceneInfo spatialScene(int series, int width, int height) {
        CziSceneInfo scene = new CziSceneInfo(series, series, width, height);
        scene.setStageXMicrons(10.0);
        scene.setStageYMicrons(20.0);
        scene.setPixelSizeXMicrons(0.5);
        scene.setPixelSizeYMicrons(0.5);
        return scene;
    }
}
