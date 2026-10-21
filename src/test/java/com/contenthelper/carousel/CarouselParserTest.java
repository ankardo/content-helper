package com.contenthelper.carousel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CarouselParserTest {

    private final CarouselParser parser = new CarouselParser();

    private static String slide(String kind, String heading, String body) {
        return "{\"kind\":\"" + kind + "\",\"heading\":\"" + heading + "\",\"body\":\"" + body + "\"}";
    }

    private static String plan(int count) {
        StringBuilder json = new StringBuilder("{\"slides\":[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(slide("point", "Heading " + i, "Body " + i));
        }
        return json.append("],\"caption\":\"A caption\"}").toString();
    }

    @Test
    void parsesSlidesAndCaption() {
        CarouselPlan plan = parser.parse(plan(8));
        assertThat(plan.slides()).hasSize(8);
        assertThat(plan.slides().get(0).kind()).isEqualTo(CarouselSlide.Kind.POINT);
        assertThat(plan.slides().get(0).heading()).isEqualTo("Heading 0");
        assertThat(plan.caption()).isEqualTo("A caption");
    }

    @Test
    void toleratesFencesAndProse() {
        CarouselPlan plan = parser.parse("Here you go:\n```json\n" + plan(8) + "\n```");
        assertThat(plan.slides()).hasSize(8);
    }

    @Test
    void rejectsNonJson() {
        assertThatThrownBy(() -> parser.parse("not json")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTooFewSlides() {
        assertThatThrownBy(() -> parser.parse(plan(3))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clampsLongHeadingAndBody() {
        String longText = "one two three four five six seven eight nine ten eleven twelve";
        String longBody = "a b c d e f g h i j k l m n o p q r s t u v w x y z "
                + "aa bb cc dd ee ff gg hh ii jj kk ll mm nn oo pp qq rr ss";
        String json = "{\"slides\":["
                + slide("point", longText, longBody) + ","
                + slide("point", "H1", "B1") + ","
                + slide("point", "H2", "B2") + ","
                + slide("point", "H3", "B3")
                + "],\"caption\":\"x\"}";
        CarouselPlan plan = parser.parse(json);
        assertThat(plan.slides().get(0).heading().split(" ")).hasSize(10);
        assertThat(plan.slides().get(0).body().split(" ")).hasSize(40);
    }

    @Test
    void codeFieldMakesACodeSlide() {
        String json = "{\"slides\":["
                + "{\"kind\":\"point\",\"heading\":\"Show the code\",\"body\":\"Small\","
                + "\"code\":\"int x = 1;\",\"language\":\"java\",\"filename\":\"A.java\"},"
                + slide("point", "H1", "B1") + ","
                + slide("point", "H2", "B2") + ","
                + slide("point", "H3", "B3")
                + "],\"caption\":\"x\"}";
        CarouselPlan plan = parser.parse(json);
        assertThat(plan.slides().get(0).kind()).isEqualTo(CarouselSlide.Kind.CODE);
        assertThat(plan.slides().get(0).code()).isEqualTo("int x = 1;");
        assertThat(plan.slides().get(0).language()).isEqualTo("java");
    }

    @Test
    void blankCaptionGetsADefault() {
        String json = "{\"slides\":["
                + slide("point", "H0", "B0") + ","
                + slide("point", "H1", "B1") + ","
                + slide("point", "H2", "B2") + ","
                + slide("point", "H3", "B3")
                + "]}";
        assertThat(parser.parse(json).caption()).isNotBlank();
    }
}
