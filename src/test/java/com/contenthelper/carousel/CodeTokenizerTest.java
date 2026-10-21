package com.contenthelper.carousel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CodeTokenizerTest {

    private static CodeTokenizer.Type typeOf(List<CodeTokenizer.Token> tokens, String text) {
        return tokens.stream()
                .filter(token -> token.text().equals(text))
                .map(CodeTokenizer.Token::type)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No token " + text));
    }

    @Test
    void tokenizesJava() {
        List<CodeTokenizer.Token> tokens =
                CodeTokenizer.tokenize("public class Foo { String s = \"hi\"; // note\n int n = 42; }", "java");
        assertThat(typeOf(tokens, "public")).isEqualTo(CodeTokenizer.Type.KEYWORD);
        assertThat(typeOf(tokens, "class")).isEqualTo(CodeTokenizer.Type.KEYWORD);
        assertThat(typeOf(tokens, "Foo")).isEqualTo(CodeTokenizer.Type.TYPE);
        assertThat(typeOf(tokens, "\"hi\"")).isEqualTo(CodeTokenizer.Type.STRING);
        assertThat(typeOf(tokens, "// note")).isEqualTo(CodeTokenizer.Type.COMMENT);
        assertThat(typeOf(tokens, "42")).isEqualTo(CodeTokenizer.Type.NUMBER);
    }

    @Test
    void tokenizesAnnotations() {
        List<CodeTokenizer.Token> tokens = CodeTokenizer.tokenize("@Override public void run() {}", "java");
        assertThat(typeOf(tokens, "@Override")).isEqualTo(CodeTokenizer.Type.ANNOTATION);
    }

    @Test
    void tokenizesTypeScript() {
        List<CodeTokenizer.Token> tokens = CodeTokenizer.tokenize("const n: number = 1; // count", "typescript");
        assertThat(typeOf(tokens, "const")).isEqualTo(CodeTokenizer.Type.KEYWORD);
        assertThat(typeOf(tokens, "number")).isEqualTo(CodeTokenizer.Type.TYPE);
        assertThat(typeOf(tokens, "1")).isEqualTo(CodeTokenizer.Type.NUMBER);
        assertThat(typeOf(tokens, "// count")).isEqualTo(CodeTokenizer.Type.COMMENT);
    }

    @Test
    void tokenizesBash() {
        List<CodeTokenizer.Token> tokens =
                CodeTokenizer.tokenize("if [ -f \"$file\" ]; then # check\n echo 1\nfi", "bash");
        assertThat(typeOf(tokens, "if")).isEqualTo(CodeTokenizer.Type.KEYWORD);
        assertThat(typeOf(tokens, "\"$file\"")).isEqualTo(CodeTokenizer.Type.STRING);
        assertThat(typeOf(tokens, "# check")).isEqualTo(CodeTokenizer.Type.COMMENT);
        assertThat(typeOf(tokens, "1")).isEqualTo(CodeTokenizer.Type.NUMBER);
        assertThat(typeOf(tokens, "fi")).isEqualTo(CodeTokenizer.Type.KEYWORD);
    }

    @Test
    void preservesEveryCharacter() {
        List<String> samples = List.of(
                "public class Foo { String s = \"hi\"; // note\n int n = 42; }",
                "const n: number = 1;\nconst s = `hi ${n}`;",
                "if [ -f \"$file\" ]; then\n  echo \"ok\" # done\nfi");
        List<String> languages = List.of("java", "typescript", "bash");
        for (int i = 0; i < samples.size(); i++) {
            String joined = CodeTokenizer.tokenize(samples.get(i), languages.get(i)).stream()
                    .map(CodeTokenizer.Token::text)
                    .reduce("", String::concat);
            assertThat(joined).isEqualTo(samples.get(i));
        }
    }

    @Test
    void unknownLanguageIsOnePlainToken() {
        List<CodeTokenizer.Token> tokens = CodeTokenizer.tokenize("select 1;", "sql");
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).type()).isEqualTo(CodeTokenizer.Type.PLAIN);
    }

    @Test
    void linesSplitOnNewlines() {
        List<List<CodeTokenizer.Token>> lines = CodeTokenizer.lines("int a = 1;\nint b = 2;", "java");
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).stream().map(CodeTokenizer.Token::text).reduce("", String::concat))
                .isEqualTo("int a = 1;");
        assertThat(lines.get(1).stream().map(CodeTokenizer.Token::text).reduce("", String::concat))
                .isEqualTo("int b = 2;");
    }
}
