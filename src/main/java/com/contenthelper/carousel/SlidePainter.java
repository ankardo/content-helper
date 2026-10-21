package com.contenthelper.carousel;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Draws one slide on a 540x675 design canvas. */
final class SlidePainter {

    static final int WIDTH = 540;
    static final int HEIGHT = 675;

    private static final float MARGIN = 44;
    private static final float CONTENT_WIDTH = WIDTH - 2 * MARGIN;

    private static final Color BG = new Color(0x0B0B0C);
    private static final Color FG = new Color(0xF4F4F0);
    private static final Color MUTED = new Color(0x9C9C97);
    private static final Color FAINT = new Color(0x27272A);
    private static final Color ACCENT = new Color(0xFF7A45);
    private static final Color CARD = new Color(0x161618);
    private static final Color CARD_BAR = new Color(0x232326);
    private static final Color LIGHT_RED = new Color(0xFF5F57);
    private static final Color LIGHT_YELLOW = new Color(0xFEBC2E);
    private static final Color LIGHT_GREEN = new Color(0x28C840);
    private static final Color CODE_PLAIN = new Color(0xD8D8D2);
    private static final Color CODE_KEYWORD = new Color(0xC792EA);
    private static final Color CODE_STRING = new Color(0xA5E075);
    private static final Color CODE_COMMENT = new Color(0x6F6F76);
    private static final Color CODE_NUMBER = new Color(0xF2C97D);
    private static final Color CODE_TYPE = new Color(0x7FD1E8);

    private final Graphics2D g;

    SlidePainter(Graphics2D g) {
        this.g = g;
    }

    void paint(CarouselSlide slide, int index, int total) {
        g.setColor(BG);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        chrome(index, total);
        switch (slide.kind()) {
            case HOOK -> hook(slide);
            case CODE -> code(slide);
            case CTA -> cta(slide);
            default -> point(slide, index);
        }
        footer();
    }

    private void chrome(int index, int total) {
        g.setColor(ACCENT);
        g.fillRect((int) MARGIN, 38, 10, 10);
        g.setFont(mono(Font.PLAIN, 10));
        g.setColor(MUTED);
        g.drawString("CAROUSEL", MARGIN + 18, 47);
        String page = "%02d / %02d".formatted(index + 1, total);
        g.setColor(MUTED);
        g.drawString(page, WIDTH - MARGIN - g.getFontMetrics().stringWidth(page), 47);
        g.setColor(FAINT);
        g.fillRect((int) MARGIN, 60, (int) CONTENT_WIDTH, 1);
    }

    private void footer() {
        g.setFont(mono(Font.PLAIN, 9));
        g.setColor(MUTED);
        g.drawString("content-helper", MARGIN, HEIGHT - 28);
        String tag = "build in public";
        g.drawString(tag, WIDTH - MARGIN - g.getFontMetrics().stringWidth(tag), HEIGHT - 28);
    }

    private void hook(CarouselSlide slide) {
        g.setColor(ACCENT);
        g.fillRect((int) MARGIN, 142, 54, 4);
        Font font = sans(Font.BOLD, 40);
        List<String> lines = SlideText.wrap(slide.heading(), g.getFontMetrics(font), CONTENT_WIDTH);
        float lineHeight = 50;
        float top = 180;
        g.setFont(font);
        g.setColor(FG);
        drawLines(lines, MARGIN, top + font.getSize(), lineHeight);
        float bodyTop = top + lines.size() * lineHeight + 24;
        if (!slide.body().isBlank()) {
            paragraph(slide.body(), MARGIN, bodyTop, CONTENT_WIDTH, 150, 19, 15, MUTED);
        }
    }

