package com.contenthelper.carousel;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Canned plan, used when the model is unavailable. Same shape as the model plan. */
@Service
public class CarouselTemplates {

    private static final int POINT_COUNT = 6;
    private static final int MAX_POINT_WORDS = 30;
    private static final String[] FALLBACK_HEADINGS = {
        "The problem", "What I tried", "What I built", "What it is not", "Why it matters", "The takeaway"
    };

    public CarouselPlan plan(String source) {
        List<Section> sections = sections(source);
        if (sections.size() >= 2) {
            return sectionPlan(sections);
        }
        return sentencePlan(source);
    }

    private static CarouselPlan sectionPlan(List<Section> sections) {
        Section first = sections.get(0);
        List<CarouselSlide> slides = new ArrayList<>();
        slides.add(CarouselSlide.hook(
                CarouselText.clampWords(first.heading(), 8), CarouselText.clampWords(excerpt(first.body()), 24)));
        List<Section> rest = sections.subList(1, sections.size());
        for (int i = 0; i < POINT_COUNT; i++) {
            Section section =
                    i < rest.size() ? rest.get(i) : new Section(FALLBACK_HEADINGS[i], i == 0 ? first.body() : "");
            slides.add(CarouselSlide.point(
                    CarouselText.clampWords(section.heading(), 8),
                    CarouselText.clampWords(excerpt(section.body()), MAX_POINT_WORDS)));
        }
        slides.add(cta());
        return new CarouselPlan(slides, caption(first.heading()));
    }

    private static CarouselPlan sentencePlan(String source) {
        List<String> sentences = sentences(source);
        String first = sentences.isEmpty() ? "What I built" : sentences.get(0);
        List<CarouselSlide> slides = new ArrayList<>();
        slides.add(CarouselSlide.hook(CarouselText.clampWords(first, 8), ""));
        for (int i = 0; i < POINT_COUNT; i++) {
            String body = i + 1 < sentences.size() ? sentences.get(i + 1) : "";
            slides.add(CarouselSlide.point(FALLBACK_HEADINGS[i], CarouselText.clampWords(body, MAX_POINT_WORDS)));
        }
        slides.add(cta());
        return new CarouselPlan(slides, caption(first));
    }

    private static CarouselSlide cta() {
        return CarouselSlide.cta(
                "What's your take?", "Do you use AI for the friction or the thinking? Tell me in the comments.");
    }

    public static String caption(String firstLine) {
        return CarouselText.clampWords(firstLine, 20)
                + "\n\nSwipe for the breakdown.\n\n#java #springboot #buildinpublic";
    }

    private static String excerpt(String body) {
        List<String> sentences = sentences(body);
        StringBuilder out = new StringBuilder();
        for (String sentence : sentences) {
            String candidate = out.isEmpty() ? sentence : out + " " + sentence;
            if (CarouselText.wordCount(candidate) > MAX_POINT_WORDS) {
                break;
            }
            out.setLength(0);
            out.append(candidate);
            if (CarouselText.wordCount(out.toString()) >= 18) {
                break;
            }
        }
        return out.isEmpty() ? body : out.toString();
    }

    private static List<Section> sections(String source) {
        List<Section> sections = new ArrayList<>();
        String heading = null;
        StringBuilder body = new StringBuilder();
        for (String raw : source.lines().toList()) {
            String line = raw.trim();
            if (line.isBlank()) {
                continue;
            }
            if (isHeading(line)) {
                if (heading != null) {
                    sections.add(new Section(heading, body.toString().trim()));
                }
                heading = line;
                body.setLength(0);
            } else if (heading != null) {
                if (!body.isEmpty()) {
                    body.append(' ');
                }
                body.append(line);
            } else {
                heading = "What I built";
                body.append(line);
            }
        }
        if (heading != null) {
            sections.add(new Section(heading, body.toString().trim()));
        }
        return sections;
    }

    private static boolean isHeading(String line) {
        return CarouselText.wordCount(line) <= 10 && !line.matches(".*[.!?]$");
    }

    private static List<String> sentences(String text) {
        List<String> sentences = new ArrayList<>();
        for (String sentence : text.split("(?<=[.!?])\\s+")) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    private record Section(String heading, String body) {}
}
