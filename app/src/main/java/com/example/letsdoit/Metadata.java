package com.example.letsdoit;

public class Metadata {

    private String filePath;
    private int width;
    private int height;
    private int quality;
    public void ImageMetadata(String filePath, int width, int height, int quality) {
        this.filePath = filePath;
        this.width = width;
        this.height = height;
        this.quality = quality;
    }

    // Getters and setters
    public String getFilePath() {
        return filePath;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getQuality() {
        return quality;
    }
}
