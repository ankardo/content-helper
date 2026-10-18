# 2. Non-interactive option splitting workaround: notes-file and header

- Status: Accepted (2026-10-16)

## Context

The tool is driven non-interactively (`--spring.shell.interactive.enabled=false`)
so it can run from scripts. In that mode Spring Shell parses the command
line itself, and a quoted option value containing spaces arrives as only
its first word: `--notes "shipped the billing rewrite"` reached the command
as `shipped`. Because every sentence in the notes was reduced to one word,
the model produced nonsense. The same bug later bit `--title`.

## Decision

Prefer file-based input for anything that can contain spaces. Notes come
from `--notes-file`, with an optional `---`-delimited header for multi-word
fields:

    ---
    title: Shipping the billing rewrite
    description: What changed and why
    tags: billing, spring
    ---
    <free-form notes>

Single-word values can still be passed inline.

## Consequences

- Multi-word input is reliable in scripted runs, which is the main use.
- There is a small custom header format to document and test (it is
  covered by shell tests).
- Interactive users lose the convenience of long inline flags, an accepted
  trade for a script-first tool.
