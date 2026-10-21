package com.contenthelper.carousel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** A small scanner for the languages that show up in the notes. */
final class CodeTokenizer {

    enum Type {
        PLAIN,
        KEYWORD,
        STRING,
        COMMENT,
        NUMBER,
        TYPE,
        ANNOTATION,
        VARIABLE
    }

    record Token(String text, Type type) {}

    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "permits",
            "private",
            "protected",
            "public",
            "record",
            "return",
            "sealed",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "var",
            "void",
            "volatile",
            "while",
            "yield",
            "true",
            "false",
            "null");

    private static final Set<String> TS_KEYWORDS = Set.of(
            "as",
            "async",
            "await",
            "break",
            "case",
            "catch",
            "class",
            "const",
            "continue",
            "debugger",
            "default",
            "delete",
            "do",
            "else",
            "enum",
            "export",
            "extends",
            "finally",
            "for",
            "from",
            "function",
            "if",
            "implements",
            "import",
            "in",
            "instanceof",
            "interface",
            "let",
            "namespace",
            "new",
            "of",
            "private",
            "protected",
            "public",
            "readonly",
            "return",
            "static",
            "super",
            "switch",
            "this",
            "throw",
            "try",
            "type",
            "typeof",
            "var",
            "while",
            "yield",
            "true",
            "false",
            "null",
            "undefined");

    private static final Set<String> TS_TYPES =
            Set.of("any", "bigint", "boolean", "never", "number", "object", "string", "symbol", "unknown", "void");

    private static final Set<String> BASH_KEYWORDS = Set.of(
            "if",
            "then",
            "else",
            "elif",
            "fi",
            "for",
            "in",
            "do",
            "done",
            "while",
            "until",
            "case",
            "esac",
            "function",
            "return",
            "export",
            "local",
            "readonly",
            "declare",
            "source",
            "set",
            "unset",
            "exit",
            "shift",
            "trap",
            "break",
            "continue",
            "echo",
            "printf",
            "cd",
            "read");

    private CodeTokenizer() {}

    static List<Token> tokenize(String code, String language) {
        String lang = language == null ? "" : language.trim().toLowerCase();
        boolean java = lang.equals("java");
        boolean typescript =
                lang.equals("typescript") || lang.equals("ts") || lang.equals("javascript") || lang.equals("js");
        boolean bash = lang.equals("bash") || lang.equals("sh") || lang.equals("shell");
        if (!java && !typescript && !bash) {
            return List.of(new Token(code, Type.PLAIN));
        }
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = code.length();
        while (i < n) {
            char c = code.charAt(i);
            if (c == '\n') {
                tokens.add(new Token("\n", Type.PLAIN));
                i++;
            } else if (Character.isWhitespace(c)) {
                int start = i;
                while (i < n && code.charAt(i) != '\n' && Character.isWhitespace(code.charAt(i))) {
                    i++;
                }
                tokens.add(new Token(code.substring(start, i), Type.PLAIN));
            } else if ((java || typescript) && c == '/' && i + 1 < n && code.charAt(i + 1) == '/') {
                int end = code.indexOf('\n', i);
                end = end < 0 ? n : end;
                tokens.add(new Token(code.substring(i, end), Type.COMMENT));
                i = end;
            } else if ((java || typescript) && c == '/' && i + 1 < n && code.charAt(i + 1) == '*') {
                int end = code.indexOf("*/", i + 2);
                end = end < 0 ? n : end + 2;
                tokens.add(new Token(code.substring(i, end), Type.COMMENT));
                i = end;
            } else if (bash && c == '#') {
                int end = code.indexOf('\n', i);
                end = end < 0 ? n : end;
                tokens.add(new Token(code.substring(i, end), Type.COMMENT));
                i = end;
            } else if (c == '"' || c == '\'' || (typescript && c == '`')) {
                int end = stringEnd(code, i, c);
                tokens.add(new Token(code.substring(i, end), Type.STRING));
                i = end;
            } else if (Character.isDigit(c)) {
                int end = numberEnd(code, i);
                tokens.add(new Token(code.substring(i, end), Type.NUMBER));
                i = end;
            } else if (bash && c == '$') {
                int end = variableEnd(code, i);
                tokens.add(new Token(code.substring(i, end), Type.VARIABLE));
                i = end;
            } else if (java && c == '@') {
                int end = identifierEnd(code, i + 1);
                if (end > i + 1) {
                    tokens.add(new Token(code.substring(i, end), Type.ANNOTATION));
                    i = end;
                } else {
                    tokens.add(new Token("@", Type.PLAIN));
                    i++;
                }
            } else if (isIdentifierStart(c)) {
                int end = identifierEnd(code, i);
                String word = code.substring(i, end);
                tokens.add(new Token(word, classify(word, java, typescript, bash)));
                i = end;
            } else {
                tokens.add(new Token(String.valueOf(c), Type.PLAIN));
                i++;
            }
        }
        return tokens;
    }

    static List<List<Token>> lines(String code, String language) {
        List<List<Token>> lines = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        for (Token token : tokenize(code, language)) {
            String text = token.text();
            int start = 0;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    if (i > start) {
                        current.add(new Token(text.substring(start, i), token.type()));
                    }
                    lines.add(current);
                    current = new ArrayList<>();
                    start = i + 1;
                }
            }
            if (start < text.length()) {
                current.add(new Token(text.substring(start), token.type()));
            }
        }
        lines.add(current);
        return lines;
    }

    private static Type classify(String word, boolean java, boolean typescript, boolean bash) {
        if (java && JAVA_KEYWORDS.contains(word)) {
            return Type.KEYWORD;
        }
        if (typescript && TS_KEYWORDS.contains(word)) {
            return Type.KEYWORD;
        }
        if (bash && BASH_KEYWORDS.contains(word)) {
            return Type.KEYWORD;
        }
        if (typescript && TS_TYPES.contains(word)) {
            return Type.TYPE;
        }
        if ((java || typescript) && !word.isEmpty() && Character.isUpperCase(word.charAt(0))) {
            return Type.TYPE;
        }
        return Type.PLAIN;
    }

    private static int stringEnd(String code, int start, char quote) {
        if (quote == '"' && code.startsWith("\"\"\"", start)) {
            int end = code.indexOf("\"\"\"", start + 3);
            return end < 0 ? code.length() : end + 3;
        }
        int i = start + 1;
        while (i < code.length()) {
            char c = code.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == quote) {
                return i + 1;
            } else if (c == '\n' && quote != '`') {
                return i;
            } else {
                i++;
            }
        }
        return code.length();
    }

    private static int numberEnd(String code, int start) {
        int i = start;
        if (i + 1 < code.length() && code.charAt(i) == '0' && "xXbBoO".indexOf(code.charAt(i + 1)) >= 0) {
            i += 2;
            while (i < code.length() && (Character.isLetterOrDigit(code.charAt(i)) || code.charAt(i) == '_')) {
                i++;
            }
            return i;
        }
        while (i < code.length()) {
            char c = code.charAt(i);
            if (Character.isDigit(c) || c == '_' || c == '.') {
                i++;
            } else if ("lLfFdDnN".indexOf(c) >= 0) {
                i++;
                break;
            } else {
                break;
            }
        }
        return i;
    }

    private static int variableEnd(String code, int start) {
        int i = start + 1;
        if (i < code.length() && code.charAt(i) == '{') {
            int end = code.indexOf('}', i);
            return end < 0 ? code.length() : end + 1;
        }
        while (i < code.length() && (Character.isLetterOrDigit(code.charAt(i)) || code.charAt(i) == '_')) {
            i++;
        }
        return Math.max(i, start + 1);
    }

    private static int identifierEnd(String code, int start) {
        int i = start;
        while (i < code.length() && isIdentifierPart(code.charAt(i))) {
            i++;
        }
        return i;
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
}
