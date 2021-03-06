package net.dnjo.model;

import java.time.LocalDateTime;

public class Image {
    private final String id;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String text;
    private final String ocrText;
    private final String type;

    public Image(final String id, final LocalDateTime createdAt, final LocalDateTime updatedAt, final String text, final String ocrText, final String type) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.text = text;
        this.ocrText = ocrText;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getText() {
        return text;
    }

    public String getOcrText() {
        return ocrText;
    }

    public String getType() {
        return type;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
