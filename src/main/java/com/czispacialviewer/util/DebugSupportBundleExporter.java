package com.czispacialviewer.util;

public class DebugSupportBundleExporter {

    public static void main(String[] args) throws Exception {
        java.nio.file.Path inputPath = DebugJsonExporter.resolveInputPath(args);
        java.nio.file.Path supportBundle = SupportBundleExporter.exportSupportBundle(inputPath);
        System.out.println("Wrote support bundle: " + supportBundle.toAbsolutePath());
    }
}
