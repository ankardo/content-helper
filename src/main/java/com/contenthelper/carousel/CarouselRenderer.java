package com.contenthelper.carousel;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

/** Renders a plan to 1080x1350 PNGs and a PDF with one page per slide. */
@Service
public class CarouselRenderer {

    public static final int WIDTH = 1080;
    public static final int HEIGHT = 1350;

    private static final float SCALE = 2f;
    private static final float PAGE_WIDTH = SlidePainter.WIDTH;
    private static final float PAGE_HEIGHT = SlidePainter.HEIGHT;

    public List<BufferedImage> render(CarouselPlan plan) {
        List<BufferedImage> images = new ArrayList<>();
        List<CarouselSlide> slides = plan.slides();
        for (int i = 0; i < slides.size(); i++) {
            images.add(renderSlide(slides.get(i), i, slides.size()));
        }
        return images;
    }

    public BufferedImage renderSlide(CarouselSlide slide, int index, int total) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.scale(SCALE, SCALE);
            new SlidePainter(g).paint(slide, index, total);
        } finally {
            g.dispose();
        }
        return image;
    }

    public List<Path> write(Path dir, CarouselPlan plan) throws IOException {
        Files.createDirectories(dir);
        List<BufferedImage> images = render(plan);
        List<Path> paths = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            Path file = dir.resolve("slide-%02d.png".formatted(i + 1));
            if (!ImageIO.write(images.get(i), "png", file.toFile())) {
                throw new IOException("No PNG writer available");
            }
            paths.add(file);
        }
        paths.add(writePdf(dir.resolve("carousel.pdf"), images));
        return paths;
    }

    private static Path writePdf(Path file, List<BufferedImage> images) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (BufferedImage image : images) {
                PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
                document.addPage(page);
                PDImageXObject xobject = LosslessFactory.createFromImage(document, image);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.drawImage(xobject, 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
                }
            }
            document.save(file.toFile());
        }
        return file;
    }
}
