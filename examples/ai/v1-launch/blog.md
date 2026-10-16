---
title: "20261016-020454"
date: 2025-12-19
---

I shipped content-helper v1 this week.

It's a Spring Boot CLI that takes my work notes and a git range and turns them into drafts for LinkedIn, X and a blog. No APIs, no posting automation — just local generation and manual paste publishing.

I'm running a local llama-glimmer model through LM Studio and talking to it via Spring AI ChatClient. The ghostwriter prompts were the real work: five measured rounds of tuning until the output felt like me and not a press release.

The boring parts are done too. CI is green on every push, 13 tests are in, and CodeRabbit is on every PR.

It's rough, it's manual, and it's mine. v1 is working.