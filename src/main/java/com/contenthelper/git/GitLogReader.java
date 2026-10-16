package com.contenthelper.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;

/** Reads commit subjects from a local git repo. Local reads only, no network. */
@Service
public class GitLogReader {

  public List<String> readLog(Path repo, String range) throws IOException, InterruptedException {
    Process process =
        new ProcessBuilder("git", "log", "--format=%h %s", range)
            .directory(repo.toFile())
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int code = process.waitFor();
    if (code != 0) {
      throw new IllegalStateException("git log failed in " + repo + ": " + output.trim());
    }
    return output.lines().filter(line -> !line.isBlank()).toList();
  }
}
