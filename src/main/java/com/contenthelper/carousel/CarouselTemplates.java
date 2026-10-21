package com.contenthelper.carousel;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Canned plan, used when the model is unavailable. Same shape as the model plan. */
@Service
public class CarouselTemplates {

    private static final int POINT_COUNT = 5;

    public CarouselPlan plan(String source) {
        List<String> lines =
                source.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
        String first = lines.isEmpty() ? "What I built" : lines.get(0);
        List<String> points = new ArrayList<>();
        for (int i = 0; i < POINT_COUNT; i++) {
            points.add(lines.isEmpty() ? first : lines.get(Math.min(i, lines.size() - 1)));
        }
        String takeaway = lines.isEmpty() ? "Ship small, write it down, repeat." : lines.get(lines.size() - 1);
        List<CarouselSlide> slides = List.of(
                CarouselSlide.hook(CarouselText.clampWords(first, 8), ""),
                CarouselSlide.point("The problem", CarouselText.clampWords(points.get(0), 25)),
                CarouselSlide.point("What I tried", CarouselText.clampWords(points.get(1), 25)),
                CarouselSlide.point("What I built", CarouselText.clampWords(points.get(2), 25)),
                CarouselSlide.point("What it is not", CarouselText.clampWords(points.get(3), 25)),
                CarouselSlide.point("Why it matters", CarouselText.clampWords(points.get(4), 25)),
                CarouselSlide.point("The takeaway", CarouselText.clampWords(takeaway, 25)),
                CarouselSlide.cta(
                        "What's your take?",
                        "Do you use AI for the friction or the thinking? Tell me in the comments."));
        return new CarouselPlan(slides, caption(first));
    }

    public String caption(String firstLine) {
        return CarouselText.clampWords(firstLine, 20)
                + "\n\nSwipe for the breakdown.\n\n#java #springboot #buildinpublic";
    }
}
