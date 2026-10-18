package com.contenthelper.shell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.contenthelper.ai.DraftGenerator;
import com.contenthelper.blog.BlogRepo;
import com.contenthelper.draft.DraftTemplates;
import com.contenthelper.draft.DraftWriter;
import com.contenthelper.git.GitLogReader;
import com.contenthelper.observe.RunMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.client.ChatClient;

class DraftCommandsTest {

  private DraftCommands canned() {
    return new DraftCommands(
        new DraftTemplates(),
        new DraftWriter(),
        new GitLogReader(),
        null,
        new BlogRepo(),
        new RunMetrics(new SimpleMeterRegistry()));
  }

  private DraftCommands wired(String reply) {
    ChatClient.Builder builder = mock(ChatClient.Builder.class);
    ChatClient chat = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(builder.build()).thenReturn(chat);
    when(chat.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(reply);
    return new DraftCommands(
        new DraftTemplates(),
        new DraftWriter(),
        new GitLogReader(),
        new DraftGenerator(builder),
        new BlogRepo(),
        new RunMetrics(new SimpleMeterRegistry()));
  }

  private Path blogRepo(Path base) throws Exception {
    Files.createDirectories(base.resolve("src/content/blog"));
    return base;
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
    String out =
        canned().draft(null, notes.toString(), null, ".", base.resolve("out").toString(), false);
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
            new DraftGenerator(builder),
            new BlogRepo(),
            new RunMetrics(new SimpleMeterRegistry()));
    String out = commands.draft("Shipped it.", null, null, ".", base.toString(), true);
    assertThat(out).contains("canned drafts");
  }

  @Test
  void recordsRunMetrics(@TempDir Path base) throws Exception {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    DraftCommands commands =
        new DraftCommands(
            new DraftTemplates(),
            new DraftWriter(),
            new GitLogReader(),
            null,
            new BlogRepo(),
            new RunMetrics(registry));
    commands.draft("Shipped it.", null, null, ".", base.toString(), false);
    assertThat(
            registry
                .get("content_helper.runs")
                .tag("command", "draft")
                .tag("mode", "canned")
                .counter()
                .count())
        .isEqualTo(1.0);
    assertThat(registry.get("content_helper.run.duration").tag("command", "draft").timer().count())
        .isEqualTo(1);
    assertThat(registry.find("content_helper.fallbacks").counter()).isNull();
  }

  @Test
  void recordsFallbackMetric(@TempDir Path base) throws Exception {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
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
            new DraftGenerator(builder),
            new BlogRepo(),
            new RunMetrics(registry));
    commands.draft("Shipped it.", null, null, ".", base.toString(), true);
    assertThat(
            registry
                .get("content_helper.fallbacks")
                .tag("command", "draft")
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  @Test
  void blogWritesToPendingNotToContent(@TempDir Path base) throws Exception {
    Path repo = blogRepo(base);
    String out =
        canned()
            .blog(
                "Some notes to draft from.", null, null, ".", repo.toString(), "My First Post",
                null, "Java, Astro", null, false);
    assertThat(out).startsWith("Pending:");
    Path pending = repo.resolve("src/content/_pending/my-first-post.md");
    assertThat(pending).exists();
    String content = Files.readString(pending);
    assertThat(content).contains("title: \"My First Post\"");
    assertThat(content).contains("pubDate:");
    assertThat(content).contains("tags: [\"Java\", \"Astro\"]");
    try (var names = Files.list(repo.resolve("src/content/blog"))) {
      assertThat(names.toList()).isEmpty();
    }
  }

  @Test
  void blogWithAiUsesModelBodyOnly(@TempDir Path base) throws Exception {
    Path repo = blogRepo(base);
    String out =
        wired("A clean markdown body.")
            .blog("notes", null, null, ".", repo.toString(), "T", null, null, null, true);
    assertThat(out).startsWith("Pending:");
    String content = Files.readString(repo.resolve("src/content/_pending/t.md"));
    assertThat(content).contains("A clean markdown body.");
    assertThat(content).contains("title: \"T\"");
  }

  @Test
  void blogUsesFileHeaderForMetadata(@TempDir Path base) throws Exception {
    Path repo = blogRepo(base);
    Path notes = base.resolve("notes.txt");
    Files.writeString(
        notes,
        "---\ntitle: A Real Title With Spaces\ndescription: A short blurb.\ntags: Java, Astro\nslug: custom-slug\n---\nThe notes body.");
    String out =
        canned()
            .blog(null, notes.toString(), null, ".", repo.toString(), null, null, null, null, false);
    assertThat(out).startsWith("Pending:");
    Path pending = repo.resolve("src/content/_pending/custom-slug.md");
    assertThat(pending).exists();
    String content = Files.readString(pending);
    assertThat(content).contains("title: \"A Real Title With Spaces\"");
    assertThat(content).contains("description: \"A short blurb.\"");
    assertThat(content).contains("tags: [\"Java\", \"Astro\"]");
    assertThat(content).contains("The notes body.");
    assertThat(content).doesNotContain("slug: custom-slug");
  }

  @Test
  void blogRefusesDuplicate(@TempDir Path base) throws Exception {
    Path repo = blogRepo(base);
    canned().blog("notes", null, null, ".", repo.toString(), "Twice", null, null, null, false);
    String out =
        canned().blog("notes", null, null, ".", repo.toString(), "Twice", null, null, null, false);
    assertThat(out).contains("Already pending");
  }

  @Test
  void approveWithoutGoIsDryRun(@TempDir Path base) throws Exception {
    Path repo = blogRepo(base);
    canned().blog("notes", null, null, ".", repo.toString(), "Hold Me", null, null, null, false);
    String out = canned().approve(repo.toString(), "hold-me", false);
    assertThat(out).startsWith("Dry run");
    assertThat(repo.resolve("src/content/_pending/hold-me.md")).exists();
    assertThat(repo.resolve("src/content/blog/hold-me.md")).doesNotExist();
  }

  @Test
  void approveWithGoPublishes(@TempDir Path base) throws Exception {
    Path repo = blogRepo(base);
    canned().blog("notes", null, null, ".", repo.toString(), "Ship Me", null, null, null, false);
    String out = canned().approve(repo.toString(), "ship-me", true);
    assertThat(out).startsWith("Published:");
    assertThat(repo.resolve("src/content/blog/ship-me.md")).exists();
    assertThat(repo.resolve("src/content/_pending/ship-me.md")).doesNotExist();
  }

  @Test
  void approveRefusesExistingPublished(@TempDir Path base) throws Exception {
    Path repo = blogRepo(base);
    Files.writeString(repo.resolve("src/content/blog/ship-me.md"), "already live");
    Files.createDirectories(repo.resolve("src/content/_pending"));
    Files.writeString(repo.resolve("src/content/_pending/ship-me.md"), "new draft");
    String out = canned().approve(repo.toString(), "ship-me", true);
    assertThat(out).contains("Already published");
    assertThat(Files.readString(repo.resolve("src/content/blog/ship-me.md")))
        .isEqualTo("already live");
  }

  @Test
  void pendingListsDrafts(@TempDir Path base) throws Exception {
    Path repo = blogRepo(base);
    canned().blog("notes", null, null, ".", repo.toString(), "One", null, null, null, false);
    canned().blog("notes", null, null, ".", repo.toString(), "Two", null, null, null, false);
    String out = canned().pending(repo.toString());
    assertThat(out).contains("one");
    assertThat(out).contains("two");
  }
}
