package com.contenthelper.post;

/** The format that fits the post type. */
public enum PostShape {
    TEXT_IMAGE("text plus one image"),
    CAROUSEL("carousel plus short text hook");

    private final String label;

    PostShape(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
