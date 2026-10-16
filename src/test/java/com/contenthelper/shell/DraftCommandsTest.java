package com.contenthelper.shell;

import static org.assertj.core.api.Assertions.assertThat;

import com.contenthelper.draft.DraftTemplates;
import com.contenthelper.draft.DraftWriter;
import com.contenthelper.git.GitLogReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DraftCommandsTest {

  private final DraftCommands commands =
      new DraftCommands(new DraftTemplates(), new DraftWriter(), new GitLogReader());

  @Test
  void missingInputsReturnsUsage(@TempDir Path base) throws Exception {
    String out = commands.draft(null, null, ".", base.toString());
    assertThat(out).contains("--notes");
  }

  @Test
  void notesInputWritesThreeFiles(@TempDir Path base) throws Exception {
    String out = commands.draft("Shipped it.", null, ".", base.toString());
    assertThat(out).startsWith("Wrote 3 drafts to");
    try (var paths = Files.list(base)) {
      assertThat(paths.toList()).hasSize(1);
    }
  }
}
