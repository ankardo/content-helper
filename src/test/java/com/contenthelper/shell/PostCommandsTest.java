package com.contenthelper.shell;

import static org.assertj.core.api.Assertions.assertThat;

import com.contenthelper.git.GitLogReader;
import com.contenthelper.post.PostAdvisor;
import com.contenthelper.post.PostSchedule;
import org.junit.jupiter.api.Test;

class PostCommandsTest {

    private final PostCommands commands = new PostCommands(new PostAdvisor(), new PostSchedule(), new GitLogReader());

    @Test
    void recommendsTextAndImageForAStory() throws Exception {
        String out = commands.post(
                "The problem I wanted to solve\nI finish work and never write about it. I tried notes files.",
                null,
                null,
                ".");
        assertThat(out).contains("Best shape: text plus one image");
        assertThat(out).contains("Type: personal story");
        assertThat(out).contains("Next: Run `draft` for the text");
        assertThat(out).contains("Window: ");
    }

    @Test
    void recommendsACarouselForAGuide() throws Exception {
        String out = commands.post(
                "How to build a CLI\nStep 1: read the notes.\nStep 2: call the model.\nStep 3: write the drafts.",
                null,
                null,
                ".");
        assertThat(out).contains("Best shape: carousel plus short text hook");
        assertThat(out).contains("Type: step by step guide");
        assertThat(out).contains("Next: Run `carousel`");
    }

    @Test
    void missingInputReturnsUsage() throws Exception {
        String out = commands.post(null, null, null, ".");
        assertThat(out).contains("--notes");
    }
}
