package com.contenthelper.source;

import com.contenthelper.git.GitLogReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the notes, notes file or git range input once, for every drafting command. */
public final class NoteSourceReader {

    public static final String USAGE = "Pass --notes-file <file> (sentences), --notes <words> or --range <git range>";

    private NoteSourceReader() {}

    public static NoteSource read(String notes, String notesFile, String range, String dir, GitLogReader git)
            throws Exception {
        if (!isBlank(notesFile)) {
            // Full sentences: the shell splits --notes and --title on spaces, a file keeps them whole.
            String text;
            try {
                text = Files.readString(Path.of(notesFile)).trim();
            } catch (Exception e) {
                return new NoteSource(null, "Cannot read notes file " + notesFile + ": " + e.getMessage(), Map.of());
            }
            if (text.isBlank()) {
                return new NoteSource(null, "Notes file " + notesFile + " is empty", Map.of());
            }
            return splitMeta(text);
        }
        if (!isBlank(notes)) {
            return new NoteSource(notes, null, Map.of());
        }
        if (!isBlank(range)) {
            List<String> lines = git.readLog(Path.of(dir), range);
            return lines.isEmpty()
                    ? new NoteSource(null, "No commits in range " + range, Map.of())
                    : new NoteSource(String.join("\n", lines), null, Map.of());
        }
        return new NoteSource(null, null, Map.of());
    }

    public static String firstLine(String text) {
        return Arrays.stream(text.split("\n"))
                .map(String::trim)
                .map(line -> line.replaceAll("^#+\\s*", ""))
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("");
    }

    /**
     * A notes file may open with a "---" block of metadata (title, description, tags, slug),
     * the reliable path around the shell splitting option values on whitespace.
     */
    private static NoteSource splitMeta(String text) {
        String[] lines = text.split("\n", -1);
        if (lines.length == 0 || !lines[0].trim().equals("---")) {
            return new NoteSource(text, null, Map.of());
        }
        int end = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().equals("---")) {
                end = i;
                break;
            }
        }
        if (end < 0) {
            return new NoteSource(text, null, Map.of());
        }
        Map<String, String> meta = new LinkedHashMap<>();
        for (int i = 1; i < end; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0) {
                meta.put(
                        lines[i].substring(0, colon).trim().toLowerCase(),
                        unquote(lines[i].substring(colon + 1).trim()));
            }
        }
        String body = String.join("\n", Arrays.copyOfRange(lines, end + 1, lines.length))
                .trim();
        return new NoteSource(body, null, meta);
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
