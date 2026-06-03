package com.czispacialviewer.metadata;

import com.czispacialviewer.util.RegionIntersection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlobalCoordinateMapper {

    private static final double PIXEL_SIZE_TOLERANCE = 0.01;

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

        double maxX = 0;
        double maxY = 0;
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

            maxX = Math.max(maxX, globalX + scene.getWidth());
            maxY = Math.max(maxY, globalY + scene.getHeight());
            totalTiles += Math.max(1, scene.getTiles().size());
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
