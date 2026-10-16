package com.contenthelper.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** Generates drafts through the configured chat model. Local only, no cloud. */
@Service
public class DraftGenerator {

  private static final String ROLE =
      "You are a ghostwriter turning a developer's work notes into social media drafts.";

  private final ChatClient chat;

  public DraftGenerator(ChatClient.Builder builder) {
    this.chat = builder.build();
  }

  public String linkedin(String source) {
    return ask(
        "Work notes:\n" + source + "\n\nWrite a LinkedIn post, max 150 words, first person, "
            + "based on these notes. End with one takeaway line. Output only the post.");
  }

  public String x(String source) {
    return ask(
        "Work notes:\n" + source + "\n\nWrite a single X post, max 280 characters, first person, "
            + "based on these notes. Output only the post, no hashtags.");
  }

  public String blog(String source, String slug) {
    return ask(
        "Work notes:\n" + source + "\n\nWrite a short blog post in markdown with YAML front matter "
            + "(title \"" + slug + "\", date today), first person, based on these notes. "
            + "Output only the markdown.");
  }

  private String ask(String prompt) {
    String out = chat.prompt().system(ROLE).user(prompt).call().content();
    if (out == null || out.isBlank()) {
      throw new IllegalStateException("Empty reply from model");
    }
    return out.trim();
  }
}
