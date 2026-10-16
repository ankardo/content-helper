# content-helper

Local draft helper: work notes in, LinkedIn, X and blog drafts out.
Java 21, Spring Boot, Spring Shell, Spring AI. Manual paste publishing,
no social APIs, nothing leaves your machine unless you paste it.

## Prerequisites

- Java 21
- A local OpenAI-compatible server for AI mode, e.g. LM Studio
  (tested with `meta/llama-glimmer` Q4). Without one, use `--ai=false`
  for canned structural drafts.

## Five minute run

```bash
./mvnw -B -DskipTests package
java -jar target/content-helper-0.0.1-SNAPSHOT.jar \
  draft --notes-file notes.txt --out drafts \
  --spring.shell.interactive.enabled=false
```

This writes `drafts/<timestamp>/linkedin.md`, `x.md` and `blog.md`.

From a git range instead:

```bash
java -jar target/content-helper-0.0.1-SNAPSHOT.jar \
  draft --range HEAD~5..HEAD --dir /path/to/repo --out drafts \
  --spring.shell.interactive.enabled=false
```

## Options

| Option | Default | Notes |
| --- | --- | --- |
| `--notes-file` | | Full sentences, preferred input |
| `--notes` | | Single words only, see below |
| `--range` | | Git log range, read from `--dir` |
| `--dir` | `.` | Repo for `--range` |
| `--out` | `drafts` | Output base dir |
| `--ai` | `true` | `false` for canned drafts, no model needed |

Why `--notes-file`: non-interactive Spring Shell splits `--notes`
values on whitespace, so a sentence arrives as its first word. A file
keeps text whole. The v1 bug behind the mysteriously bad drafts.

## Configuration

`src/main/resources/application.properties`:

| Key | Default | Notes |
| --- | --- | --- |
| `spring.ai.openai.base-url` | `http://localhost:1234/v1` | LM Studio server |
| `spring.ai.openai.api-key` | `noop` | LM Studio takes anything |
| `spring.ai.openai.chat.options.model` | `meta/llama-glimmer` | Any served model id |
| `spring.ai.openai.chat.options.max-completion-tokens` | `2000` | Reasoning models think first, budget room |
| `spring.ai.openai.chat.options.temperature` | `0.7` | Greedy decoding latches onto misreadings |

When the model is unreachable the command still exits zero with canned
drafts and logs the cause. CI relies on this: `mvnw verify` runs with
no model present.

## Examples

- `examples/ai/v1-launch/`: real model output, input in
  `examples/ai/v1-launch-notes.txt`. Outputs vary run to run.
- `examples/canned/v1-launch/`: same input, `--ai=false`.

Feed it real notes. One-word inputs get one-word drafts, and drafts
always want a human edit before posting.

## Tests

```bash
./mvnw -B test
```

Unit tests for writer, templates, generator (mocked `ChatClient`) and
shell wiring, plus the Boot context test. CI runs `mvnw verify` on
every push and pull request.

## License

MIT, see LICENSE.
