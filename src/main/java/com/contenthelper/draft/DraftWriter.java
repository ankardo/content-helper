package com.contenthelper.draft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;

/** Writes the three drafts as files. Files only, never publishes anywhere. */
@Service
public class DraftWriter {

  public List<Path> write(Path base, String slug, String linkedin, String x, String blog)
      throws IOException {
    Path dir = base.resolve(slug);
    Files.createDirectories(dir);
    Path a = Files.writeString(dir.resolve("linkedin.md"), linkedin);
    Path b = Files.writeString(dir.resolve("x.md"), x);
    Path c = Files.writeString(dir.resolve("blog.md"), blog);
    return List.of(a, b, c);
  }
}
