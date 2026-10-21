package com.contenthelper.carousel;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CarouselRendererTest {

    private final CarouselRenderer renderer = new CarouselRenderer();
    private final CarouselPlan plan =
            new CarouselTemplates().plan("Line one\nLine two\nLine three\nLine four\nLine five");

    @Test
    void rendersEightImagesAtPrintSize() {
        List<BufferedImage> images = renderer.render(plan);
        assertThat(images).hasSize(8);
        assertThat(images.get(0).getWidth()).isEqualTo(1080);
        assertThat(images.get(0).getHeight()).isEqualTo(1350);
    }

    @Test
    void writesPngsAndPdf(@TempDir Path dir) throws Exception {
        List<Path> paths = renderer.write(dir, plan);
        assertThat(paths).hasSize(9);
        assertThat(dir.resolve("slide-01.png")).exists();
        assertThat(dir.resolve("slide-08.png")).exists();
        assertThat(dir.resolve("carousel.pdf")).exists();

        try (PDDocument document = Loader.loadPDF(dir.resolve("carousel.pdf").toFile())) {
            assertThat(document.getNumberOfPages()).isEqualTo(8);
            assertThat(document.getPage(0).getMediaBox().getWidth()).isEqualTo(540f);
            assertThat(document.getPage(0).getMediaBox().getHeight()).isEqualTo(675f);
        }

        BufferedImage first = ImageIO.read(dir.resolve("slide-01.png").toFile());
        assertThat(first.getWidth()).isEqualTo(1080);
        assertThat(first.getHeight()).isEqualTo(1350);
    }

    @Test
    void rendersACodeSlide() {
        CarouselPlan codePlan = new CarouselPlan(
                List.of(
                        CarouselSlide.hook("Hook", ""),
                        CarouselSlide.point("Point", "Body"),
                        CarouselSlide.code(
                                "The loop", "", "for (int i = 0; i < 3; i++) {\n  run(i);\n}", "java", "Runner.java"),
                        CarouselSlide.cta("Done", "Bye")),
                "caption");
        BufferedImage image = renderer.renderSlide(codePlan.slides().get(2), 2, 4);
        assertThat(image.getWidth()).isEqualTo(1080);
        assertThat(image.getHeight()).isEqualTo(1350);
    }
}
