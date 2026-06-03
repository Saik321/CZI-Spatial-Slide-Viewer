package com.czispacialviewer.util;

import java.nio.file.Path;

public class DebugSupportBundleExporter {

    public static void main(String[] args) throws Exception {
        Path inputPath = args.length > 0 ? Path.of(args[0]) : DebugJsonExporter.DEFAULT_INPUT_PATH;
        Path supportBundle = SupportBundleExporter.exportSupportBundle(inputPath);
        System.out.println("Wrote support bundle: " + supportBundle.toAbsolutePath());
    }
}
