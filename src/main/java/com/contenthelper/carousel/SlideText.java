package com.contenthelper.carousel;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

/** Word wrapping and fitting for the slide canvas. */
final class SlideText {

    private SlideText() {}

    static List<String> wrap(String text, FontMetrics metrics, float maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }
            wrapParagraph(paragraph.trim(), metrics, maxWidth, lines);
        }
        return lines;
    }

    private static void wrapParagraph(String text, FontMetrics metrics, float maxWidth, List<String> lines) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
                line.setLength(0);
            }
            String rest = word;
            while (metrics.stringWidth(rest) > maxWidth) {
                int cut = rest.length();
                while (cut > 1 && metrics.stringWidth(rest.substring(0, cut)) > maxWidth) {
                    cut--;
                }
                lines.add(rest.substring(0, cut));
                rest = rest.substring(cut);
            }
            line.append(rest);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
    }

    static String fit(String text, FontMetrics metrics, float maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return end == 0 ? "" : text.substring(0, end) + ellipsis;
    }
}
