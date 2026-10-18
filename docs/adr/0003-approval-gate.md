# 3. Approval gate: generation is free, publishing needs an explicit go

- Status: Accepted (2026-10-17)

## Context

Generating a draft is cheap, local, and reversible: it writes files the
user can delete. Publishing a post into the blog is outward-facing and
awkward to undo. An autonomous tool should not be able to do the second
on its own.

## Decision

Split the two. Generation writes drafts freely. Publishing goes through
`approve --slug <slug>` and does nothing unless the current invocation
also passes an explicit `--go`; without it the command is a dry run. An
existing published file is never overwritten.

## Consequences

- The tool never self-publishes; the human is the gate. This is a standing
  rule, not a default that can be toggled off in config.
- Publishing is a deliberate two-step: inspect the draft, then approve it.
- The approval verb carries the only side effect into the blog repo, which
  keeps the blast radius of a bug small.
