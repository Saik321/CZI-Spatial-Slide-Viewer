package com.czispacialviewer.ui;

import com.czispacialviewer.backend.BioFormatsCziReaderBackend;
import com.czispacialviewer.metadata.CziSpatialMetadata;
import com.czispacialviewer.util.DebugJsonExporter;
import com.czispacialviewer.util.LayoutPreviewExporter;
import com.czispacialviewer.util.OutputPaths;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;

import java.nio.file.Files;
import java.nio.file.Path;

public class ExportContactSheetCommand implements Runnable {

    private final QuPathGUI qupath;

    public ExportContactSheetCommand(QuPathGUI qupath) {
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
        Thread worker = new Thread(() -> export(inputPath), "CZI Spatial Contact Sheet Export");
        worker.setDaemon(true);
        worker.start();
    }

    private void export(Path inputPath) {
        try (var backend = new BioFormatsCziReaderBackend()) {
            CziSpatialMetadata metadata = backend.readMetadata(inputPath);
            OutputPaths paths = new OutputPaths(DebugJsonExporter.OUTPUT_DIR, inputPath);
            LayoutPreviewExporter.exportContactSheet(metadata, paths.contactSheet(), backend);
            Dialogs.showInfoNotification("CZI Spatial Viewer", "Exported contact sheet: " + paths.contactSheet());
        } catch (Exception e) {
            Dialogs.showErrorMessage("CZI Spatial Viewer", e);
        }
    }
}
