package com.contenthelper.shell;

import com.contenthelper.ai.DraftGenerator;
import com.contenthelper.blog.BlogRepo;
import com.contenthelper.draft.DraftTemplates;
import com.contenthelper.draft.DraftWriter;
import com.contenthelper.git.GitLogReader;
import com.contenthelper.observe.RunMetrics;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
public class DraftCommands {

    private static final Logger log = LoggerFactory.getLogger(DraftCommands.class);
    private static final String USAGE = "Pass --notes-file <file> (sentences), --notes <words> or --range <git range>";

    private final DraftTemplates templates;
    private final DraftWriter writer;
    private final GitLogReader git;
    private final DraftGenerator generator;
    private final BlogRepo repo;
    private final RunMetrics metrics;

    public DraftCommands(
            DraftTemplates templates,
            DraftWriter writer,
            GitLogReader git,
            DraftGenerator generator,
            BlogRepo repo,
            RunMetrics metrics) {
        this.templates = templates;
        this.writer = writer;
        this.git = git;
        this.generator = generator;
        this.repo = repo;
        this.metrics = metrics;
    }

    @Command(
            name = {"draft"},
            description = "Draft LinkedIn, X and blog posts from notes or a commit range.")
    public String draft(
            @Option(longName = "notes", defaultValue = "") String notes,
            @Option(longName = "notes-file", defaultValue = "") String notesFile,
            @Option(longName = "range", defaultValue = "") String range,
            @Option(longName = "dir", defaultValue = ".") String dir,
            @Option(longName = "out", defaultValue = "drafts") String out,
            @Option(longName = "ai", defaultValue = "true") boolean ai)
            throws Exception {
        long start = System.nanoTime();
        Source input = readSource(notes, notesFile, range, dir);
        if (input.error() != null) {
            return input.error();
        }
        if (input.text() == null) {
            return USAGE;
        }
        String source = input.text();

        String slug = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String linkedin;
        String x;
        String blog;
        String note = "";
        if (ai) {
            try {
                DraftGenerator.Drafts drafts = generator.generateAll(source, slug);
                linkedin = drafts.linkedin();
                x = drafts.x();
                blog = drafts.blog();
            } catch (Exception e) {
                log.warn("AI generation failed, falling back to canned drafts", e);
                note = " (model unreachable, canned drafts)";
                linkedin = templates.linkedin(source);
                x = templates.x(source);
                blog = templates.blog(source, slug);
            }
        } else {
            linkedin = templates.linkedin(source);
            x = templates.x(source);
            blog = templates.blog(source, slug);
        }
        List<Path> written = writer.write(Path.of(out), slug, linkedin, x, blog);
        boolean fallback = !note.isBlank();
        record("draft", ai && !fallback, written.size(), start, fallback);
        return "Wrote " + written.size() + " drafts to " + out + "/" + slug + note;
    }

