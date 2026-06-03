package com.czispacialviewer.metadata;

import java.time.Instant;

public class DiagnosticMessage {

    private final DiagnosticLevel level;
    private final String message;
    private final String source;
    private final Integer sceneIndex;
    private final Integer seriesIndex;
    private final Instant timestamp;

    public DiagnosticMessage(DiagnosticLevel level, String message, String source, Integer sceneIndex, Integer seriesIndex) {
        this.level = level;
        this.message = message;
        this.source = source;
        this.sceneIndex = sceneIndex;
        this.seriesIndex = seriesIndex;
        this.timestamp = Instant.now();
    }

    public DiagnosticLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    public Integer getSceneIndex() {
        return sceneIndex;
    }

    public Integer getSeriesIndex() {
        return seriesIndex;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
