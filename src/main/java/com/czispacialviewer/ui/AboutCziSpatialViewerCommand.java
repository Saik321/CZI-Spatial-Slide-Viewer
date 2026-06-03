package com.czispacialviewer.ui;

import com.czispacialviewer.CziSpatialSlideViewerInfo;
import qupath.lib.gui.dialogs.Dialogs;

public class AboutCziSpatialViewerCommand implements Runnable {

    @Override
    public void run() {
        Dialogs.showInfoNotification("About CZI Spatial Viewer",
                CziSpatialSlideViewerInfo.PLUGIN_NAME + " " + CziSpatialSlideViewerInfo.VERSION
                        + ". Bio-Formats backend. Creates a virtual coordinate-correct canvas and reads pixels lazily; it does not stitch scenes.");
    }
}
