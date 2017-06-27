package com.github.tfkpayne.sparkrest;

/**
 * Created by tom on 27/06/2017.
 */
public class Page {

    private String path;
    private String content;


    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Page{" +
                "path='" + path + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
