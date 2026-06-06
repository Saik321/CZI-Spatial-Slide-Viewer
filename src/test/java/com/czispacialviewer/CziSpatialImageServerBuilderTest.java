package com.czispacialviewer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CziSpatialImageServerBuilderTest {

    @Test
    void unsupportedExtensionReportsNoSupport() throws IOException {
        var support = new CziSpatialImageServerBuilder().checkImageSupport(URI.create("file:///tmp/not-a-czi.txt"));

        assertEquals(0f, support.getSupportLevel());
    }

    @Test
    void missingCziReportsSpecificReason() {
        Exception exception = assertThrows(Exception.class,
                () -> new CziSpatialImageServerBuilder().buildServer(URI.create("file:///tmp/missing-example.czi")));

        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void remoteCziReportsSpecificReason() {
        Exception exception = assertThrows(Exception.class,
                () -> new CziSpatialImageServerBuilder().buildServer(URI.create("https://example.org/slide.czi")));

        assertTrue(exception.getMessage().contains("local file URIs"));
    }
}
