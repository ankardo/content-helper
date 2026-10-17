package com.contenthelper.draft;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DraftTemplatesTest {

  private final DraftTemplates templates = new DraftTemplates();

  @Test
  void linkedinKeepsSourceAndHasTags() {
    String out = templates.linkedin("Shipped the shell command.");
    assertThat(out).contains("Shipped the shell command.");
    assertThat(out).contains("#buildinpublic");
  }

  @Test
  void xStaysShort() {
    String out = templates.x("Shipped the shell command.\nSecond line here.");
    assertThat(out).contains("Shipped the shell command.");
    assertThat(out.length()).isLessThan(300);
  }

  @Test
  void blogHasFrontMatter() {
    String out = templates.blog("Some notes.", "20250101-120000");
    assertThat(out).contains("title: \"20250101-120000\"");
    assertThat(out).contains("Some notes.");
  }

  @Test
  void blogPostMatchesBlogSchema() {
    String out =
        templates.blogPost(
            "My Post", "A short blurb.", LocalDate.of(2026, 10, 17), List.of("Java", "Astro"), "Body here.");
    assertThat(out).contains("title: \"My Post\"");
    assertThat(out).contains("description: \"A short blurb.\"");
    assertThat(out).contains("pubDate: \"2026-10-17\"");
    assertThat(out).contains("tags: [\"Java\", \"Astro\"]");
    assertThat(out).doesNotContain("date:");
    assertThat(out).endsWith("Body here.\n");
  }

  @Test
  void blogPostOmitsTagsWhenEmpty() {
    String out = templates.blogPost("T", "d", LocalDate.of(2026, 1, 1), List.of(), "b");
    assertThat(out).doesNotContain("tags:");
  }

  @Test
  void blogPostEscapesQuotes() {
    String out = templates.blogPost("A \"quoted\" title", "d", LocalDate.of(2026, 1, 1), List.of(), "b");
    assertThat(out).contains("title: \"A \\\"quoted\\\" title\"");
  }
}
