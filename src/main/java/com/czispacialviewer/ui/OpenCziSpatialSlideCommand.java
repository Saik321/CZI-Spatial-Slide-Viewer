package com.czispacialviewer.ui;

import com.czispacialviewer.CziSpatialImageServerBuilder;
import com.czispacialviewer.CziSpatialImageServer;
import javafx.application.Platform;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;

import java.nio.file.Files;
import java.nio.file.Path;

public class OpenCziSpatialSlideCommand implements Runnable {

    private final QuPathGUI qupath;

    public OpenCziSpatialSlideCommand(QuPathGUI qupath) {
        this.qupath = qupath;
    }

    @Override
    public void run() {
        Path inputPath = CziFileDialogs.chooseCziFile(qupath);
        if (inputPath == null) {
            return;
        }
        if (!CziFileDialogs.isCzi(inputPath)) {
            Dialogs.showErrorMessage("CZI Spatial Viewer", "Selected file is not a CZI: " + inputPath);
            return;
        }
        if (!Files.exists(inputPath)) {
            Dialogs.showErrorMessage("CZI Spatial Viewer", "File not found: " + inputPath);
            return;
        }
        Dialogs.showInfoNotification("CZI Spatial Viewer", "Opening CZI metadata in background: " + inputPath.getFileName());
        Thread thread = new Thread(() -> {
            try {
                var server = new CziSpatialImageServerBuilder().buildServer(inputPath.toUri());
                var imageData = new ImageData<>(server);
                Platform.runLater(() -> {
                    if (qupath.getViewer() == null) {
                        Dialogs.showErrorMessage("CZI Spatial Viewer", "No active QuPath viewer is available.");
                        return;
                    }
                    try {
                        qupath.getViewer().setImageData(imageData);
                    } catch (Exception e) {
                        Dialogs.showErrorMessage("CZI Spatial Viewer", e);
                        return;
                    }
                    var metadata = server instanceof CziSpatialImageServer cziServer ? cziServer.getSpatialMetadata() : null;
                    if (metadata == null) {
                        Dialogs.showInfoNotification("CZI Spatial Viewer", "Opened CZI Spatial Slide. Real pixels enabled.");
                        return;
                    }
                    Dialogs.showInfoNotification("CZI Spatial Viewer",
                            "Opened " + metadata.getScenes().size() + " scenes, canvas "
                                    + metadata.getGlobalWidth() + " x " + metadata.getGlobalHeight()
                                    + ", warnings " + metadata.getWarnings().size()
                                    + ". Real pixels enabled.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> Dialogs.showErrorMessage("CZI Spatial Viewer", e));
            }
        }, "czi-spatial-open");
        thread.setDaemon(true);
        thread.start();
    }
}
