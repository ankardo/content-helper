# content-helper

Local draft helper: work notes in, LinkedIn, X and blog drafts out.
Java 21, Spring Boot, Spring Shell, Spring AI. Manual paste publishing,
no social APIs, nothing leaves your machine unless you paste it.

## Prerequisites

- Java 21
- A local OpenAI-compatible server for AI mode, e.g. LM Studio
  (tested with `meta/muse-glimmer` Q4). Without one, use `--ai=false`
  for canned structural drafts.

## Five minute run

```bash
./mvnw -B -DskipTests package
java -jar target/content-helper-0.0.1-SNAPSHOT.jar \
  draft --notes-file notes.txt --out drafts \
  --spring.shell.interactive.enabled=false
```

This writes `drafts/<timestamp>/linkedin.md`, `x.md` and `blog.md`.
The three drafts are generated concurrently on virtual threads, so wall
time is close to the slowest single call instead of their sum.

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
| `spring.ai.openai.chat.options.model` | `meta/muse-glimmer` | Any served model id |
| `spring.ai.openai.chat.options.max-completion-tokens` | `2000` | Reasoning models think first, budget room |
| `spring.ai.openai.chat.options.temperature` | `0.7` | Greedy decoding latches onto misreadings |
| `spring.ai.openai.max-retries` | `0` | SDK retry is off, Resilience4j owns the retry |
| `content-helper.model.timeout` | `30s` | Max wall clock for one model call |
| `content-helper.model.retry-attempts` | `3` | Total attempts per call, including the first |
| `content-helper.model.retry-delay` | `500ms` | Fixed backoff between attempts |

Every model call is wrapped by `ModelGuard`, which bounds it with a
timeout and retries it a bounded number of times with a fixed backoff
(Resilience4j `TimeLimiter` plus `Retry`). A delay or an error therefore
ends in a retry, then the fallback, in bounded time. The OpenAI SDK has
its own retry, so it is turned off here; two layers stacked multiplies
the attempts instead of adding them, and the SDK timeout did not fire for
a slow but connected response in testing. The fallback is the existing
behaviour: exit zero with canned drafts and the cause logged.

When the model is unreachable the command still exits zero with canned
drafts and logs the cause. CI relies on this: `mvnw verify` runs with
no model present.

## Examples

- `examples/ai/v1-launch/`: real model output, input in
  `examples/ai/v1-launch-notes.txt`. Outputs vary run to run.
- `examples/canned/v1-launch/`: same input, `--ai=false`.

Feed it real notes. One-word inputs get one-word drafts, and drafts
always want a human edit before posting.

## Blog handoff

For an Astro blog (like `ankardo/digital-vault`), three verbs hand a
post over without publishing it until you say so.

```bash
# 1. Write a post into the blog repo's pending area (Astro ignores it)
java -jar target/content-helper-0.0.1-SNAPSHOT.jar \
  blog --notes-file notes.txt --into /path/to/blog \
  --spring.shell.interactive.enabled=false

# 2. See what is waiting for your go
java -jar ... pending --into /path/to/blog --spring.shell.interactive.enabled=false

# 3. Publish one, only with an explicit --go
java -jar ... approve --into /path/to/blog --slug my-post --go \
  --spring.shell.interactive.enabled=false
```

- `blog` writes `<blog>/src/content/_pending/<slug>.md`. The Astro
  loader globs only `src/content/blog`, so pending drafts never build.
- Front matter (`title`, `description`, `pubDate`, `tags`) is written
  by code to match the blog schema. The model writes the body only.
- `approve` moves one file into `src/content/blog/`. Without `--go` it
  is a dry run, and it never overwrites an existing post.
- Nothing is committed to the blog repo, and nothing publishes without
  your explicit go.

The same shell splitting applies to `--title`, so multi-word metadata
goes in the notes file header, the reliable path:

```markdown
---
title: A title with spaces
description: One line for the preview card.
tags: Java, Astro
slug: optional-short-slug
---
The notes body starts here and keeps every sentence.
```

`--title`, `--description`, `--tags`, `--slug` still work for single
tokens; the header wins for anything with spaces.

## Observability

Each run records Micrometer metrics (`content_helper.runs`,
`content_helper.run.duration`, `content_helper.fallbacks`) and logs one
structured line:

```
run command=draft mode=ai duration_ms=8123 outputs=3 fallback=false
```

Spring AI observes the model calls, so traces carry `gen_ai.*` spans,
and actuator is on the classpath. For machine-readable logs, add the
`json` profile (ECS on the console):

```bash
java -jar ... --spring.profiles.active=json
```

OpenTelemetry export is off by default so the normal build and CI stay
lean. To turn it on, build with the `otel` Maven profile and run with
its Spring profile; metrics and traces go to an OTLP endpoint on
`localhost:4318`.

```bash
./mvnw -B -Potel -DskipTests package
java -jar target/content-helper-0.0.1-SNAPSHOT.jar \
  draft --notes-file notes.txt --out drafts \
  --spring.profiles.active=otel \
  --spring.shell.interactive.enabled=false
```

## Tests

```bash
./mvnw -B test
```

Unit tests for writer, templates, generator (mocked `ChatClient`) and
shell wiring, plus the Boot context test. Two mock-server tests run the
real HTTP path against a local fake model, so neither needs a live one:
`ModelResilienceTest` (timeout, retry, fallback) and `WireContractTest`
(the outgoing request shape plus the non-200 and malformed-body fallback
paths). CI runs `mvnw verify` on every push and pull request.

## Build gates

`mvnw verify` is the single entry point and enforces every gate, so CI
fails on any of them:

- **Formatting** via Spotless with Palantir Java Format. Palantir keeps
  long string concatenations readable, which google-java-format splits one
  token per line. Run `mvnw spotless:apply` to fix locally.
- **Bug finder** via SpotBugs (max effort, medium threshold). The only
  excluded pattern is `EI_EXPOSE_REP2` on Spring beans, where storing an
  injected collaborator is the injection contract, not a leak.
- **Architecture** via ArchUnit: no cycles between the feature packages,
  `blog` never depends on `shell`, and the shell layer sits on top of the
  features.
- **Coverage** via JaCoCo, with a line floor of 80%. Measured coverage is
  85.7% locally and 84.0% on CI, so the floor sits below the observed
  cross-environment spread to gate real regressions without flapping.
  Raise it deliberately when coverage grows.

## Supply chain

- **Dependabot** opens weekly PRs for Maven and GitHub Actions updates.
- **SBOM**: a CycloneDX SBOM (`target/classes/META-INF/sbom/application.cdx.json`,
  131 components) is produced by `mvnw package` and both ships inside the
  jar and is uploaded as a CI artifact.
- **Vulnerability scan**: `.github/workflows/osv-scan.yml` runs OSV-Scanner
  weekly and on demand, off the pull request path so PR feedback stays
  fast. It scans the CycloneDX SBOM against the public OSV database
  (keyless, same NVD-sourced CVE data) and uploads a markdown report. It
  reports rather than gates; Dependabot opens the fix PRs. The first run
  on 2026-10-18 flagged 14 advisories in jackson-core and jackson-databind
  2.21.5 and 3.1.5, all with published fixes.

## License

MIT, see LICENSE.
