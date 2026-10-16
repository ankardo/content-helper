package com.contenthelper.shell;

import com.contenthelper.draft.DraftTemplates;
import com.contenthelper.draft.DraftWriter;
import com.contenthelper.git.GitLogReader;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
public class DraftCommands {

  private final DraftTemplates templates;
  private final DraftWriter writer;
  private final GitLogReader git;

  public DraftCommands(DraftTemplates templates, DraftWriter writer, GitLogReader git) {
    this.templates = templates;
    this.writer = writer;
    this.git = git;
  }

  @Command(name = {"draft"}, description = "Draft LinkedIn, X and blog posts from notes or a commit range.")
  public String draft(
      @Option(longName = "notes", defaultValue = "") String notes,
      @Option(longName = "range", defaultValue = "") String range,
      @Option(longName = "dir", defaultValue = ".") String dir,
      @Option(longName = "out", defaultValue = "drafts") String out) throws Exception {
    String source;
    if (notes != null && !notes.isBlank()) {
      source = notes;
    } else if (range != null && !range.isBlank()) {
      List<String> lines = git.readLog(Path.of(dir), range);
      if (lines.isEmpty()) {
        return "No commits in range " + range;
      }
      source = String.join("\n", lines);
    } else {
      return "Pass --notes \"...\" or --range HEAD~5..HEAD";
    }
    String slug = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    List<Path> written =
        writer.write(
            Path.of(out),
            slug,
            templates.linkedin(source),
            templates.x(source),
            templates.blog(source, slug));
    return "Wrote " + written.size() + " drafts to " + out + "/" + slug;
  }
}
