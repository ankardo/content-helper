package com.contenthelper.ai;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

  /** The three social drafts produced from one input. */
  public record Drafts(String linkedin, String x, String blog) {}

  /** Runs the three model calls concurrently on virtual threads. */
  public Drafts generateAll(String source, String slug)
      throws InterruptedException, ExecutionException {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<String> linkedin = executor.submit(() -> linkedin(source));
      Future<String> x = executor.submit(() -> x(source));
      Future<String> blog = executor.submit(() -> blog(source, slug));
      return new Drafts(linkedin.get(), x.get(), blog.get());
    }
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

  /** Blog body only. Front matter is written by code, never by the model. */
  public String blogBody(String source) {
    return ask(
        "Work notes:\n" + source + "\n\nWrite a short blog post in markdown, first person, "
            + "based on these notes. Output only the markdown body, no front matter.");
  }

  private String ask(String prompt) {
    String out = chat.prompt().system(ROLE).user(prompt).call().content();
    if (out == null || out.isBlank()) {
      throw new IllegalStateException("Empty reply from model");
    }
    return out.trim();
  }
}
