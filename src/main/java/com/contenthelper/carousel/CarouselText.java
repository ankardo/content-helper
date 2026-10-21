package com.contenthelper.carousel;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Small text helpers shared by the plan parser and the canned plan. */
final class CarouselText {

    private CarouselText() {}

    static String clampWords(String value, int maxWords) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return "";
        }
        String[] words = trimmed.split(" ");
        if (words.length <= maxWords) {
            return trimmed;
        }
        return Arrays.stream(words).limit(maxWords).collect(Collectors.joining(" "));
    }
}
