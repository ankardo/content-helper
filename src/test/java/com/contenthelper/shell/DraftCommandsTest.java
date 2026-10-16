package com.contenthelper.shell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.contenthelper.ai.DraftGenerator;
import com.contenthelper.draft.DraftTemplates;
import com.contenthelper.draft.DraftWriter;
import com.contenthelper.git.GitLogReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.client.ChatClient;

class DraftCommandsTest {

  private DraftCommands canned() {
    return new DraftCommands(new DraftTemplates(), new DraftWriter(), new GitLogReader(), null);
  }

  private DraftCommands wired(String reply) {
    ChatClient.Builder builder = mock(ChatClient.Builder.class);
    ChatClient chat = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(builder.build()).thenReturn(chat);
    when(chat.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(reply);
    DraftGenerator generator = new DraftGenerator(builder);
    return new DraftCommands(
        new DraftTemplates(), new DraftWriter(), new GitLogReader(), generator);
  }

  @Test
  void missingInputsReturnsUsage(@TempDir Path base) throws Exception {
    String out = canned().draft(null, null, null, ".", base.toString(), false);
    assertThat(out).contains("--notes");
  }

  @Test
  void notesInputWritesThreeFiles(@TempDir Path base) throws Exception {
    String out = canned().draft("Shipped it.", null, null, ".", base.toString(), false);
    assertThat(out).startsWith("Wrote 3 drafts to");
    try (var paths = Files.list(base)) {
      assertThat(paths.toList()).hasSize(1);
    }
  }

  @Test
  void notesFileKeepsSentencesWhole(@TempDir Path base) throws Exception {
    Path notes = base.resolve("notes.txt");
    Files.writeString(notes, "alpha bravo charlie delta");
    String out = canned().draft(null, notes.toString(), null, ".", base.resolve("out").toString(), false);
    assertThat(out).startsWith("Wrote 3 drafts to");
    try (var slugs = Files.list(base.resolve("out"))) {
      Path slug = slugs.toList().get(0);
      assertThat(Files.readString(slug.resolve("linkedin.md")))
          .contains("alpha bravo charlie delta");
    }
  }

  @Test
  void missingNotesFileExplainsItself(@TempDir Path base) throws Exception {
    String out =
        canned().draft(null, base.resolve("nope.txt").toString(), null, ".", base.toString(), false);
    assertThat(out).startsWith("Cannot read notes file");
  }

  @Test
  void aiPathUsesModel(@TempDir Path base) throws Exception {
    String out = wired("model words").draft("Shipped it.", null, null, ".", base.toString(), true);
    assertThat(out).startsWith("Wrote 3 drafts to");
    try (var slugs = Files.list(base)) {
      Path slug = slugs.toList().get(0);
      assertThat(Files.readString(slug.resolve("linkedin.md"))).isEqualTo("model words");
    }
  }

  @Test
  void aiFailureFallsBackToCanned(@TempDir Path base) throws Exception {
    ChatClient.Builder builder = mock(ChatClient.Builder.class);
    ChatClient chat = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(builder.build()).thenReturn(chat);
    when(chat.prompt().system(anyString()).user(anyString()).call())
        .thenThrow(new RuntimeException("down"));
    DraftCommands commands =
        new DraftCommands(
            new DraftTemplates(),
            new DraftWriter(),
            new GitLogReader(),
            new DraftGenerator(builder));
    String out = commands.draft("Shipped it.", null, null, ".", base.toString(), true);
    assertThat(out).contains("canned drafts");
  }
}
