package com.czispacialviewer.ui;

import com.czispacialviewer.util.DebugJsonExporter;
import javafx.stage.FileChooser;
import qupath.lib.gui.QuPathGUI;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CziFileDialogs {

    private CziFileDialogs() {
    }

    public static Path chooseCziFile(QuPathGUI qupath) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose CZI spatial slide");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zeiss CZI", "*.czi", "*.czis"));
        Path defaultPath = DebugJsonExporter.DEFAULT_INPUT_PATH;
        if (Files.exists(defaultPath) && defaultPath.getParent() != null) {
            chooser.setInitialDirectory(defaultPath.getParent().toAbsolutePath().toFile());
        }
        File file = chooser.showOpenDialog(qupath == null ? null : qupath.getStage());
        return file == null ? null : file.toPath();
    }

    public static boolean isCzi(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String lower = path.getFileName().toString().toLowerCase();
        return lower.endsWith(".czi") || lower.endsWith(".czis");
    }
}