    private void point(CarouselSlide slide, int index) {
        g.setFont(mono(Font.BOLD, 12));
        g.setColor(ACCENT);
        g.drawString("%02d".formatted(index + 1), MARGIN, 138);
        Font heading = sans(Font.BOLD, 27);
        List<String> lines = SlideText.wrap(slide.heading(), g.getFontMetrics(heading), CONTENT_WIDTH);
        float lineHeight = 34;
        float top = 158;
        g.setFont(heading);
        g.setColor(FG);
        drawLines(lines, MARGIN, top + heading.getSize(), lineHeight);
        float bodyTop = top + lines.size() * lineHeight + 28;
        paragraph(slide.body(), MARGIN, bodyTop, CONTENT_WIDTH, HEIGHT - 110 - bodyTop, 20, 14, MUTED);
    }

    private void code(CarouselSlide slide) {
        float top = 130;
        if (!slide.heading().isBlank()) {
            Font heading = sans(Font.BOLD, 22);
            g.setFont(heading);
            g.setColor(FG);
            g.drawString(SlideText.fit(slide.heading(), g.getFontMetrics(), CONTENT_WIDTH), MARGIN, 146);
            top = 176;
        }
        if (!slide.body().isBlank()) {
            top = paragraph(slide.body(), MARGIN, top, CONTENT_WIDTH, 54, 14, 12, MUTED) + 12;
        }
        float cardTop = Math.max(top, 190);
        float cardWidth = CONTENT_WIDTH;
        float maxCardHeight = HEIGHT - 84 - cardTop;

        List<List<CodeTokenizer.Token>> lines = CodeTokenizer.lines(slide.code(), slide.language());
        int fontSize = 12;
        Font codeFont = mono(Font.PLAIN, fontSize);
        FontMetrics metrics = g.getFontMetrics(codeFont);
        float widest = 0;
        for (List<CodeTokenizer.Token> line : lines) {
            widest = Math.max(widest, lineWidth(line, metrics));
        }
        float codeWidth = cardWidth - 36;
        if (widest > codeWidth) {
            fontSize = Math.max(8, (int) Math.floor(fontSize * codeWidth / widest));
            codeFont = mono(Font.PLAIN, fontSize);
            metrics = g.getFontMetrics(codeFont);
        }
        float lineHeight = fontSize * 1.45f;
        int visible = Math.max(1, (int) ((maxCardHeight - 62) / lineHeight));
        if (lines.size() > visible) {
            lines = new ArrayList<>(lines.subList(0, visible));
        }
        float desired = 30 + 18 + lines.size() * lineHeight + 16;
        float cardHeight = Math.min(maxCardHeight, Math.max(150, desired));

        drawCard(cardTop, cardWidth, cardHeight, slide.filename());

        float x = MARGIN + 18;
        float right = MARGIN + cardWidth - 18;
        float y = cardTop + 30 + fontSize + 8;
        g.setFont(codeFont);
        for (List<CodeTokenizer.Token> line : lines) {
            drawCodeLine(line, x, right, y, metrics);
            y += lineHeight;
        }
    }

    private void drawCard(float cardTop, float cardWidth, float cardHeight, String filename) {
        RoundRectangle2D card = new RoundRectangle2D.Float(MARGIN, cardTop, cardWidth, cardHeight, 16, 16);
        g.setColor(CARD);
        g.fill(card);
        g.setColor(CARD_BAR);
        g.fill(new RoundRectangle2D.Float(MARGIN, cardTop, cardWidth, 30, 16, 16));
        g.fillRect((int) MARGIN, (int) cardTop + 15, (int) cardWidth, 15);

        g.setColor(LIGHT_RED);
        g.fillOval((int) MARGIN + 14, (int) cardTop + 11, 8, 8);
        g.setColor(LIGHT_YELLOW);
        g.fillOval((int) MARGIN + 28, (int) cardTop + 11, 8, 8);
        g.setColor(LIGHT_GREEN);
        g.fillOval((int) MARGIN + 42, (int) cardTop + 11, 8, 8);

        String name = filename.isBlank() ? "snippet" : filename;
        g.setFont(mono(Font.PLAIN, 10));
        g.setColor(MUTED);
        String fitted = SlideText.fit(name, g.getFontMetrics(), cardWidth - 130);
        float nameX = MARGIN + (cardWidth - g.getFontMetrics().stringWidth(fitted)) / 2;
        g.drawString(fitted, nameX, cardTop + 20);
    }

