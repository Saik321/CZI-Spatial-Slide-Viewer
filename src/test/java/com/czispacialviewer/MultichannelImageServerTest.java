package com.czispacialviewer;

import com.czispacialviewer.metadata.CziSceneInfo;
import com.czispacialviewer.metadata.CziSpatialMetadata;
import com.czispacialviewer.util.BandedImageTools;
import org.junit.jupiter.api.Test;
import qupath.lib.images.servers.PixelType;

import java.awt.image.DataBuffer;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultichannelImageServerTest {

    @Test
    void multichannelMetadataIsNotDeclaredAsRgb() {
        CziSceneInfo scene = new CziSceneInfo(0, 0, 128, 64);
        scene.setStageXMicrons(0.0);
        scene.setStageYMicrons(0.0);
        scene.setPixelSizeXMicrons(0.5);
        scene.setPixelSizeYMicrons(0.5);
        scene.setRgb(false);
        scene.setPixelType("uint16");
        scene.setChannelCount(4);
        scene.getChannelNames().addAll(List.of("DAPI", "FITC", "TRITC", "Cy5"));

        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "example_validation.czi"), List.of(scene));
        metadata.setRgb(false);
        metadata.setPixelType("uint16");
        metadata.setChannelCount(4);
        metadata.getChannelNames().addAll(scene.getChannelNames());
        metadata.setGlobalWidth(128);
        metadata.setGlobalHeight(64);

        var imageMetadata = CziSpatialImageServer.buildImageServerMetadata(URI.create("file:///example_validation.czi"), metadata);

        assertFalse(imageMetadata.isRGB());
        assertEquals(PixelType.UINT16, imageMetadata.getPixelType());
        assertEquals(4, imageMetadata.getChannels().size());
        assertEquals("DAPI", imageMetadata.getChannel(0).getName());
    }

    @Test
    void bandedImagePreservesAllChannelsAndBitDepth() {
        BufferedImage image = BandedImageTools.createBandedImage(8, 8, 5, DataBuffer.TYPE_USHORT);

        assertEquals(5, image.getRaster().getNumBands());
        assertEquals(DataBuffer.TYPE_USHORT, image.getRaster().getDataBuffer().getDataType());
    }

    @Test
    void rgbBrightfieldMetadataUsesStandardQuPathRgbDisplayModel() {
        CziSceneInfo scene = new CziSceneInfo(0, 0, 128, 64);
        scene.setStageXMicrons(0.0);
        scene.setStageYMicrons(0.0);
        scene.setPixelSizeXMicrons(0.5);
        scene.setPixelSizeYMicrons(0.5);
        scene.setRgb(true);
        scene.setPixelType("uint16");
        scene.setChannelCount(3);

        CziSpatialMetadata metadata = new CziSpatialMetadata(Path.of("data", "example_validation.czi"), List.of(scene));
        metadata.setRgb(true);
        metadata.setPixelType("uint8");
        metadata.setChannelCount(3);
        metadata.setGlobalWidth(128);
        metadata.setGlobalHeight(64);

        var imageMetadata = CziSpatialImageServer.buildImageServerMetadata(URI.create("file:///example_validation.czi"), metadata);

        assertTrue(imageMetadata.isRGB());
        assertEquals(PixelType.UINT8, imageMetadata.getPixelType());
    }
}
