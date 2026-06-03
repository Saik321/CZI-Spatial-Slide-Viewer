package com.czispacialviewer;

import com.czispacialviewer.ui.AboutCziSpatialViewerCommand;
import com.czispacialviewer.ui.ExportContactSheetCommand;
import com.czispacialviewer.ui.ExportCziLayoutManifestCommand;
import com.czispacialviewer.ui.ExportCziLayoutPreviewCommand;
import com.czispacialviewer.ui.ExportSupportBundleCommand;
import com.czispacialviewer.ui.OpenCziSpatialSlideCommand;
import com.czispacialviewer.ui.PluginSettingsCommand;
import com.czispacialviewer.ui.RunDebugSepsisCommand;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.localization.QuPathResources;

public class CziSpatialSlideExtension implements QuPathExtension {

    @Override
    public void installExtension(QuPathGUI qupath) {
        MenuBar menuBar = qupath.getMenuBar();
        String extensionsLabel = QuPathResources.getString("Menu.Extensions");
        Menu extensionsMenu = findOrCreateMenu(menuBar, extensionsLabel);

        Menu cziMenu = findOrCreateSubMenu(extensionsMenu, "CZI Spatial Viewer");

        MenuItem openItem = new MenuItem("Open CZI Spatial Slide");
        openItem.setOnAction(e -> new OpenCziSpatialSlideCommand(qupath).run());

        MenuItem manifestItem = new MenuItem("Export CZI Layout Manifest");
        manifestItem.setOnAction(e -> new ExportCziLayoutManifestCommand(qupath).run());

        MenuItem previewItem = new MenuItem("Export Low-Resolution Layout Preview");
        previewItem.setOnAction(e -> new ExportCziLayoutPreviewCommand(qupath).run());

        MenuItem contactSheetItem = new MenuItem("Export Contact Sheet");
        contactSheetItem.setOnAction(e -> new ExportContactSheetCommand(qupath).run());

        MenuItem supportItem = new MenuItem("Export Support Bundle");
        supportItem.setOnAction(e -> new ExportSupportBundleCommand(qupath).run());

        MenuItem debugSepsisItem = new MenuItem("Run Debug Test on Sepsis CZI");
        debugSepsisItem.setOnAction(e -> new RunDebugSepsisCommand().run());

        MenuItem settingsItem = new MenuItem("Plugin Settings");
        settingsItem.setOnAction(e -> new PluginSettingsCommand().run());

        MenuItem aboutItem = new MenuItem("About CZI Spatial Viewer");
        aboutItem.setOnAction(e -> new AboutCziSpatialViewerCommand().run());

        cziMenu.getItems().setAll(openItem, manifestItem, previewItem, contactSheetItem,
                supportItem, debugSepsisItem, settingsItem, aboutItem);
    }

    @Override
    public String getName() {
        return "CZI Spatial Slide Viewer";
    }

    @Override
    public String getDescription() {
        return "Displays multi-scene CZI files in a coordinate-correct virtual canvas.";
    }

    @Override
    public Version getQuPathVersion() {
        return Version.parse("0.5.0");
    }

    private Menu findOrCreateMenu(MenuBar menuBar, String label) {
        for (Menu menu : menuBar.getMenus()) {
            if (label.equals(menu.getText())) {
                return menu;
            }
        }
        Menu menu = new Menu(label);
        menuBar.getMenus().add(menu);
        return menu;
    }

    private Menu findOrCreateSubMenu(Menu parent, String label) {
        for (MenuItem item : parent.getItems()) {
            if (item instanceof Menu menu && label.equals(menu.getText())) {
                return menu;
            }
        }
        Menu menu = new Menu(label);
        parent.getItems().add(menu);
        return menu;
    }
}
