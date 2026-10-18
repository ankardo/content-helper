package com.contenthelper.draft;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Renders the three drafts from a source text. Canned structure in v1 on
 * purpose: issue 2 puts these templates behind the AI chat client.
 */
@Service
public class DraftTemplates {

    public String linkedin(String source) {
        return "# LinkedIn draft\n\n"
                + "What I built this week:\n\n"
                + source.trim()
                + "\n\n---\n"
                + "Takeaway: ship small, write it down, repeat.\n"
                + "\n#buildinpublic #java #springboot\n";
    }

    public String x(String source) {
        String firstLine = source.trim().lines().findFirst().orElse("").trim();
        if (firstLine.length() > 200) {
            firstLine = firstLine.substring(0, 200).trim() + "...";
        }
        return firstLine + "\n\nShipped today. Notes below.\n";
    }

    public String blog(String source, String slug) {
        return "---\n"
                + "title: \"" + slug + "\"\n"
                + "date: " + LocalDate.now() + "\n"
                + "---\n\n"
                + "# " + slug + "\n\n"
                + source.trim()
                + "\n\n## Notes\n\nWrite the longer story here.\n";
    }

    /** Canned blog body, used when the model is unavailable. Front matter is added by blogPost. */
    public String blogBody(String source) {
        return source.trim() + "\n";
    }

    /**
     * Blog post with front matter matching the Astro blog schema
     * (title, description, pubDate, optional tags). Body is passed in whole.
     */
    public String blogPost(String title, String description, LocalDate pubDate, List<String> tags, String body) {
        StringBuilder out = new StringBuilder();
        out.append("---\n");
        out.append("title: \"").append(escape(title)).append("\"\n");
        out.append("description: \"").append(escape(description)).append("\"\n");
        out.append("pubDate: \"").append(pubDate).append("\"\n");
        if (tags != null && !tags.isEmpty()) {
            out.append("tags: [")
                    .append(tags.stream().map(t -> "\"" + escape(t) + "\"").collect(Collectors.joining(", ")))
                    .append("]\n");
        }
        out.append("---\n\n");
        out.append(body.trim()).append("\n");
        return out.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
