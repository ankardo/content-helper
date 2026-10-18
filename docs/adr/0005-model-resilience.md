# 5. One retry owner: Resilience4j over Spring Retry

- Status: Accepted (2026-10-18)

## Context

A local model call can hang or fail transiently. The helper needs a bound
on how long a call may take and how many times it is retried. Two
candidate mechanisms were on the table: Spring Retry, and the retry built
into the OpenAI SDK that Spring AI uses.

## Decision

Every model call goes through `ModelGuard`, which wraps the call in a
Resilience4j `TimeLimiter` (a timeout that cancels a hung call) plus a
Retry (bounded attempts, fixed backoff). The SDK's own retry is disabled
(`spring.ai.openai.max-retries=0`) so there is exactly one retry owner.

## Consequences

- A single hung call is actually cancelled, not just abandoned between
  attempts. Spring Retry was rejected because its timeout only stops
  retrying, it cannot cut off a call already in flight.
- Stacking our retry on the SDK's produced 12 requests for 3 calls before
  this was fixed, which is why the SDK retry is turned off.
- The SDK's own request timeout did not fire for a slow-but-connected
  response (a server sleeping 20s with a 1s timeout still completed), so
  the bound lives above the SDK. `ModelResilienceTest` covers the timeout
  and the retry-then-fallback paths against a mock server.
