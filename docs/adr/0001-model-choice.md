# 1. Model choice: meta/muse-glimmer via LM Studio, keyless local-first

- Status: Accepted (2026-10-16)
- Supersedes: an earlier note proposing Ollama (never shipped)

## Context

The helper drafts LinkedIn, X, and blog posts from work notes, which are
private. There is no cloud API key in the environment, and per-token cost
and data egress are both unwanted for a personal tool. The machine has a
local GPU (RTX 4090, 18GB).

## Decision

Run `meta/muse-glimmer` (Q4_K_M, about 18GB, full GPU offload) through
LM Studio, which exposes an OpenAI-compatible endpoint at
`http://localhost:1234/v1`. The app uses `spring-ai-starter-model-openai`
pointed at that base URL with a dummy API key (`noop`), temperature 0.7,
and a 2000-token completion budget.

## Consequences

- No secrets and no per-token cost; notes never leave the machine.
- A local model server is a prerequisite. The README documents the model
  and a canned (`--ai=false`) path that works with no model at all.
- Quality tracks the model. The system prompt drives a first-person
  ghostwriter voice; tiny or ambiguous notes can still go sideways, so the
  README says to feed it real notes.
- Swapping providers is a one-line base-URL/API-key change, which is why
  the OpenAI-compatible starter was chosen over a bespoke client.
