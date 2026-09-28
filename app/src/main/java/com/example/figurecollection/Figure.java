package com.example.figurecollection;

public class Figure {
    public long id;
    public String name;
    public String photoPath;
    public String length;
    public String width;
    public String height;

    public Figure(long id, String name, String photoPath, String length, String width, String height) {
        this.id = id;
        this.name = name;
        this.photoPath = photoPath;
        this.length = length;
        this.width = width;
        this.height = height;
    }

    public String sizeText() {
        StringBuilder sb = new StringBuilder();
        if (length != null && !length.isEmpty()) sb.append("长 ").append(length);
        if (width != null && !width.isEmpty()) {
            if (sb.length() > 0) sb.append("    ");
            sb.append("宽 ").append(width);
        }
        if (height != null && !height.isEmpty()) {
            if (sb.length() > 0) sb.append("    ");
            sb.append("高 ").append(height);
        }
        return sb.length() == 0 ? "未记录尺寸" : sb.toString();
    }
}
