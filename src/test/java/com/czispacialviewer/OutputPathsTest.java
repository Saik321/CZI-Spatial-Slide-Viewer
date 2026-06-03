package com.czispacialviewer;

import com.czispacialviewer.util.OutputPaths;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputPathsTest {

    @Test
    void testSafeFilenameHandlesSpacesAndSymbols() {
        String safe = OutputPaths.safeBaseName(Path.of("data", "Axio 4 unsafe no.czi"));
        assertEquals("axio_4_unsafe_no", safe);
    }

    @Test
    void testOutputPathNamesUseSafeBase() {
        OutputPaths paths = new OutputPaths(Path.of("outputs"), Path.of("data", "My Slide 01.czi"));
        assertEquals(Path.of("outputs", "my_slide_01_manifest.json"), paths.manifest());
    }
}
