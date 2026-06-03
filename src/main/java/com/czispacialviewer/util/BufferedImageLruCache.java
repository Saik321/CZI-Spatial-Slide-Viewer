package com.czispacialviewer.util;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class BufferedImageLruCache {

    private final long maxBytes;
    private final LinkedHashMap<String, BufferedImage> entries = new LinkedHashMap<>(64, 0.75f, true);
    private long estimatedBytes;

    public BufferedImageLruCache(long maxBytes) {
        this.maxBytes = Math.max(1, maxBytes);
    }

    public synchronized BufferedImage get(String key) {
        return entries.get(key);
    }

    public synchronized void put(String key, BufferedImage image) {
        BufferedImage old = entries.put(key, image);
        if (old != null) {
            estimatedBytes -= estimateBytes(old);
        }
        estimatedBytes += estimateBytes(image);
        evictIfNeeded();
    }

    public synchronized long getEstimatedBytes() {
        return estimatedBytes;
    }

    public synchronized int size() {
        return entries.size();
    }

    private void evictIfNeeded() {
        Iterator<Map.Entry<String, BufferedImage>> iterator = entries.entrySet().iterator();
        while (estimatedBytes > maxBytes && iterator.hasNext()) {
            Map.Entry<String, BufferedImage> entry = iterator.next();
            estimatedBytes -= estimateBytes(entry.getValue());
            iterator.remove();
        }
    }

    private long estimateBytes(BufferedImage image) {
        if (image == null) {
            return 0L;
        }
        int bands = image.getRaster() == null ? 4 : image.getRaster().getNumBands();
        int bytesPerSample = image.getColorModel() == null ? 1 : Math.max(1, image.getColorModel().getPixelSize() / Math.max(1, bands) / 8);
        return (long) image.getWidth() * (long) image.getHeight() * Math.max(1, bands) * Math.max(1, bytesPerSample);
    }
}
