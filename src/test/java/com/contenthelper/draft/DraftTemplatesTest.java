package com.contenthelper.draft;

import static org.assertj.core.api.Assertions.assertThat;

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
}
