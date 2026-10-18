package com.contenthelper.draft;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DraftWriterTest {

    private final DraftWriter writer = new DraftWriter();

    @Test
    void writesThreeFiles(@TempDir Path base) throws Exception {
        List<Path> written = writer.write(base, "slug-1", "in", "x", "blog");
        assertThat(written).hasSize(3);
        assertThat(Files.readString(base.resolve("slug-1/linkedin.md"))).isEqualTo("in");
        assertThat(Files.readString(base.resolve("slug-1/x.md"))).isEqualTo("x");
        assertThat(Files.readString(base.resolve("slug-1/blog.md"))).isEqualTo("blog");
    }
}
