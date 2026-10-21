package com.contenthelper.source;

import static org.assertj.core.api.Assertions.assertThat;

import com.contenthelper.git.GitLogReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NoteSourceReaderTest {

    private final GitLogReader git = new GitLogReader();

    @Test
    void notesFileKeepsSentencesWhole(@TempDir Path base) throws Exception {
        Path notes = base.resolve("notes.txt");
        Files.writeString(notes, "alpha bravo charlie");
        NoteSource source = NoteSourceReader.read(null, notes.toString(), null, ".", git);
        assertThat(source.text()).isEqualTo("alpha bravo charlie");
        assertThat(source.error()).isNull();
    }

    @Test
    void splitsAFileHeader(@TempDir Path base) throws Exception {
        Path notes = base.resolve("notes.txt");
        Files.writeString(notes, "---\ntitle: A Real Title\ndescription: \"A blurb.\"\n---\nThe body.");
        NoteSource source = NoteSourceReader.read(null, notes.toString(), null, ".", git);
        assertThat(source.text()).isEqualTo("The body.");
        assertThat(source.meta()).containsEntry("title", "A Real Title").containsEntry("description", "A blurb.");
    }

    @Test
    void missingFileIsAnError(@TempDir Path base) throws Exception {
        NoteSource source = NoteSourceReader.read(null, base.resolve("nope.txt").toString(), null, ".", git);
        assertThat(source.error()).startsWith("Cannot read notes file");
    }

    @Test
    void missingInputHasNoTextAndNoError() throws Exception {
        NoteSource source = NoteSourceReader.read(null, null, null, ".", git);
        assertThat(source.text()).isNull();
        assertThat(source.error()).isNull();
    }

    @Test
    void firstLineStripsMarkdownHeading() {
        assertThat(NoteSourceReader.firstLine("\n# A Title\nbody")).isEqualTo("A Title");
    }
}
