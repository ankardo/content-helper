package com.contenthelper.carousel;

/** One slide in a carousel plan. Unused fields are empty, never null. */
public record CarouselSlide(Kind kind, String heading, String body, String code, String language, String filename) {

    public enum Kind {
        HOOK,
        POINT,
        CODE,
        CTA
    }

    public CarouselSlide {
        kind = kind == null ? Kind.POINT : kind;
        heading = heading == null ? "" : heading.trim();
        body = body == null ? "" : body.trim();
        code = code == null ? "" : code.stripTrailing();
        language = language == null ? "" : language.trim().toLowerCase();
        filename = filename == null ? "" : filename.trim();
    }

    public static CarouselSlide hook(String heading, String body) {
        return new CarouselSlide(Kind.HOOK, heading, body, "", "", "");
    }

    public static CarouselSlide point(String heading, String body) {
        return new CarouselSlide(Kind.POINT, heading, body, "", "", "");
    }

    public static CarouselSlide code(String heading, String body, String code, String language, String filename) {
        return new CarouselSlide(Kind.CODE, heading, body, code, language, filename);
    }

    public static CarouselSlide cta(String heading, String body) {
        return new CarouselSlide(Kind.CTA, heading, body, "", "", "");
    }

    public boolean hasCode() {
        return kind == Kind.CODE && !code.isBlank();
    }
}
