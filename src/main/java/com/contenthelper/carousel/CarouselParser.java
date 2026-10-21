package com.contenthelper.carousel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Parses the model's JSON into a plan. Tolerates fences and prose around the
 * object, and clamps headings and bodies to slide sized text.
 */
@Service
public class CarouselParser {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MIN_SLIDES = 4;
    private static final int MAX_SLIDES = 10;
    private static final int MAX_HEADING_WORDS = 10;
    private static final int MAX_BODY_WORDS = 40;

    public CarouselPlan parse(String raw) {
        JsonNode root;
        try {
            root = JSON.readTree(extractJson(raw));
        } catch (Exception e) {
            throw new IllegalArgumentException("Model reply is not JSON", e);
        }
        JsonNode slidesNode = root.path("slides");
        if (!slidesNode.isArray()) {
            throw new IllegalArgumentException("Model reply has no slides array");
        }
        List<CarouselSlide> slides = new ArrayList<>();
        for (JsonNode node : slidesNode) {
            CarouselSlide slide = slide(node);
            if (slide != null) {
                slides.add(slide);
            }
            if (slides.size() == MAX_SLIDES) {
                break;
            }
        }
        if (slides.size() < MIN_SLIDES) {
            throw new IllegalArgumentException("Model reply has too few slides: " + slides.size());
        }
        String caption = root.path("caption").asText("").trim();
        if (caption.isBlank()) {
            caption = "Swipe for the breakdown.";
        }
        return new CarouselPlan(slides, caption);
    }

    private static CarouselSlide slide(JsonNode node) {
        if (!node.isObject()) {
            return null;
        }
        String heading = CarouselText.clampWords(node.path("heading").asText(""), MAX_HEADING_WORDS);
        String body = CarouselText.clampWords(node.path("body").asText(""), MAX_BODY_WORDS);
        String code = node.path("code").asText("");
        String language = node.path("language").asText("");
        String filename = node.path("filename").asText("");
        if (heading.isBlank() && body.isBlank() && code.isBlank()) {
            return null;
        }
        CarouselSlide.Kind kind = kind(node.path("kind").asText(""));
        if (!code.isBlank()) {
            kind = CarouselSlide.Kind.CODE;
        } else if (kind == CarouselSlide.Kind.CODE) {
            kind = CarouselSlide.Kind.POINT;
        }
        return new CarouselSlide(kind, heading, body, code, language, filename);
    }

    private static CarouselSlide.Kind kind(String value) {
        return switch (value.trim().toLowerCase()) {
            case "hook", "title", "cover" -> CarouselSlide.Kind.HOOK;
            case "code", "snippet" -> CarouselSlide.Kind.CODE;
            case "cta", "question", "end" -> CarouselSlide.Kind.CTA;
            default -> CarouselSlide.Kind.POINT;
        };
    }

    private static String extractJson(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Model reply is empty");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("No JSON object in model reply");
        }
        return raw.substring(start, end + 1);
    }
}
