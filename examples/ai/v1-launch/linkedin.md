I shipped content-helper v1 this week. It's a Spring Boot CLI that turns my work notes and git ranges into LinkedIn, X and blog drafts.

It runs a local llama-glimmer model via LM Studio with Spring AI ChatClient. The ghostwriter prompts are tuned over five measured rounds, so the drafts actually sound like me.

CI is green on every push, 13 tests, and CodeRabbit reviews every PR. Publishing is still manual paste, no APIs hooked up yet.

Small tool, real workflow win.
Takeaway: Ship the helper before the perfect platform.