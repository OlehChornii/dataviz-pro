package com.dataviz.service.export;

import java.util.Objects;

public final class ExportOptions {

    private final int    width;
    private final int    height;
    private final int    dpi;
    private final String title;
    private final String author;
    private final int    jpegQuality;

    public ExportOptions(int width, int height, int dpi) {
        this.width       = width;
        this.height      = height;
        this.dpi         = dpi;
        this.title       = "DataViz Export";
        this.author      = "DataViz Pro";
        this.jpegQuality = 90;
    }

    private ExportOptions(Builder b) {
        this.width       = b.width;
        this.height      = b.height;
        this.dpi         = b.dpi;
        this.title       = b.title;
        this.author      = b.author;
        this.jpegQuality = b.jpegQuality;
    }

    public int    getWidth()       { return width; }
    public int    getHeight()      { return height; }
    public int    getDpi()         { return dpi; }
    public String getTitle()       { return title; }
    public String getAuthor()      { return author; }
    public int    getJpegQuality() { return jpegQuality; }

    public static ExportOptions forPublication() {
        return new Builder().dpi(150).title("DataViz Export").build();
    }

    public static ExportOptions forPrint() {
        return new Builder().dpi(300).title("DataViz Export").build();
    }

    public static ExportOptions forScreen() {
        return new Builder().dpi(72).title("DataViz Export").build();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int    width       = 1920;
        private int    height      = 1080;
        private int    dpi         = 150;
        private String title       = "DataViz Export";
        private String author      = "DataViz Pro";
        private int    jpegQuality = 90;

        public Builder width(int v)       { this.width = v;        return this; }
        public Builder height(int v)      { this.height = v;       return this; }
        public Builder dpi(int v)         { this.dpi = v;          return this; }
        public Builder title(String v)    { this.title = Objects.requireNonNull(v); return this; }
        public Builder author(String v)   { this.author = v;        return this; }
        public Builder jpegQuality(int v) { this.jpegQuality = v;   return this; }
        public ExportOptions build()      { return new ExportOptions(this); }
    }
}