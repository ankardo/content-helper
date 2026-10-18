package com.contenthelper;

import static org.assertj.core.api.Assertions.assertThat;

import com.contenthelper.shell.DraftCommands;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Proves the model calls are bounded: a slow or erroring server leads to a retry then the canned
 * fallback, not an open ended wait. Uses a small OpenAI compatible mock server. The three drafts
 * run concurrently and each call is retried by Resilience4j up to three attempts, so up to nine
 * requests are expected. An exact count is not asserted: when the client aborts a slow request mid
 * flight, whether that attempt was already counted as a wire request depends on connection timing.
 */
@SpringBootTest(
    properties = {
      "content-helper.model.timeout=500ms",
      "content-helper.model.retry-attempts=3",
      "content-helper.model.retry-delay=50ms",
      "spring.ai.openai.max-retries=0"
    })
class ModelResilienceTest {

  private static final int MAX_ATTEMPTS = 9;
  private static final AtomicInteger requests = new AtomicInteger();
  private static volatile long delayMillis = 0;
  private static volatile int status = 200;

  private static final HttpServer SERVER = startServer();
  private static final String BASE_URL =
      "http://127.0.0.1:" + SERVER.getAddress().getPort() + "/v1";

  private static HttpServer startServer() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.setExecutor(Executors.newFixedThreadPool(16));
      server.createContext(
          "/v1/chat/completions",
          exchange -> {
            requests.incrementAndGet();
            try {
              exchange.getRequestBody().readAllBytes();
              if (delayMillis > 0) {
                Thread.sleep(delayMillis);
              }
              byte[] body = completion().getBytes();
              exchange.sendResponseHeaders(status, body.length);
              try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
              }
            } catch (Exception ignored) {
              // client gave up (timeout), nothing to answer
            } finally {
              exchange.close();
            }
          });
      server.start();
      return server;
    } catch (Exception e) {
      throw new IllegalStateException("could not start mock model server", e);
    }
  }

  private static String completion() {
    return """
        {"id":"chatcmpl-1","object":"chat.completion","created":0,"model":"mock",
         "choices":[{"index":0,"message":{"role":"assistant","content":"a post"},
         "finish_reason":"stop"}]}
        """;
  }

  @AfterAll
  static void stopServer() {
    SERVER.stop(0);
  }

  @DynamicPropertySource
  static void baseUrl(DynamicPropertyRegistry registry) {
    registry.add("spring.ai.openai.base-url", () -> BASE_URL);
  }

  @Autowired DraftCommands commands;

  @BeforeEach
  void reset() {
    requests.set(0);
    delayMillis = 0;
    status = 200;
  }

  @Test
  void slowModelFallsBackWithinBoundedTime(@TempDir Path out) throws Exception {
    delayMillis = 5000;
    long start = System.nanoTime();
    String result = commands.draft("Shipped it.", null, null, ".", out.toString(), true);
    long millis = (System.nanoTime() - start) / 1_000_000;
    assertThat(result).contains("canned drafts");
    assertThat(requests.get()).isGreaterThan(3).isLessThanOrEqualTo(MAX_ATTEMPTS);
    assertThat(millis).isLessThan(8000);
  }

  @Test
  void serverErrorsFallBackAfterRetries(@TempDir Path out) throws Exception {
    status = 503;
    long start = System.nanoTime();
    String result = commands.draft("Shipped it.", null, null, ".", out.toString(), true);
    long millis = (System.nanoTime() - start) / 1_000_000;
    assertThat(result).contains("canned drafts");
    assertThat(requests.get()).isGreaterThan(3).isLessThanOrEqualTo(MAX_ATTEMPTS);
    assertThat(millis).isLessThan(8000);
  }
}