    @Command(
            name = {"blog"},
            description = "Write a blog post into the blog repo pending area. Not published until approved.")
    public String blog(
            @Option(longName = "notes", defaultValue = "") String notes,
            @Option(longName = "notes-file", defaultValue = "") String notesFile,
            @Option(longName = "range", defaultValue = "") String range,
            @Option(longName = "dir", defaultValue = ".") String dir,
            @Option(longName = "into", defaultValue = "") String into,
            @Option(longName = "title", defaultValue = "") String title,
            @Option(longName = "description", defaultValue = "") String description,
            @Option(longName = "tags", defaultValue = "") String tags,
            @Option(longName = "slug", defaultValue = "") String slug,
            @Option(longName = "ai", defaultValue = "true") boolean ai)
            throws Exception {
        long start = System.nanoTime();
        Source input = readSource(notes, notesFile, range, dir);
        if (input.error() != null) {
            return input.error();
        }
        if (input.text() == null) {
            return USAGE;
        }
        if (into == null || into.isBlank()) {
            return "Pass --into <blog repo> (the Astro project holding src/content)";
        }
        Path blogRepo = Path.of(into);
        if (!repo.looksLikeBlogRepo(blogRepo)) {
            return into + " does not look like the blog repo, no src/content found";
        }
        String source = input.text();
        Map<String, String> meta = input.meta();
        String theTitle = firstNonBlank(title, meta.get("title"), firstLine(source));
        String theSlug = firstNonBlank(slug, meta.get("slug"));
        theSlug = theSlug.isEmpty() ? slugify(theTitle) : theSlug;

        String body;
        String note = "";
        if (ai) {
            try {
                body = generator.blogBody(source);
            } catch (Exception e) {
                log.warn("AI generation failed, falling back to canned blog body", e);
                note = " (model unreachable, canned body)";
                body = templates.blogBody(source);
            }
        } else {
            body = templates.blogBody(source);
        }
        String theDescription = firstNonBlank(description, meta.get("description"), firstLine(body));
        String tagsValue = firstNonBlank(tags, meta.get("tags"));
        String content = templates.blogPost(theTitle, theDescription, LocalDate.now(), tagList(tagsValue), body);
        try {
            Path path = repo.writePending(blogRepo, theSlug, content);
            boolean fallback = !note.isBlank();
            record("blog", ai && !fallback, 1, start, fallback);
            return "Pending: "
                    + path
                    + note
                    + "\nAwaiting your go. Publish with: approve --into "
                    + into
                    + " --slug "
                    + theSlug
                    + " --go";
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    @Command(
            name = {"pending"},
            description = "List blog drafts awaiting your go.")
    public String pending(@Option(longName = "into", defaultValue = "") String into) throws Exception {
        if (into == null || into.isBlank()) {
            return "Pass --into <blog repo>";
        }
        Path blogRepo = Path.of(into);
        if (!repo.looksLikeBlogRepo(blogRepo)) {
            return into + " does not look like the blog repo, no src/content found";
        }
        List<String> pending = repo.pending(blogRepo);
        return pending.isEmpty() ? "Nothing pending." : "Pending, awaiting your go:\n" + String.join("\n", pending);
    }

    @Command(
            name = {"approve"},
            description = "Publish a pending blog draft. Refuses without --go.")
    public String approve(
            @Option(longName = "into", defaultValue = "") String into,
            @Option(longName = "slug", defaultValue = "") String slug,
            @Option(longName = "go", defaultValue = "false") boolean go)
            throws Exception {
        if (into == null || into.isBlank()) {
            return "Pass --into <blog repo>";
        }
        Path blogRepo = Path.of(into);
        if (!repo.looksLikeBlogRepo(blogRepo)) {
            return into + " does not look like the blog repo, no src/content found";
        }
        String theSlug = slug;
        if (isBlank(theSlug)) {
            List<String> pending = repo.pending(blogRepo);
            if (pending.size() != 1) {
                return "Pass --slug <slug>. Pending: " + (pending.isEmpty() ? "none" : pending);
            }
            theSlug = pending.get(0);
        }
        if (!go) {
            return "Dry run: would publish "
                    + repo.pendingPath(blogRepo, theSlug)
                    + " to "
                    + repo.publishedPath(blogRepo, theSlug)
                    + ". Re-run with --go after your explicit go.";
        }
        try {
            Path moved = repo.approve(blogRepo, theSlug);
            return "Published: " + moved;
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    private Source readSource(String notes, String notesFile, String range, String dir) throws Exception {
        if (!isBlank(notesFile)) {
            // Full sentences: the shell splits --notes and --title on spaces, a file keeps them whole.
            String text;
            try {
                text = Files.readString(Path.of(notesFile)).trim();
            } catch (Exception e) {
                return new Source(null, "Cannot read notes file " + notesFile + ": " + e.getMessage(), Map.of());
            }
            if (text.isBlank()) {
                return new Source(null, "Notes file " + notesFile + " is empty", Map.of());
            }
            return splitMeta(text);
        }
        if (!isBlank(notes)) {
            return new Source(notes, null, Map.of());
        }
        if (!isBlank(range)) {
            List<String> lines = git.readLog(Path.of(dir), range);
            return lines.isEmpty()
                    ? new Source(null, "No commits in range " + range, Map.of())
                    : new Source(String.join("\n", lines), null, Map.of());
        }
        return new Source(null, null, Map.of());
    }

    /**
     * A notes file may open with a "---" block of metadata (title, description, tags, slug),
     * the reliable path around the shell splitting option values on whitespace.
     */
    private static Source splitMeta(String text) {
        String[] lines = text.split("\n", -1);
        if (lines.length == 0 || !lines[0].trim().equals("---")) {
            return new Source(text, null, Map.of());
        }
        int end = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().equals("---")) {
                end = i;
                break;
            }
        }
        if (end < 0) {
            return new Source(text, null, Map.of());
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
        return new Source(body, null, meta);
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private void record(String command, boolean ai, int outputs, long start, boolean fallback) {
        String mode = ai ? "ai" : "canned";
        long millis = (System.nanoTime() - start) / 1_000_000;
        metrics.run(command, mode, millis, fallback);
        log.info(
                "run command={} mode={} duration_ms={} outputs={} fallback={}",
                command,
                mode,
                millis,
                outputs,
                fallback);
    }

    private static List<String> tagList(String tags) {
        if (isBlank(tags)) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(t -> !t.isBlank())
                .toList();
    }

    static String slugify(String title) {
        String slug = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
        if (slug.length() > 60) {
            slug = slug.substring(0, 60);
            int lastDash = slug.lastIndexOf('-');
            if (lastDash > 20) {
                slug = slug.substring(0, lastDash);
            }
            slug = slug.replaceAll("-+$", "");
        }
        return slug.isBlank() ? "post" : slug;
    }

    private static String firstLine(String text) {
        return Arrays.stream(text.split("\n"))
                .map(String::trim)
                .map(line -> line.replaceAll("^#+\\s*", ""))
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Source(String text, String error, Map<String, String> meta) {}
}
