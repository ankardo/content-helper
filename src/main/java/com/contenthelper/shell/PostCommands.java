package com.contenthelper.shell;

import com.contenthelper.git.GitLogReader;
import com.contenthelper.post.PostAdvice;
import com.contenthelper.post.PostAdvisor;
import com.contenthelper.post.PostSchedule;
import com.contenthelper.source.NoteSource;
import com.contenthelper.source.NoteSourceReader;
import java.time.Clock;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
public class PostCommands {

    private final PostAdvisor advisor;
    private final PostSchedule schedule;
    private final GitLogReader git;

    public PostCommands(PostAdvisor advisor, PostSchedule schedule, GitLogReader git) {
        this.advisor = advisor;
        this.schedule = schedule;
        this.git = git;
    }

    @Command(
            name = {"post"},
            description = "Recommend the best post shape and the next publish window.")
    public String post(
            @Option(longName = "notes", defaultValue = "") String notes,
            @Option(longName = "notes-file", defaultValue = "") String notesFile,
            @Option(longName = "range", defaultValue = "") String range,
            @Option(longName = "dir", defaultValue = ".") String dir)
            throws Exception {
        NoteSource input = NoteSourceReader.read(notes, notesFile, range, dir, git);
        if (input.error() != null) {
            return input.error();
        }
        if (input.text() == null) {
            return NoteSourceReader.USAGE;
        }
        PostAdvice advice = advisor.advise(input.text());
        String window = schedule.nextWindow(Clock.systemUTC());
        return String.join(
                System.lineSeparator(),
                "Best shape: " + advice.shape().label(),
                "Type: " + advice.type().label(),
                "Why: " + advice.reason(),
                "Next: " + advice.nextStep(),
                "Window: " + window);
    }
}
