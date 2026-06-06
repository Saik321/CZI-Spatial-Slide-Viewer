package com.czispacialviewer.ui;

import com.czispacialviewer.util.DebugJsonExporter;
import com.czispacialviewer.util.SupportBundleExporter;
import qupath.lib.gui.dialogs.Dialogs;

import java.nio.file.Files;
import java.nio.file.Path;

public class RunDebugValidationCommand implements Runnable {

    @Override
    public void run() {
        Path inputPath = DebugJsonExporter.DEFAULT_INPUT_PATH;
        if (!Files.exists(inputPath)) {
            Dialogs.showErrorMessage("CZI Spatial Viewer",
                    "Example validation file not found: " + inputPath
                            + "\n\nPlace a local CZI at that path or use the file-picker export commands.");
            return;
        }
        Thread worker = new Thread(() -> export(inputPath), "CZI Spatial Validation Debug Export");
        worker.setDaemon(true);
        worker.start();
    }

    private void export(Path inputPath) {
        try {
            DebugJsonExporter.exportAll(inputPath);
            SupportBundleExporter.exportSupportBundle(inputPath);
            Dialogs.showInfoNotification("CZI Spatial Viewer", "Example debug export completed in outputs/");
        } catch (Exception e) {
            Dialogs.showErrorMessage("CZI Spatial Viewer", e);
        }
    }
}
