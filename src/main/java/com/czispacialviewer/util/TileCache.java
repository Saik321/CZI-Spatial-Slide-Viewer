package com.czispacialviewer.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class TileCache<K, V> extends LinkedHashMap<K, V> {

    private final int maxEntries;

    public TileCache(int maxEntries) {
        super(16, 0.75f, true);
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxEntries;
    }
}
