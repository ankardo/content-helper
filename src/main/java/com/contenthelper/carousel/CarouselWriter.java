package com.contenthelper.carousel;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Writes the plan artifacts. Files only, never publishes anywhere. */
@Service
public class CarouselWriter {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final CarouselRenderer renderer;

    public CarouselWriter(CarouselRenderer renderer) {
        this.renderer = renderer;
    }

    public List<Path> writeAll(Path base, String slug, CarouselPlan plan) throws IOException {
        Path dir = base.resolve(slug);
        Files.createDirectories(dir);
        List<Path> paths = new ArrayList<>(writePlan(base, slug, plan));
        paths.addAll(renderer.write(dir, plan));
        return paths;
    }

    public List<Path> writePlan(Path base, String slug, CarouselPlan plan) throws IOException {
        Path dir = base.resolve(slug);
        Files.createDirectories(dir);
        Path planFile = Files.writeString(
                dir.resolve("plan.json"), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(plan));
        Path caption = Files.writeString(dir.resolve("caption.md"), plan.caption() + "\n");
        return List.of(planFile, caption);
    }
}