    private void drawCodeLine(List<CodeTokenizer.Token> line, float x, float right, float y, FontMetrics metrics) {
        float cursor = x;
        for (CodeTokenizer.Token token : line) {
            String text = expandTabs(token.text());
            String draw = text;
            if (cursor + metrics.stringWidth(draw) > right) {
                draw = SlideText.fit(draw, metrics, right - cursor);
            }
            if (draw.isEmpty()) {
                break;
            }
            g.setColor(codeColor(token.type()));
            g.drawString(draw, cursor, y);
            cursor += metrics.stringWidth(draw);
            if (cursor >= right) {
                break;
            }
        }
    }

    private static float lineWidth(List<CodeTokenizer.Token> line, FontMetrics metrics) {
        float width = 0;
        for (CodeTokenizer.Token token : line) {
            width += metrics.stringWidth(expandTabs(token.text()));
        }
        return width;
    }

    private static String expandTabs(String text) {
        return text.replace("\t", "    ");
    }

    private static Color codeColor(CodeTokenizer.Type type) {
        return switch (type) {
            case KEYWORD -> CODE_KEYWORD;
            case STRING -> CODE_STRING;
            case COMMENT -> CODE_COMMENT;
            case NUMBER, ANNOTATION -> CODE_NUMBER;
            case TYPE, VARIABLE -> CODE_TYPE;
            default -> CODE_PLAIN;
        };
    }

    private void cta(CarouselSlide slide) {
        g.setColor(ACCENT);
        g.fillRect((int) MARGIN, 190, 54, 4);
        Font heading = sans(Font.BOLD, 32);
        List<String> lines = SlideText.wrap(slide.heading(), g.getFontMetrics(heading), CONTENT_WIDTH);
        float lineHeight = 42;
        float top = 230;
        g.setFont(heading);
        g.setColor(FG);
        drawLines(lines, MARGIN, top, lineHeight);
        float bodyTop = top + lines.size() * lineHeight + 16;
        paragraph(slide.body(), MARGIN, bodyTop, CONTENT_WIDTH, 220, 20, 15, MUTED);
    }

    private float paragraph(
            String text, float x, float top, float maxWidth, float maxHeight, int startSize, int minSize, Color color) {
        int size = startSize;
        List<String> lines = List.of();
        float lineHeight = 0;
        for (; size >= minSize; size -= 2) {
            Font font = sans(Font.PLAIN, size);
            g.setFont(font);
            lines = SlideText.wrap(text, g.getFontMetrics(font), maxWidth);
            lineHeight = size * 1.4f;
            if (lines.size() * lineHeight <= maxHeight) {
                break;
            }
        }
        size = Math.max(size, minSize);
        Font font = sans(Font.PLAIN, size);
        g.setFont(font);
        lines = SlideText.wrap(text, g.getFontMetrics(font), maxWidth);
        lineHeight = size * 1.4f;
        int fit = Math.max(1, (int) (maxHeight / lineHeight));
        if (lines.size() > fit) {
            lines = new ArrayList<>(lines.subList(0, fit));
            int last = lines.size() - 1;
            lines.set(last, SlideText.fit(lines.get(last) + "...", g.getFontMetrics(font), maxWidth));
        }
        g.setColor(color);
        drawLines(lines, x, top + size, lineHeight);
        return top + lines.size() * lineHeight;
    }

    private void drawLines(List<String> lines, float x, float baseline, float lineHeight) {
        float y = baseline;
        for (String line : lines) {
            g.drawString(line, x, y);
            y += lineHeight;
        }
    }

    private static Font sans(int style, int size) {
        return new Font(Font.SANS_SERIF, style, size);
    }

    private static Font mono(int style, int size) {
        return new Font(Font.MONOSPACED, style, size);
    }
}
