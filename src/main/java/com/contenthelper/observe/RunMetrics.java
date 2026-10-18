package com.contenthelper.observe;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/** Records one run: how many, how long, and whether it fell back to canned drafts. */
@Service
public class RunMetrics {

  private final MeterRegistry registry;

  public RunMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void run(String command, String mode, long millis, boolean fallback) {
    registry.counter("content_helper.runs", "command", command, "mode", mode).increment();
    registry
        .timer("content_helper.run.duration", "command", command)
        .record(millis, TimeUnit.MILLISECONDS);
    if (fallback) {
      registry.counter("content_helper.fallbacks", "command", command).increment();
    }
  }
}
