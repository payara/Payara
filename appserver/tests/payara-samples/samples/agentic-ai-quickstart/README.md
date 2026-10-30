# Agentic AI — Quickstart sample

The smallest Jakarta Agentic AI sample on Payara: a single `@Agent` answers a
question, exercising the four specification phases (`@Trigger`, `@Decision`,
`@Action`, `@Outcome`). A synchronous REST call fires the trigger event and
returns the LLM answer in the same response.

```
POST /agentic-ai-quickstart/api/ask   { "question": "..." }  ->  { "question", "answer" }
```

The LLM provider is selected by the server through MicroProfile Config — this
sample ships `META-INF/microprofile-config.properties` pointing at a small local
**Ollama** model (no API key, no cost):

```properties
payara.agentic.llm.provider=ollama
payara.agentic.llm.model=gemma3:4b
payara.agentic.llm.ollama.base-url=http://localhost:11434
```

## Run it manually

1. Install Ollama and pull the small model:
   ```powershell
   winget install Ollama.Ollama
   ollama pull gemma3:4b
   ```
2. Make sure the distribution contains the current `agentic-ai-core` module
   (the LLM backends + factory). From the Payara repo root:
   ```powershell
   mvn -q -pl appserver/agentic-ai/agentic-ai-core package
   Copy-Item appserver\agentic-ai\agentic-ai-core\target\agentic-ai-core.jar `
             appserver\distributions\payara\target\stage\payara7\glassfish\modules\agentic-ai-core.jar -Force
   ```
   Restart the domain clearing the OSGi cache (see the impl docs).
3. Build and deploy this sample:
   ```powershell
   mvn -q -pl appserver/tests/payara-samples/samples/agentic-ai-quickstart package
   asadmin deploy appserver\tests\payara-samples\samples\agentic-ai-quickstart\target\agentic-ai-quickstart.war
   ```
4. Ask:
   ```powershell
   $body = @{ question = "What is Jakarta EE in one sentence?" } | ConvertTo-Json
   Invoke-RestMethod -Uri http://localhost:8080/agentic-ai-quickstart/api/ask -Method Post -Body $body -ContentType "application/json"
   ```
   Watch `server.log` for `[TRIGGER]` → `[DECISION]` → `[ACTION]` → `[OUTCOME]`.
   An empty `question` demonstrates early termination: `@Decision` returns
   `Result(false, ...)` and `@Action` never runs.

## Integration test

`AgenticQuickstartIT` runs under the payara-samples Arquillian harness and does
**not** need a live LLM: its deployment adds `StubLargeLanguageModel`, which the
runtime uses (the application supplies its own `LargeLanguageModel`). The test
asserts the workflow returns the stubbed answer, and that a blank question
terminates before `@Action`.
