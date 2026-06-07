package com.czispacialviewer.metadata;

import com.czispacialviewer.util.RegionIntersection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlobalCoordinateMapper {

    private static final double PIXEL_SIZE_TOLERANCE = 0.01;
    private static final double SAME_COLUMN_X_OVERLAP_RATIO = 0.60;
    private static final double MIN_VERTICAL_OVERLAP_TO_CORRECT = 2.0;

    public void apply(CziSpatialMetadata metadata) {
        List<CziSceneInfo> scenes = metadata.getScenes();
        if (scenes.isEmpty()) {
            metadata.addWarning("No scenes found.");
            return;
        }

        Double refPixelX = null;
        Double refPixelY = null;
        for (CziSceneInfo scene : scenes) {
            if (scene.getPixelSizeXMicrons() != null && scene.getPixelSizeYMicrons() != null) {
                refPixelX = scene.getPixelSizeXMicrons();
                refPixelY = scene.getPixelSizeYMicrons();
                break;
            }
        }

        if (refPixelX == null || refPixelY == null) {
            metadata.addWarning("Missing pixel size metadata; global coordinates may be unreliable.");
        } else {
            metadata.setPixelSizeXMicrons(refPixelX);
            metadata.setPixelSizeYMicrons(refPixelY);
        }

        Double minStageX = null;
        Double minStageY = null;
        for (CziSceneInfo scene : scenes) {
            if (scene.getStageXMicrons() == null || scene.getStageYMicrons() == null) {
                metadata.addWarning("Missing stage X/Y metadata for scene " + scene.getSceneIndex());
                continue;
            }
            minStageX = minStageX == null ? scene.getStageXMicrons() : Math.min(minStageX, scene.getStageXMicrons());
            minStageY = minStageY == null ? scene.getStageYMicrons() : Math.min(minStageY, scene.getStageYMicrons());
        }

        if (minStageX == null || minStageY == null) {
            metadata.addWarning("No valid stage positions found; using origin at (0,0).");
            minStageX = 0.0;
            minStageY = 0.0;
        }

        int totalTiles = 0;
        Set<String> seenStageCoordinates = new HashSet<>();

        for (CziSceneInfo scene : scenes) {
            if (scene.getTiles().isEmpty()) {
                scene.getTiles().add(new CziTileInfo(scene.getSceneIndex(), 0, 0, 1.0, scene.getWidth(), scene.getHeight()));
            }
            if (scene.getPixelSizeXMicrons() != null && refPixelX != null) {
                double delta = Math.abs(scene.getPixelSizeXMicrons() - refPixelX) / refPixelX;
                if (delta > PIXEL_SIZE_TOLERANCE) {
                    metadata.addWarning("Inconsistent pixel size X for scene " + scene.getSceneIndex());
                }
            }
            if (scene.getPixelSizeYMicrons() != null && refPixelY != null) {
                double delta = Math.abs(scene.getPixelSizeYMicrons() - refPixelY) / refPixelY;
                if (delta > PIXEL_SIZE_TOLERANCE) {
                    metadata.addWarning("Inconsistent pixel size Y for scene " + scene.getSceneIndex());
                }
            }

            double globalX = 0;
            double globalY = 0;
            if (scene.getStageXMicrons() != null && scene.getStageYMicrons() != null && refPixelX != null && refPixelY != null) {
                String stageKey = Math.round(scene.getStageXMicrons() * 1000.0) + ":" + Math.round(scene.getStageYMicrons() * 1000.0);
                if (!seenStageCoordinates.add(stageKey)) {
                    metadata.addWarning("Duplicate stage coordinates detected for scene " + scene.getSceneIndex());
                }
                globalX = (scene.getStageXMicrons() - minStageX) / refPixelX;
                globalY = (scene.getStageYMicrons() - minStageY) / refPixelY;
            }
            scene.setGlobalX(globalX);
            scene.setGlobalY(globalY);
            for (CziTileInfo tile : scene.getTiles()) {
                tile.setGlobalX(globalX);
                tile.setGlobalY(globalY);
            }

            totalTiles += Math.max(1, scene.getTiles().size());
        }

        resolveLikelyVerticalStackingOverlaps(metadata);

        double maxX = 0;
        double maxY = 0;
        for (CziSceneInfo scene : scenes) {
            maxX = Math.max(maxX, scene.getGlobalX() + scene.getWidth());
            maxY = Math.max(maxY, scene.getGlobalY() + scene.getHeight());
        }

        metadata.setMinX(0);
        metadata.setMinY(0);
        metadata.setMaxX(maxX);
        metadata.setMaxY(maxY);
        metadata.setGlobalWidth(Math.max(1, (int)Math.ceil(maxX)));
        metadata.setGlobalHeight(Math.max(1, (int)Math.ceil(maxY)));
        metadata.setTotalTiles(totalTiles);

        checkOverlaps(metadata);
        buildGlobalPyramidLevels(metadata);
    }

    private void resolveLikelyVerticalStackingOverlaps(CziSpatialMetadata metadata) {
        List<CziSceneInfo> sorted = new ArrayList<>(metadata.getScenes());
        sorted.sort(Comparator
                .comparingDouble(CziSceneInfo::getGlobalY)
                .thenComparingDouble(CziSceneInfo::getGlobalX)
                .thenComparingInt(CziSceneInfo::getSceneIndex));

        for (int i = 0; i < sorted.size(); i++) {
            CziSceneInfo upper = sorted.get(i);
            for (int j = i + 1; j < sorted.size(); j++) {
                CziSceneInfo lower = sorted.get(j);
                if (!looksLikeSameColumnVerticalOverlap(upper, lower)) {
                    continue;
                }
                double correctedY = upper.getGlobalY() + upper.getHeight();
                if (correctedY > lower.getGlobalY()) {
                    double shift = correctedY - lower.getGlobalY();
                    shiftSceneY(lower, correctedY);
                    metadata.addWarning("Adjusted likely same-column vertical scene overlap between scenes "
                            + upper.getSceneIndex() + " and " + lower.getSceneIndex()
                            + " by shifting scene " + lower.getSceneIndex() + " down " + Math.round(shift)
                            + " px. This preserves separate scene rectangles when CZI metadata appears to use tissue-bounding-box origins.");
                }
            }
        }
    }

    private boolean looksLikeSameColumnVerticalOverlap(CziSceneInfo upper, CziSceneInfo lower) {
        if (upper.getGlobalY() > lower.getGlobalY()) {
            return looksLikeSameColumnVerticalOverlap(lower, upper);
        }
        double xOverlap = overlapLength(upper.getGlobalX(), upper.getGlobalX() + upper.getWidth(),
                lower.getGlobalX(), lower.getGlobalX() + lower.getWidth());
        double minWidth = Math.max(1.0, Math.min(upper.getWidth(), lower.getWidth()));
        double xOverlapRatio = xOverlap / minWidth;
        if (xOverlapRatio < SAME_COLUMN_X_OVERLAP_RATIO) {
            return false;
        }

        double verticalOverlap = upper.getGlobalY() + upper.getHeight() - lower.getGlobalY();
        if (verticalOverlap < MIN_VERTICAL_OVERLAP_TO_CORRECT) {
            return false;
        }

        double centerDeltaY = Math.abs(centerY(upper) - centerY(lower));
        double minHeight = Math.max(1.0, Math.min(upper.getHeight(), lower.getHeight()));
        return centerDeltaY > minHeight * 0.25;
    }

    private double overlapLength(double a1, double a2, double b1, double b2) {
        return Math.max(0.0, Math.min(a2, b2) - Math.max(a1, b1));
    }

    private double centerY(CziSceneInfo scene) {
        return scene.getGlobalY() + scene.getHeight() / 2.0;
    }

    private void shiftSceneY(CziSceneInfo scene, double newY) {
        scene.setGlobalY(newY);
        for (CziTileInfo tile : scene.getTiles()) {
            tile.setGlobalY(newY);
        }
    }

    private void checkOverlaps(CziSpatialMetadata metadata) {
        List<CziSceneInfo> scenes = metadata.getScenes();
        for (int i = 0; i < scenes.size(); i++) {
            CziSceneInfo a = scenes.get(i);
            for (int j = i + 1; j < scenes.size(); j++) {
                CziSceneInfo b = scenes.get(j);
                if (RegionIntersection.intersects(
                        a.getGlobalX(), a.getGlobalY(), a.getWidth(), a.getHeight(),
                        b.getGlobalX(), b.getGlobalY(), b.getWidth(), b.getHeight())) {
                    metadata.addWarning("Overlapping scenes detected: " + a.getSceneIndex() + " and " + b.getSceneIndex());
                }
            }
        }
    }

    private void buildGlobalPyramidLevels(CziSpatialMetadata metadata) {
        Set<Double> downsampleSet = new HashSet<>();
        for (CziSceneInfo scene : metadata.getScenes()) {
            if (scene.getPyramidLevels().isEmpty()) {
                metadata.addWarning("Missing pyramid levels for scene " + scene.getSceneIndex());
            }
            for (CziPyramidLevel level : scene.getPyramidLevels()) {
                downsampleSet.add(level.getDownsample());
            }
        }
        if (downsampleSet.isEmpty()) {
            downsampleSet.add(1.0);
        }

        List<Double> downsamples = new ArrayList<>(downsampleSet);
        downsamples.sort(Comparator.naturalOrder());

        List<CziPyramidLevel> globalLevels = new ArrayList<>();
        int levelIndex = 0;
        for (double downsample : downsamples) {
            int width = Math.max(1, (int)Math.ceil(metadata.getGlobalWidth() / downsample));
            int height = Math.max(1, (int)Math.ceil(metadata.getGlobalHeight() / downsample));
            globalLevels.add(new CziPyramidLevel(levelIndex++, width, height, downsample));
        }
        metadata.setPyramidLevels(globalLevels);
    }
}
