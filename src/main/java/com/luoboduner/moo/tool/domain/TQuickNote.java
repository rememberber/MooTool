package com.luoboduner.moo.tool.domain;

public class TQuickNote {
    private Integer id;

    private String name;

    private String content;

    private String createTime;

    private String modifiedTime;

    private String color;

    private String style;

    private String fontName;

    private String fontSize;

    private String syntax;

    private String lineWrap;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? null : content.trim();
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime == null ? null : createTime.trim();
    }

    public String getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime == null ? null : modifiedTime.trim();
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color == null ? null : color.trim();
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style == null ? null : style.trim();
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName == null ? null : fontName.trim();
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize == null ? null : fontSize.trim();
    }

    public String getSyntax() {
        return syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax == null ? null : syntax.trim();
    }

    public String getLineWrap() {
        return lineWrap;
    }

    public void setLineWrap(String lineWrap) {
        this.lineWrap = lineWrap == null ? null : lineWrap.trim();
    }
}