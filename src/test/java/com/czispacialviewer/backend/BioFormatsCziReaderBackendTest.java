package com.czispacialviewer.backend;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
}
