package net.dnjo.model;

import java.time.LocalDateTime;

public class Image {
    private final String id;
    private final LocalDateTime createdAt;
    private final String text;
    private final String ocrText;
    private final String type;

    public Image(String id, LocalDateTime createdAt, String text, String ocrText, String type) {
        this.id = id;
        this.createdAt = createdAt;
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
}
