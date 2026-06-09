package com.czispacialviewer;

import org.junit.jupiter.api.Test;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServers;
import qupath.lib.io.GsonTools;

import java.io.IOException;
import java.lang.reflect.Type;
import java.awt.image.BufferedImage;
import java.net.URI;
import com.google.gson.reflect.TypeToken;

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
    void cziSupportUsesQuPathDefaultSerializableBuilder() throws IOException {
        var support = new CziSpatialImageServerBuilder().checkImageSupport(URI.create("file:///tmp/example.czi"));

        assertEquals(4.5f, support.getSupportLevel());
        assertEquals(1, support.getBuilders().size());
        assertTrue(support.getBuilders().get(0).getClass().getName().contains("DefaultImageServerBuilder"));
    }

    @Test
    void cziSupportBuilderSerializesWithQuPathGsonFactory() throws IOException {
        var support = new CziSpatialImageServerBuilder().checkImageSupport(URI.create("file:///tmp/example.czi"));
        ImageServerBuilder.ServerBuilder<BufferedImage> builder = support.getBuilders().get(0);
        Type type = new TypeToken<ImageServerBuilder.ServerBuilder<BufferedImage>>() {
        }.getType();
        var gson = GsonTools.getDefaultBuilder()
                .registerTypeAdapterFactory(ImageServers.getServerBuilderFactory())
                .create();

        String json = gson.toJson(builder, type);
        ImageServerBuilder.ServerBuilder<BufferedImage> roundTrip = gson.fromJson(json, type);

        assertTrue(json.contains("CziSpatialImageServerBuilder"));
        assertEquals(builder.getURIs(), roundTrip.getURIs());
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
