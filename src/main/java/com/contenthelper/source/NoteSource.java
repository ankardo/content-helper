package com.contenthelper.source;

import java.util.Map;

/** One resolved input: the text, an error to show, and any file header metadata. */
public record NoteSource(String text, String error, Map<String, String> meta) {

    public NoteSource {
        meta = meta == null ? Map.of() : Map.copyOf(meta);
    }
}
