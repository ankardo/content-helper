package com.contenthelper.carousel;

import com.contenthelper.ai.ModelGuard;
import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** Asks the model for a carousel plan and parses it into slides. */
@Service
public class CarouselGenerator {

    private static final String ROLE =
            "You are a ghostwriter turning a developer's work notes into a LinkedIn carousel.";

    private static final String SCHEMA =
            """
            {"slides":[{"kind":"hook|point|code|cta","heading":"...","body":"...",\
            "code":"","language":"","filename":""}],"caption":"..."}
            """;

    private final ChatClient chat;
    private final ModelGuard guard;
    private final CarouselParser parser;

    public CarouselGenerator(ChatClient.Builder builder, ModelGuard guard, CarouselParser parser) {
        this.chat = builder.build();
        this.guard = guard;
        this.parser = parser;
    }

    public CarouselPlan plan(String source) throws Exception {
        return plan(source, null, 0);
    }

    public CarouselPlan plan(String source, Duration timeout, int attempts) throws Exception {
        String reply = guard.call(
                () -> chat.prompt().system(ROLE).user(prompt(source)).call().content(), timeout, attempts);
        if (reply == null || reply.isBlank()) {
            throw new IllegalStateException("Empty reply from model");
        }
        return parser.parse(reply);
    }

    private static String prompt(String source) {
        return "Work notes:\n" + source + "\n\n"
                + "Return JSON only, with no markdown fences, in this shape:\n" + SCHEMA + "\n"
                + "Rules: exactly 8 slides. The first slide is kind hook, the last is kind cta. "
                + "At most one code slide, only when the notes contain code; put the code in the code "
                + "field and the language and filename beside it. "
                + "A heading is at most 8 words, a body at most 25 words. First person. "
                + "Keep the author's facts and invent none. "
                + "The caption is a short LinkedIn caption with a swipe call to action and 3 hashtags.";
    }
}
