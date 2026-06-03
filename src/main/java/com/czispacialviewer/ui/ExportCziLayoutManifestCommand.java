package com.czispacialviewer.ui;

import com.czispacialviewer.backend.BioFormatsCziReaderBackend;
import com.czispacialviewer.metadata.CziSpatialMetadata;
import com.czispacialviewer.util.DebugJsonExporter;
import com.czispacialviewer.util.OutputPaths;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;

import java.nio.file.Files;
import java.nio.file.Path;

public class ExportCziLayoutManifestCommand implements Runnable {

    private final QuPathGUI qupath;

    public ExportCziLayoutManifestCommand(QuPathGUI qupath) {
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
            DebugJsonExporter.writeManifest(metadata, paths.manifest());
            DebugJsonExporter.writeDebugReport(metadata, backend.getName(), paths.debugReport());
            Dialogs.showInfoNotification("CZI Spatial Viewer", "Exported manifest and debug report to outputs/");
        } catch (Exception e) {
            Dialogs.showErrorMessage("CZI Spatial Viewer", e);
        }
    }
}
