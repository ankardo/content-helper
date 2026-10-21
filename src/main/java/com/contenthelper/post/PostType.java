package com.contenthelper.post;

/** What kind of post the notes describe. */
public enum PostType {
    STORY("personal story"),
    GUIDE("step by step guide"),
    FRAMEWORK("framework"),
    COMPARISON("comparison"),
    BUILD_BREAKDOWN("build breakdown");

    private final String label;

    PostType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean wantsCarousel() {
        return this != STORY;
    }
}
