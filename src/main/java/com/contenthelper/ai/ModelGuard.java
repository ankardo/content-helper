package com.contenthelper.ai;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Bounds every model call. A single call may not run longer than the configured timeout, and a
 * failed or timed out call is retried a bounded number of times before the caller sees the failure.
 * Resilience4j owns both the timeout and the retry; the OpenAI SDK retry is turned off so there is
 * one retry owner.
 */
@Component
public class ModelGuard {

    private final TimeLimiter timeLimiter;
    private final Retry retry;
    private final ExecutorService executor;

    public ModelGuard(
            @Value("${content-helper.model.timeout:30s}") Duration timeout,
            @Value("${content-helper.model.retry-attempts:3}") int attempts,
            @Value("${content-helper.model.retry-delay:500ms}") Duration delay) {
        this.timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(timeout)
                .cancelRunningFuture(true)
                .build());
        this.retry = Retry.of(
                "model",
                RetryConfig.custom()
                        .maxAttempts(Math.max(1, attempts))
                        .waitDuration(delay)
                        .build());
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /** Runs one bounded, retried model call. Throws when the timeout or the retries are exhausted. */
    public <T> T call(Callable<T> action) throws Exception {
        return retry.executeCallable(() -> timeLimiter.executeFutureSupplier(() -> executor.submit(action)));
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
