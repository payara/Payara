# Agentic AI — Tutorial Generator sample

A Jakarta Agentic AI agent that writes a **field-by-field guide** for a web form
and lets a developer **refine it through chat**. The sample form is a Customer
registration form to contract Azul Payara Server.

The page shows the form on the left and the generated guide on the right; a chat
box below sends refinement instructions to the agent.

```
GET  /agentic-ai/                     the side-by-side UI
GET  /agentic-ai/api/form             the form metadata (FormSpec)
POST /agentic-ai/api/tutorial/generate   generate a fresh guide
POST /agentic-ai/api/tutorial/refine     { "instruction": "..." }  refine the current guide
```

## How it works

`CustomerFormSpec` is the single source of truth: the page renders the live form
from it, and the agent explains the same fields. `TutorialAgent` runs the four
phases — `@Trigger`, `@Decision` (enough fields?), `@Action` (the LLM generates
or revises the HTML), `@Outcome` (store it). Refinement passes the *current
HTML + instruction* to the model each turn, so it edits the real artifact rather
than relying on memory alone.

The LLM provider is selected by the server via MicroProfile Config. This sample
uses **Anthropic (Claude)** for high-quality HTML; the system prompt is shipped
as config and reused as the prompt-cache prefix:

```properties
payara.agentic.llm.provider=anthropic
payara.agentic.llm.model=claude-opus-4-8
payara.agentic.llm.max-tokens=8192
payara.agentic.llm.system=You are a senior technical writer...
```

## Run it manually

1. Make sure the distribution contains the current `agentic-ai-core` module
   (LLM backends + factory) — see the impl docs for the package + hot-swap step.
2. Provide the Claude API key **before** starting the domain so the server
   process inherits it:
   ```powershell
   $env:ANTHROPIC_API_KEY = "sk-ant-..."
   .\bin\asadmin restart-domain
   ```
3. Build and deploy this sample:
   ```powershell
   mvn -q -pl appserver/tests/payara-samples/samples/agentic-ai package
   asadmin deploy appserver\tests\payara-samples\samples\agentic-ai\target\agentic-ai.war
   ```
4. Open `http://localhost:8080/agentic-ai/`, click **Generate tutorial**, then
   use the chat box to refine it (e.g. *"make the business email explanation
   friendlier and add an example"*). Watch `server.log` for
   `[TRIGGER]` → `[DECISION]` → `[ACTION]` → `[OUTCOME]`.

> To run fully local instead of Claude, switch the config to
> `payara.agentic.llm.provider=ollama` / `model=gemma3:12b` (a 12B-class model is
> recommended for HTML quality).

## Integration test

`AgenticTutorialIT` runs under the payara-samples Arquillian harness and does
**not** need a live LLM: its deployment adds `StubLargeLanguageModel`, which the
runtime uses. It asserts the form is exposed, a tutorial is generated, and a chat
refinement produces a different result.
