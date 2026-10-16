package com.contenthelper.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class DraftGeneratorTest {

  private DraftGenerator generator(String reply) {
    ChatClient.Builder builder = mock(ChatClient.Builder.class);
    ChatClient chat = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(builder.build()).thenReturn(chat);
    when(chat.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(reply);
    return new DraftGenerator(builder);
  }

  @Test
  void passesSourceThrough() {
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
}
