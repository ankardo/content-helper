package com.contenthelper.carousel;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SlideTextTest {

    private final BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    private final Graphics2D g = image.createGraphics();

    @AfterEach
    void dispose() {
        g.dispose();
    }

    private FontMetrics metrics(int size) {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, size);
        g.setFont(font);
        return g.getFontMetrics();
    }

    @Test
    void wrapsWithinWidth() {
        FontMetrics metrics = metrics(27);
        List<String> lines = SlideText.wrap("one two three four five six seven eight nine ten", metrics, 200);
        assertThat(lines).hasSizeGreaterThan(1);
        assertThat(lines)
                .allSatisfy(line -> assertThat(metrics.stringWidth(line)).isLessThanOrEqualTo(200));
    }

    @Test
    void breaksAWordWiderThanTheLine() {
        FontMetrics metrics = metrics(27);
        List<String> lines = SlideText.wrap("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz", metrics, 100);
        assertThat(lines).hasSizeGreaterThan(1);
        assertThat(lines)
                .allSatisfy(line -> assertThat(metrics.stringWidth(line)).isLessThanOrEqualTo(100));
    }

    @Test
    void fitShortensWithEllipsis() {
        FontMetrics metrics = metrics(27);
        String fitted = SlideText.fit("a very long line that will not fit", metrics, 120);
        assertThat(fitted).endsWith("...");
        assertThat(metrics.stringWidth(fitted)).isLessThanOrEqualTo(120);
    }

    @Test
    void fitLeavesShortTextAlone() {
        FontMetrics metrics = metrics(27);
        assertThat(SlideText.fit("short", metrics, 300)).isEqualTo("short");
    }
}
