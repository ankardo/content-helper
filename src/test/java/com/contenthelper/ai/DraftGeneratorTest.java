package com.contenthelper.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class DraftGeneratorTest {

  private final ModelGuard guard = new ModelGuard(Duration.ofSeconds(30), 1, Duration.ZERO);

  private DraftGenerator generator(String reply) {
    ChatClient.Builder builder = mock(ChatClient.Builder.class);
    ChatClient chat = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(builder.build()).thenReturn(chat);
    when(chat.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(reply);
    return new DraftGenerator(builder, guard);
  }

  @Test
  void passesSourceThrough() throws Exception {
    DraftGenerator gen = generator("a post");
    assertThat(gen.linkedin("my work")).isEqualTo("a post");
    assertThat(gen.x("my work")).isEqualTo("a post");
    assertThat(gen.blog("my work", "s")).isEqualTo("a post");
  }

  @Test
  void blankReplyFails() {
    DraftGenerator gen = generator("  ");
    assertThatThrownBy(() -> gen.linkedin("my work")).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void generateAllReturnsTheThreeDrafts() throws Exception {
    DraftGenerator gen = generator("a post");
    DraftGenerator.Drafts drafts = gen.generateAll("my work", "s");
    assertThat(drafts.linkedin()).isEqualTo("a post");
    assertThat(drafts.x()).isEqualTo("a post");
    assertThat(drafts.blog()).isEqualTo("a post");
  }

  @Test
  void generateAllPropagatesFailure() {
    DraftGenerator gen = generator("  ");
    assertThatThrownBy(() -> gen.generateAll("my work", "s"))
        .isInstanceOf(ExecutionException.class);
  }

  @Test
  void generateAllRunsTheCallsConcurrently() throws Exception {
    ChatClient.Builder builder = mock(ChatClient.Builder.class);
    ChatClient chat = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(builder.build()).thenReturn(chat);
    when(chat.prompt().system(anyString()).user(anyString()).call().content())
        .thenAnswer(
            invocation -> {
              Thread.sleep(200);
              return "a post";
            });
    DraftGenerator gen = new DraftGenerator(builder, guard);
    long start = System.nanoTime();
    gen.generateAll("my work", "s");
    long millis = (System.nanoTime() - start) / 1_000_000;
    assertThat(millis).isLessThan(450);
  }
}
