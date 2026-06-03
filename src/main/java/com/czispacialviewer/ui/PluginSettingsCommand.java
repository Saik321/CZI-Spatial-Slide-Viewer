package com.czispacialviewer.ui;

import com.czispacialviewer.CziSpatialViewerSettings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import qupath.lib.gui.dialogs.Dialogs;

import java.util.Optional;

public class PluginSettingsCommand implements Runnable {

    @Override
    public void run() {
        CziSpatialViewerSettings settings = CziSpatialViewerSettings.getDefault();

        Spinner<Integer> cacheMb = spinner(64, 4096, settings.getMaxCacheMemoryMb(), 64);
        Spinner<Integer> previewDimension = spinner(512, 16384, settings.getMaxPreviewDimension(), 512);
        Spinner<Integer> thumbnailSize = spinner(64, 1024, settings.getThumbnailSize(), 32);
        Spinner<Integer> maxOutputPixels = spinner(1_048_576, 268_435_456, settings.getMaxOutputPixels(), 1_048_576);
        Spinner<Integer> backgroundThreshold = spinner(120, 245, settings.getTransparentBackgroundThreshold(), 1);
        Spinner<Integer> colorSpread = spinner(0, 120, settings.getTransparentBackgroundMaxColorSpread(), 1);

        CheckBox transparentBackground = checkbox(
                "Make light blank scene background transparent while compositing",
                settings.isTransparentLightBackgroundInOverlaps());
        CheckBox debugLogging = checkbox("Enable debug logging", settings.isDebugLogging());
        CheckBox sceneBoundaries = checkbox("Show scene boundaries overlay", settings.isOverlaySceneBoundaries());
        CheckBox sceneLabels = checkbox("Show scene labels overlay", settings.isOverlaySceneLabels());
        CheckBox tileBoundaries = checkbox("Show requested tile boundary overlay", settings.isOverlayTileBoundaries());
        CheckBox coordinateGrid = checkbox("Show coordinate grid overlay", settings.isOverlayCoordinateGrid());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        int row = 0;
        row = addRow(grid, row, "Max cache memory MB", cacheMb);
        row = addRow(grid, row, "Max preview dimension px", previewDimension);
        row = addRow(grid, row, "Thumbnail size px", thumbnailSize);
        row = addRow(grid, row, "Max output pixels per tile", maxOutputPixels);
        row = addRow(grid, row, "Blank background threshold", backgroundThreshold);
        row = addRow(grid, row, "Allowed neutral color spread", colorSpread);

        grid.add(transparentBackground, 0, row++, 2, 1);
        grid.add(sceneBoundaries, 0, row++, 2, 1);
        grid.add(sceneLabels, 0, row++, 2, 1);
        grid.add(tileBoundaries, 0, row++, 2, 1);
        grid.add(coordinateGrid, 0, row++, 2, 1);
        grid.add(debugLogging, 0, row++, 2, 1);

        Label note = new Label("Background transparency is intended for brightfield Axioscan scenes where blank rectangular scan areas overlap tissue. It does not move scenes.");
        note.setWrapText(true);
        grid.add(note, 0, row, 2, 1);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("CZI Spatial Viewer Settings");
        dialog.getDialogPane().setContent(grid);
        ButtonType save = new ButtonType("Save");
        ButtonType reset = new ButtonType("Reset Defaults");
        dialog.getDialogPane().getButtonTypes().addAll(save, reset, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
            return;
        }
        if (result.get() == reset) {
            settings.resetDefaults();
            Dialogs.showInfoNotification("CZI Spatial Viewer", "Settings reset. Reopen the image or pan/zoom to refresh cached tiles.");
            return;
        }

        settings.setMaxCacheMemoryMb(cacheMb.getValue());
        settings.setMaxPreviewDimension(previewDimension.getValue());
        settings.setThumbnailSize(thumbnailSize.getValue());
        settings.setMaxOutputPixels(maxOutputPixels.getValue());
        settings.setTransparentBackgroundThreshold(backgroundThreshold.getValue());
        settings.setTransparentBackgroundMaxColorSpread(colorSpread.getValue());
        settings.setTransparentLightBackgroundInOverlaps(transparentBackground.isSelected());
        settings.setDebugLogging(debugLogging.isSelected());
        settings.setOverlaySceneBoundaries(sceneBoundaries.isSelected());
        settings.setOverlaySceneLabels(sceneLabels.isSelected());
        settings.setOverlayTileBoundaries(tileBoundaries.isSelected());
        settings.setOverlayCoordinateGrid(coordinateGrid.isSelected());
        settings.save();

        Dialogs.showInfoNotification("CZI Spatial Viewer", "Settings saved. Reopen the image or pan/zoom to refresh cached tiles.");
    }

    private static Spinner<Integer> spinner(int min, int max, int value, int step) {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setEditable(true);
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, value, step));
        spinner.setPrefWidth(160);
        return spinner;
    }

    private static CheckBox checkbox(String text, boolean selected) {
        CheckBox checkBox = new CheckBox(text);
        checkBox.setSelected(selected);
        return checkBox;
    }

    private static int addRow(GridPane grid, int row, String label, Node control) {
        grid.add(new Label(label), 0, row);
        grid.add(control, 1, row);
        return row + 1;
    }
}
