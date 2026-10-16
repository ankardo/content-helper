package com.contenthelper.shell;

import com.contenthelper.ai.DraftGenerator;
import com.contenthelper.draft.DraftTemplates;
import com.contenthelper.draft.DraftWriter;
import com.contenthelper.git.GitLogReader;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
public class DraftCommands {

  private static final Logger log = LoggerFactory.getLogger(DraftCommands.class);

  private final DraftTemplates templates;
  private final DraftWriter writer;
  private final GitLogReader git;
  private final DraftGenerator generator;

  public DraftCommands(
      DraftTemplates templates, DraftWriter writer, GitLogReader git, DraftGenerator generator) {
    this.templates = templates;
    this.writer = writer;
    this.git = git;
    this.generator = generator;
  }

  @Command(name = {"draft"}, description = "Draft LinkedIn, X and blog posts from notes or a commit range.")
  public String draft(
      @Option(longName = "notes", defaultValue = "") String notes,
      @Option(longName = "notes-file", defaultValue = "") String notesFile,
      @Option(longName = "range", defaultValue = "") String range,
      @Option(longName = "dir", defaultValue = ".") String dir,
      @Option(longName = "out", defaultValue = "drafts") String out,
      @Option(longName = "ai", defaultValue = "true") boolean ai) throws Exception {
    String source;
    if (notesFile != null && !notesFile.isBlank()) {
      // Full sentences: the shell splits --notes on spaces, a file keeps them whole.
      try {
        source = java.nio.file.Files.readString(Path.of(notesFile)).trim();
      } catch (Exception e) {
        return "Cannot read notes file " + notesFile + ": " + e.getMessage();
      }
      if (source.isBlank()) {
        return "Notes file " + notesFile + " is empty";
      }
    } else if (notes != null && !notes.isBlank()) {
      source = notes;
    } else if (range != null && !range.isBlank()) {
      List<String> lines = git.readLog(Path.of(dir), range);
      if (lines.isEmpty()) {
        return "No commits in range " + range;
      }
      source = String.join("\n", lines);
    } else {
      return "Pass --notes-file <file> (sentences), --notes <words> or --range <git range>";
    }
    String slug = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    String linkedin;
    String x;
    String blog;
    String note = "";
    if (ai) {
      try {
        linkedin = generator.linkedin(source);
        x = generator.x(source);
        blog = generator.blog(source, slug);
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
    return "Wrote " + written.size() + " drafts to " + out + "/" + slug + note;
  }
}
