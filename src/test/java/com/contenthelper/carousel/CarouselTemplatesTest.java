package com.contenthelper.carousel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CarouselTemplatesTest {

    private final CarouselTemplates templates = new CarouselTemplates();

    @Test
    void cannedPlanHasEightSlides() {
        CarouselPlan plan = templates.plan("First line\nSecond line\nThird line\nFourth line\nFifth line");
        assertThat(plan.slides()).hasSize(8);
        assertThat(plan.slides().get(0).kind()).isEqualTo(CarouselSlide.Kind.HOOK);
        assertThat(plan.slides().get(7).kind()).isEqualTo(CarouselSlide.Kind.CTA);
        assertThat(plan.caption()).contains("Swipe");
    }

    @Test
    void shortSourceStillHasEightSlides() {
        CarouselPlan plan = templates.plan("One line only");
        assertThat(plan.slides()).hasSize(8);
        assertThat(plan.slides().get(7).kind()).isEqualTo(CarouselSlide.Kind.CTA);
    }

    @Test
    void headingStaysShort() {
        CarouselPlan plan = templates.plan("one two three four five six seven eight nine ten eleven twelve");
        assertThat(plan.slides().get(0).heading().split(" ")).hasSize(8);
    }
}
