# Contributing

Thanks for looking. This is a small personal project, so the process is
light, but the conventions are the ones it actually follows.

## Setup

    ./mvnw clean verify

`verify` is the single entry point. It compiles, runs the tests, and
enforces formatting, bug finding, architecture rules, and a coverage
floor. CI runs exactly this on every push and pull request, so a green
local `verify` is a green CI.

If formatting fails, run:

    ./mvnw spotless:apply

## Branches

One branch per issue, named `type/<issue>-<slug>`, for example
`feat/15-supply-chain`. Branch off `main`; do not commit to `main`
directly.

## Commits

    type(#<issue>): what changed

Keep the summary under 75 characters after the colon. Put the reason in a
second message line:

    git commit -m "feat(#15): add a keyless scheduled vulnerability scan" \
               -m "why: OWASP dependency-check needs an NVD key; OSV needs none"

Types in use: `feat`, `fix`, `ci`, `docs`, `test`, `chore`.

## Pull requests

- Target `main`, and reference the issue with `Closes #<n>`.
- Wait for CI to be green.
- Merged with a merge commit; the branch is deleted after.

## Tests

Add or update tests with any behaviour change. The build fails on a
formatting, architecture, or coverage-floor regression, so run `verify`
before opening a pull request.

## Adding a decision

Significant choices get a short record under `docs/adr/`. Copy the shape
of an existing one: context, decision, consequences, status.
