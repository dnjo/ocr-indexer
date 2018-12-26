package net.dnjo.model;

public class ImageUpdate {
    private final String text;
    private final String ocrText;

    public ImageUpdate(String text, String ocrText) {
        this.text = text;
        this.ocrText = ocrText;
    }

    public String getText() {
        return text;
    }

    public String getOcrText() {
        return ocrText;
    }
}
