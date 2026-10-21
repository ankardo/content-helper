package com.contenthelper.carousel;

import java.util.List;

/** The whole carousel: the ordered slides and the caption that ships with them. */
public record CarouselPlan(List<CarouselSlide> slides, String caption) {

    public CarouselPlan {
        slides = slides == null ? List.of() : List.copyOf(slides);
        caption = caption == null ? "" : caption.trim();
    }
}
