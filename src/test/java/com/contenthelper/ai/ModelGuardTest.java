package com.contenthelper.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ModelGuardTest {

    private final ModelGuard guard = new ModelGuard(Duration.ofMillis(50), 1, Duration.ZERO);

    @AfterEach
    void shutdown() {
        guard.shutdown();
    }

    @Test
    void perCallTimeoutCanBeLongerThanTheDefault() throws Exception {
        String result = guard.call(
                () -> {
                    Thread.sleep(150);
                    return "ok";
                },
                Duration.ofSeconds(2),
                1);
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void perCallTimeoutCanBeShorterThanTheDefault() {
        assertThatThrownBy(() -> guard.call(
                        () -> {
                            Thread.sleep(500);
                            return "ok";
                        },
                        Duration.ofMillis(20),
                        1))
                .isInstanceOf(TimeoutException.class);
    }
}
