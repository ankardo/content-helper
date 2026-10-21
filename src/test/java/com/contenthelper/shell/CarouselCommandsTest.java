package com.contenthelper.shell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.contenthelper.ai.ModelGuard;
import com.contenthelper.carousel.CarouselGenerator;
import com.contenthelper.carousel.CarouselParser;
import com.contenthelper.carousel.CarouselRenderer;
import com.contenthelper.carousel.CarouselTemplates;
import com.contenthelper.carousel.CarouselWriter;
import com.contenthelper.git.GitLogReader;
import com.contenthelper.observe.RunMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.client.ChatClient;

class CarouselCommandsTest {

    private final ModelGuard guard = new ModelGuard(Duration.ofSeconds(30), 1, Duration.ZERO);

    private CarouselCommands canned(SimpleMeterRegistry registry) {
        return new CarouselCommands(
                new CarouselTemplates(),
                null,
                new CarouselWriter(new CarouselRenderer()),
                new GitLogReader(),
                new RunMetrics(registry));
    }

    private CarouselCommands wired(String reply, SimpleMeterRegistry registry) {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chat = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(builder.build()).thenReturn(chat);
        when(chat.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(reply);
        return new CarouselCommands(
                new CarouselTemplates(),
                new CarouselGenerator(builder, guard, new CarouselParser()),
                new CarouselWriter(new CarouselRenderer()),
                new GitLogReader(),
                new RunMetrics(registry));
    }

    private static String reply() {
        StringBuilder json = new StringBuilder("{\"slides\":[");
        for (int i = 0; i < 8; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"kind\":\"point\",\"heading\":\"H")
                    .append(i)
                    .append("\",\"body\":\"B")
                    .append(i)
                    .append("\"}");
        }
        return json.append("],\"caption\":\"Model caption\"}").toString();
    }

    @Test
    void cannedWritesPlanAndCaption(@TempDir Path base) throws Exception {
        String out = canned(new SimpleMeterRegistry())
                .carousel("First line\nSecond line", null, null, ".", base.toString(), false, 120, 2);
        assertThat(out).startsWith("Wrote a 8 slide carousel");
        List<Path> dirs;
        try (var paths = Files.list(base)) {
            dirs = paths.toList();
        }
        assertThat(dirs).hasSize(1);
        assertThat(dirs.get(0).resolve("plan.json")).exists();
        assertThat(dirs.get(0).resolve("caption.md")).exists();
        assertThat(dirs.get(0).resolve("slide-01.png")).exists();
        assertThat(dirs.get(0).resolve("slide-08.png")).exists();
        assertThat(dirs.get(0).resolve("carousel.pdf")).exists();
        assertThat(Files.readString(dirs.get(0).resolve("plan.json"))).contains("\"slides\"");
    }

    @Test
    void modelPlanIsUsedWhenItParses(@TempDir Path base) throws Exception {
        String out = wired(reply(), new SimpleMeterRegistry())
                .carousel("notes", null, null, ".", base.toString(), true, 120, 2);
        assertThat(out).startsWith("Wrote a 8 slide carousel");
        assertThat(out).doesNotContain("canned");
        try (var paths = Files.list(base)) {
            Path dir = paths.toList().get(0);
            assertThat(Files.readString(dir.resolve("caption.md"))).contains("Model caption");
        }
    }

    @Test
    void aiFailureFallsBackToCanned(@TempDir Path base) throws Exception {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chat = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(builder.build()).thenReturn(chat);
        when(chat.prompt().system(anyString()).user(anyString()).call()).thenThrow(new RuntimeException("down"));
        CarouselCommands commands = new CarouselCommands(
                new CarouselTemplates(),
                new CarouselGenerator(builder, guard, new CarouselParser()),
                new CarouselWriter(new CarouselRenderer()),
                new GitLogReader(),
                new RunMetrics(new SimpleMeterRegistry()));
        String out = commands.carousel("notes", null, null, ".", base.toString(), true, 120, 2);
        assertThat(out).contains("canned plan");
    }

    @Test
    void missingInputReturnsUsage(@TempDir Path base) throws Exception {
        String out = canned(new SimpleMeterRegistry()).carousel(null, null, null, ".", base.toString(), false, 120, 2);
        assertThat(out).contains("--notes");
    }

    @Test
    void recordsRunMetrics(@TempDir Path base) throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        canned(registry).carousel("notes", null, null, ".", base.toString(), false, 120, 2);
        assertThat(registry.get("content_helper.runs")
                        .tag("command", "carousel")
                        .tag("mode", "canned")
                        .counter()
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.find("content_helper.fallbacks").counter()).isNull();
    }
}
