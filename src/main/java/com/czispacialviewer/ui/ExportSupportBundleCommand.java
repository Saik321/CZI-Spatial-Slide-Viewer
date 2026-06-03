package com.czispacialviewer.ui;

import com.czispacialviewer.util.SupportBundleExporter;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;

import java.nio.file.Path;

public class ExportSupportBundleCommand implements Runnable {

    private final QuPathGUI qupath;

    public ExportSupportBundleCommand(QuPathGUI qupath) {
        this.qupath = qupath;
    }

    @Override
    public void run() {
        Path inputPath = CziFileDialogs.chooseCziFile(qupath);
        if (inputPath == null) {
            return;
        }
        Thread worker = new Thread(() -> export(inputPath), "CZI Spatial Support Bundle Export");
        worker.setDaemon(true);
        worker.start();
    }

    private void export(Path inputPath) {
        try {
            Path bundle = SupportBundleExporter.exportSupportBundle(inputPath);
            Dialogs.showInfoNotification("CZI Spatial Viewer", "Exported support bundle: " + bundle);
        } catch (Exception e) {
            Dialogs.showErrorMessage("CZI Spatial Viewer", e);
        }
    }
}
