package com.czispacialviewer.ui;

import com.czispacialviewer.backend.BioFormatsCziReaderBackend;
import com.czispacialviewer.metadata.CziSpatialMetadata;
import com.czispacialviewer.util.DebugJsonExporter;
import com.czispacialviewer.util.LayoutPreviewExporter;
import com.czispacialviewer.util.OutputPaths;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;

import java.nio.file.Files;
import java.nio.file.Path;

public class ExportCziLayoutPreviewCommand implements Runnable {

    private final QuPathGUI qupath;

    public ExportCziLayoutPreviewCommand(QuPathGUI qupath) {
        this.qupath = qupath;
    }

    @Override
    public void run() {
        Path inputPath = CziFileDialogs.chooseCziFile(qupath);
        if (inputPath == null) {
            return;
        }
        if (!Files.exists(inputPath)) {
            Dialogs.showErrorMessage("CZI Spatial Viewer", "File not found: " + inputPath);
            return;
        }
        try (var backend = new BioFormatsCziReaderBackend()) {
            CziSpatialMetadata metadata = backend.readMetadata(inputPath);
            OutputPaths paths = new OutputPaths(DebugJsonExporter.OUTPUT_DIR, inputPath);
            LayoutPreviewExporter.exportPreview(metadata, paths.layoutPreview(), backend);
            LayoutPreviewExporter.exportHaloStylePreview(metadata, paths.haloStylePreview(), backend);
            LayoutPreviewExporter.exportContactSheet(metadata, paths.contactSheet(), backend);
            Dialogs.showInfoNotification("CZI Spatial Viewer", "Exported spatial preview, HALO-style preview, and contact sheet to outputs/");
        } catch (Exception e) {
            Dialogs.showErrorMessage("CZI Spatial Viewer", e);
        }
    }
}
