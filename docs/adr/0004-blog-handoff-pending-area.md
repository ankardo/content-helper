# 4. Blog handoff through a pending area

- Status: Accepted (2026-10-17)

## Context

The blog is a separate Astro repository. The site is built from
`src/content/blog/*.md` against a schema in `src/content.config.ts`
(required `title`, `description`, `pubDate`). Writing straight into that
directory from another tool risks breaking the build with malformed front
matter or a colliding slug.

## Decision

The helper writes to `src/content/_pending/<slug>.md`, a directory Astro
ignores, so a generated post can never break the site. Front matter is
written in code to match the Astro schema; the model writes only the body
prose. `approve --go` moves the file from `_pending` into
`src/content/blog`.

## Consequences

- The blog stays buildable at all times; the pending area is inert.
- Schema drift is contained to the one place that writes front matter.
- Publishing is a move between two directories, which is easy to test and
  to reason about, and the plan was verified against a copy of the real
  blog repo before shipping.
