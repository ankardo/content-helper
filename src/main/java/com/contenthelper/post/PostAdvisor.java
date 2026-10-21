package com.contenthelper.post;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Reads the shape of the notes and recommends a format. Deterministic on purpose: the advice must be
 * cheap and must not depend on the model being up.
 */
@Service
public class PostAdvisor {

    private static final Pattern COMPARISON = Pattern.compile(
            "\\b(vs\\.?|versus|compared\\s+(to|with)|better\\s+than|alternative\\s+to)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GUIDE = Pattern.compile(
            "\\b(how\\s+to|step\\s+\\d|steps|guide|tutorial|walkthrough|first,|then,|finally)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FRAMEWORK =
            Pattern.compile("\\b(framework|pillars|components|principles)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUILD = Pattern.compile(
            "\\b(architecture|stack|tests?|ci|bugs?|built|shipped|release|repo|commit|pipeline)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FIRST_PERSON = Pattern.compile("\\b(i|my|me|myself)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NARRATIVE = Pattern.compile(
            "\\b(i\\s+felt|i\\s+watched|i\\s+told\\s+myself|i\\s+wanted|i\\s+tried|i\\s+built"
                    + "|i\\s+edited|i\\s+am\\s+posting|the\\s+problem|what\\s+i\\s+tried|the\\s+meta\\s+part)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^\\s*\\d+[.)]\\s+", Pattern.MULTILINE);

    public PostAdvice advise(String source) {
        String text = source == null ? "" : source;
        int story = Math.min(6, count(FIRST_PERSON, text)) + 2 * count(NARRATIVE, text);
        int comparison = count(COMPARISON, text);
        int guide = count(GUIDE, text) + 2 * count(NUMBERED_LINE, text);
        int framework = count(FRAMEWORK, text);
        int build = count(BUILD, text);

        PostType type;
        if (comparison >= 2) {
            type = PostType.COMPARISON;
        } else if (guide >= 3 && guide > story) {
            type = PostType.GUIDE;
        } else if (framework >= 2 && framework > story) {
            type = PostType.FRAMEWORK;
        } else if (build >= 4 && build > story) {
            type = PostType.BUILD_BREAKDOWN;
        } else {
            type = PostType.STORY;
        }

        PostShape shape = type.wantsCarousel() ? PostShape.CAROUSEL : PostShape.TEXT_IMAGE;
        return new PostAdvice(type, shape, reason(type), nextStep(type));
    }

    private static String reason(PostType type) {
        return switch (type) {
            case STORY -> "First person narrative. A carousel would flatten it; a text post carries the story.";
            case GUIDE -> "Step by step. A carousel gives each step its own page.";
            case FRAMEWORK -> "Named parts. A carousel works as a reference deck.";
            case COMPARISON -> "Side by side. A carousel keeps the difference on one page per point.";
            case BUILD_BREAKDOWN -> "Build details. A carousel turns the work into proof.";
        };
    }

    private static String nextStep(PostType type) {
        return type.wantsCarousel()
                ? "Run `carousel` for the deck, then `draft` for the short text hook."
                : "Run `draft` for the text. Attach one real screenshot.";
    }

    private static int count(Pattern pattern, String text) {
        int found = 0;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            found++;
        }
        return found;
    }
}
