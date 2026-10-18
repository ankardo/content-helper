package com.contenthelper;

import static org.assertj.core.api.Assertions.assertThat;

import com.contenthelper.ai.DraftGenerator;
import com.contenthelper.shell.DraftCommands;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Locks the outgoing model request shape. The shape was captured earlier by hand from a dummy
 * server; this freezes it so a change to the wire contract fails the build. Runs against
 * MockWebServer, so no live model and CI stays keyless.
 */
@SpringBootTest
class WireContractTest {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String COMPLETION =
      """
      {"id":"chatcmpl-1","object":"chat.completion","created":0,"model":"mock",
       "choices":[{"index":0,"message":{"role":"assistant","content":"a post"},
       "finish_reason":"stop"}]}
      """;

  private static final MockWebServer SERVER = start();

  private static MockWebServer start() {
    try {
      MockWebServer server = new MockWebServer();
      server.start();
      return server;
    } catch (Exception e) {
      throw new IllegalStateException("could not start MockWebServer", e);
    }
  }

  @DynamicPropertySource
  static void baseUrl(DynamicPropertyRegistry registry) {
    registry.add("spring.ai.openai.base-url", () -> SERVER.url("/v1").toString());
  }

  @Autowired DraftGenerator generator;
  @Autowired DraftCommands commands;

  @BeforeEach
  void reset() throws InterruptedException {
    SERVER.setDispatcher(new QueueDispatcher());
    while (SERVER.takeRequest(20, TimeUnit.MILLISECONDS) != null) {
      // discard leftover requests from the previous test
    }
  }

  @AfterAll
  static void stop() throws Exception {
    SERVER.shutdown();
  }

  @Test
  void outgoingRequestMatchesTheCapturedShape() throws Exception {
    SERVER.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(COMPLETION));

    generator.linkedin("Shipped the wire test.");

    RecordedRequest request = SERVER.takeRequest(5, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getPath()).isEqualTo("/v1/chat/completions");

    JsonNode body = JSON.readTree(request.getBody().readUtf8());
    assertThat(body.path("model").asText()).isEqualTo("meta/muse-glimmer");
    assertThat(body.path("temperature").asDouble()).isEqualTo(0.7);
    assertThat(body.path("max_completion_tokens").asInt()).isEqualTo(2000);

    JsonNode system = body.path("messages").get(0);
    assertThat(system.path("role").asText()).isEqualTo("system");
    assertThat(system.path("content").asText()).contains("ghostwriter");
    JsonNode user = body.path("messages").get(1);
    assertThat(user.path("role").asText()).isEqualTo("user");
    assertThat(user.path("content").asText()).contains("Shipped the wire test.");
  }

  @Test
  void non200TriggersTheCannedFallback(@TempDir Path out) throws Exception {
    alwaysAnswer(500, null, "");
    String result = commands.draft("Shipped the wire test.", null, null, ".", out.toString(), true);
    assertThat(result).contains("canned drafts");
  }

  @Test
  void malformedBodyTriggersTheCannedFallback(@TempDir Path out) throws Exception {
    alwaysAnswer(200, "application/json", "{not json");
    String result = commands.draft("Shipped the wire test.", null, null, ".", out.toString(), true);
    assertThat(result).contains("canned drafts");
  }

  private void alwaysAnswer(int status, String contentType, String body) {
    SERVER.setDispatcher(
        new Dispatcher() {
          @Override
          public MockResponse dispatch(RecordedRequest request) {
            MockResponse response = new MockResponse().setResponseCode(status).setBody(body);
            if (contentType != null) {
              response.setHeader("Content-Type", contentType);
            }
            return response;
          }
        });
  }
}
