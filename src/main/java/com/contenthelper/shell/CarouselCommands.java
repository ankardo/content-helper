package com.contenthelper.shell;

import com.contenthelper.carousel.CarouselGenerator;
import com.contenthelper.carousel.CarouselPlan;
import com.contenthelper.carousel.CarouselTemplates;
import com.contenthelper.carousel.CarouselWriter;
import com.contenthelper.git.GitLogReader;
import com.contenthelper.observe.RunMetrics;
import com.contenthelper.observe.RunRecorder;
import com.contenthelper.source.NoteSource;
import com.contenthelper.source.NoteSourceReader;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
public class CarouselCommands {

    private static final Logger log = LoggerFactory.getLogger(CarouselCommands.class);

    private final CarouselTemplates templates;
    private final CarouselGenerator generator;
    private final CarouselWriter writer;
    private final GitLogReader git;
    private final RunMetrics metrics;

    public CarouselCommands(
            CarouselTemplates templates,
            CarouselGenerator generator,
            CarouselWriter writer,
            GitLogReader git,
            RunMetrics metrics) {
        this.templates = templates;
        this.generator = generator;
        this.writer = writer;
        this.git = git;
        this.metrics = metrics;
    }

    @Command(
            name = {"carousel"},
            description = "Plan a LinkedIn carousel from notes or a commit range.")
    public String carousel(
            @Option(longName = "notes", defaultValue = "") String notes,
            @Option(longName = "notes-file", defaultValue = "") String notesFile,
            @Option(longName = "range", defaultValue = "") String range,
            @Option(longName = "dir", defaultValue = ".") String dir,
            @Option(longName = "out", defaultValue = "carousels") String out,
            @Option(longName = "ai", defaultValue = "true") boolean ai)
            throws Exception {
        long start = System.nanoTime();
        NoteSource input = NoteSourceReader.read(notes, notesFile, range, dir, git);
        if (input.error() != null) {
            return input.error();
        }
        if (input.text() == null) {
            return NoteSourceReader.USAGE;
        }
        String source = input.text();
        String slug = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        CarouselPlan plan;
        String note = "";
        if (ai) {
            try {
                plan = generator.plan(source);
            } catch (Exception e) {
                log.warn("Carousel plan failed, falling back to canned", e);
                note = " (model unreachable, canned plan)";
                plan = templates.plan(source);
            }
        } else {
            plan = templates.plan(source);
        }
        List<Path> written = writer.writeAll(Path.of(out), slug, plan);
        boolean fallback = !note.isBlank();
        RunRecorder.record(metrics, "carousel", ai && !fallback, written.size(), start, fallback);
        return "Wrote a " + plan.slides().size() + " slide carousel to " + out + "/" + slug + " (8 PNGs, carousel.pdf)"
                + note;
    }
}
