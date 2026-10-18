# 7. Container image, and why the native image is dropped

- Status: Accepted (2026-10-18)

## Context

The project is a short-lived CLI, so a container buys less than it would
for a long-running service. It still rounds out the deploy story: a single
artifact that runs anywhere with no local Java. A GraalVM native image was
on the table as an optional extra, timeboxed, to be dropped if it did not
pay off.

## Decision

Ship a multi-stage Dockerfile: build with the Maven wrapper on
`eclipse-temurin:21-jdk`, run on `eclipse-temurin:21-jre` as a non-root
user. Not a native image. The native image is dropped rather than
abandoned mid-build: it was not attempted.

## Consequences

- The container works with no local Java; `docker run` drafts from a
  mounted notes file, verified in canned mode with no model and no network
  beyond the image pull.
- To reach a model on the host, the container needs `--network host`
  (Linux) or a `host.docker.internal` base URL, since `localhost` inside
  the container is the container.
- The native image was judged not worth the cost for a CLI: it would need
  a GraalVM toolchain plus a native build profile, and reachability hints
  for the reflection-heavy parts (Spring Shell's command model and the
  OpenAI SDK's OkHttp and Jackson). The runtime gain is a few hundred
  milliseconds on a tool that also spends seconds in a model call.
- Revisiting it is cheap if wanted: add the `native-maven-plugin` and a
  `native` profile, then work through the hints. Recorded here so the
  decision is a choice, not an oversight.
