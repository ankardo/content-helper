package com.contenthelper.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostAdvisorTest {

    private final PostAdvisor advisor = new PostAdvisor();

    @Test
    void personalStoryGetsTextAndImage() {
        String notes =
                """
                The problem I wanted to solve
                I finish work and then I do not write about it. I watched smaller projects get attention.
                What I tried before
                I kept a notes file. I opened LinkedIn, wrote two lines, deleted them.
                What I built
                A small tool. It runs a local model and writes drafts.
                The meta part
                This post started as notes. I edited it, cut what was not true, and I am posting it myself.
                """;
        PostAdvice advice = advisor.advise(notes);
        assertThat(advice.type()).isEqualTo(PostType.STORY);
        assertThat(advice.shape()).isEqualTo(PostShape.TEXT_IMAGE);
        assertThat(advice.nextStep()).contains("draft");
        assertThat(advice.nextStep()).contains("screenshot");
    }

    @Test
    void howToGetsACarousel() {
        String notes =
                """
                How to build a CLI in Java
                Step 1: pick a command framework.
                Step 2: read the notes from a file.
                Step 3: call the model.
                Step 4: write the drafts.
                """;
        PostAdvice advice = advisor.advise(notes);
        assertThat(advice.type()).isEqualTo(PostType.GUIDE);
        assertThat(advice.shape()).isEqualTo(PostShape.CAROUSEL);
        assertThat(advice.nextStep()).contains("carousel");
    }

    @Test
    void comparisonGetsACarousel() {
        String notes =
                """
                React vs Angular
                Compared to Angular, React gives you hooks. Angular gives you a full framework.
                I compared both and the trade off is real.
                """;
        PostAdvice advice = advisor.advise(notes);
        assertThat(advice.type()).isEqualTo(PostType.COMPARISON);
        assertThat(advice.shape()).isEqualTo(PostShape.CAROUSEL);
    }

    @Test
    void frameworkGetsACarousel() {
        String notes =
                """
                A framework with three pillars
                The framework has three pillars. The components are independent and the principles are simple.
                """;
        PostAdvice advice = advisor.advise(notes);
        assertThat(advice.type()).isEqualTo(PostType.FRAMEWORK);
        assertThat(advice.shape()).isEqualTo(PostShape.CAROUSEL);
    }

    @Test
    void buildDetailsGetACarousel() {
        String notes =
                """
                The architecture of the tool
                The architecture is a shell around a model. The stack is Java and Spring.
                Tests cover every command. CI runs the tests. Two bugs showed up in the build.
                """;
        PostAdvice advice = advisor.advise(notes);
        assertThat(advice.type()).isEqualTo(PostType.BUILD_BREAKDOWN);
        assertThat(advice.shape()).isEqualTo(PostShape.CAROUSEL);
    }

    @Test
    void emptyNotesStayAStory() {
        PostAdvice advice = advisor.advise("");
        assertThat(advice.type()).isEqualTo(PostType.STORY);
        assertThat(advice.reason()).isNotBlank();
    }
}
