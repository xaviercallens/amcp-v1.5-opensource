I'll create a short TODO plan to review the current implementation against your orchestration spec, then scan the codebase to identify what's already implemented (or missing) around the orchestrator, agents, CloudEvents compliance, correlation handling, and prompts. Running multiple repository searches in parallel will speed this up.

1
Scan repository for orchestrator implementation and agent classes; map capabilities to event types

Verify CloudEvents 1.0 compliance in emitted events (required attributes, reverse-DNS types, datacontenttype)

Check correlationId strategy in requests/responses and timeout handling

Assess prompt management: planning prompts, few-shot examples, JSON enforcement

Evaluate multi-turn mesh chat flow support and agent sequencing

Review testing coverage for orchestrator and agents (unit/integration)

Propose actionable improvements and sequencing without implementing

To review what is already implemented versus your LLM Orchestration spec, I’ll scan the codebase for orchestrator/agent implementations, CloudEvents usage/compliance, correlation handling, prompts, and mesh chat support. I’ll run multiple searches in parallel to map capabilities and locate relevant classes and event types.

I’m going to open the key orchestrator, planning, correlation, fallback, normalization, and example agent classes to compare the current implementation with your specification. This will let me identify gaps and propose targeted actions without making changes. I’ll read them in parallel for speed.

4 more
Evaluate multi-turn mesh chat flow support and agent sequencing

6
Review testing coverage for orchestrator and agents (unit/integration)

1 more
Findings
[orchestrator classes present]
connectors/src/main/java/io/amcp/connectors/ai/CloudEventsCompliantOrchestratorAgent.java
 implements intent analysis, publishes task/error events, logs compliance, and integrates with Ollama.
connectors/src/main/java/io/amcp/connectors/ai/EnhancedCloudEventsOrchestratorAgent.java
 declares full feature set (planning, parallel dispatch, correlation, fallback, normalization, health) but many handlers are placeholders (e.g., 
handleTaskResponse()
, 
publishErrorEvent()
, 
cleanup()
).
connectors/src/main/java/io/amcp/connectors/ai/orchestration/EnhancedOrchestratorAgent.java
 implements a self-contained LLM planning + workflow coordinator using 
TaskProtocol
, but uses AMCP Event topics (e.g., orchestrator.*) rather than reverse-DNS CloudEvents types.
[task protocol & events]
connectors/.../orchestration/TaskProtocol.java standardizes orchestrator.task.request/response using AMCP Event with correlationId and structured 
TaskRequestData
 / 
TaskResponseData
. Good structure, but not wrapping into core/cloudevents/CloudEvent.
Topics widely use short names (orchestrator.request, task.response) vs reverse-DNS type naming.
[correlation & timeout]
connectors/.../correlation/CorrelationTrackingManager.java provides correlation contexts, chaining, timeouts, cleanup and metrics. This aligns with the spec’s correlation map and timeout handling.
EnhancedCloudEventsOrchestratorAgent
 declares a 
CorrelationTrackingManager
 but doesn’t yet wire it through request/response lifecycle.
[fallback]
connectors/.../fallback/FallbackStrategyManager.java includes re-prompting, alternate agent routing, partial response composition, circuit breaker, emergency messages. Strong base matching spec. Integration points into orchestrator flows appear missing.
[normalization]
connectors/.../normalization/DataNormalizationEngine.java normalizes queries and fields, extracts JSON blocks, cleans values. It does not yet offer specific helpers for “Nice, Fr” → “Nice,FR”/“Nice, France” or ISO date normalization; generic but not domain-specific.
[prompt optimization]
connectors/.../prompts/PromptOptimizationEngine.java has model-agnostic few-shot templates, JSON-only enforcement sections, and quality/performance scaffolding. This matches the plan’s strategies.
[cloudevents compliance infra]
core/src/main/java/io/amcp/cloudevents/CloudEvent.java
 and 
CloudEventsAdapter.java
 provide full v1.0 struct and conversion to/from io.amcp.core.Event. This is ready to enforce specversion/type/source/id/datacontenttype and extensions.
[agents/examples]
examples/.../meshchat/MeshChatAgent.java coordinates with orchestrator but uses orchestrator.request/response topics and free-form payloads in several places; not strictly 
TaskProtocol
 or CloudEvents.
examples/.../meshchat/TravelPlannerAgent.java returns rich textual responses, subscribes to orchestrator.task.travel.**, but does not emit standardized 
TaskProtocol.createTaskResponse()
 nor CloudEvents types. It mixes user-friendly strings vs structured JSON fields.
[tests/docs/demos]
MeshChat tests exist (examples/src/test/java/io/amcp/examples/meshchat/...).
Orchestrator demos (
examples/src/main/java/io/amcp/examples/orchestrator/OrchestratorDemo.java
) are architectural, not end-to-end CloudEvents conformance/timeout/correlation tests.
Docs in docs/ describe CloudEvents compliance and orchestrator features.
Gaps vs Spec
[reverse-DNS type naming]: Many event topics/types are short-form (orchestrator.request) instead of reverse-DNS CloudEvents types like io.amcp.orchestration.task.request. Some constants exist (“io.amcp.*”) in 
CloudEventsCompliantOrchestratorAgent
, but broader code paths still use short topics.
[CloudEvents envelope]: Orchestrator and agents use io.amcp.core.Event with metadata. They’re not consistently building and publishing core/cloudevents/CloudEvent (or converting via 
CloudEventsAdapter
) for every outbound event.
[correlation propagation]: 
TaskProtocol
 sets correlationId on request/response; however, 
EnhancedCloudEventsOrchestratorAgent
 doesn’t yet connect send/receive hooks to 
CorrelationTrackingManager
 to enforce expected responses or fan-in completion rules.
[fallback wiring]: 
FallbackStrategyManager
 is implemented, but orchestrators do not currently invoke it on malformed LLM output, timeouts, or agent errors in a cohesive flow.
[data normalization specifics]: No explicit helpers for country codes, ISO dates, language code normalization, or “Nice, Fr” style transformations required by the examples in the spec.
[agent response schema]: Example agents produce human-readable strings; they should emit structured JSON fields per template (e.g., temperature, conditions, budget, currency) and echo correlationId and optional sourceAgent.
[mesh chat sequencing]: MeshChat exists but lacks explicit standardized priorMessages contracts across specialized agents with clearly named fields and response scaffolding (speaker, message, correlationId) in CloudEvents envelopes.
[observability/tracing]: No standard traceId/amcpTraceId extension is applied across events; lifecycle compliance logging exists in one class, not system-wide.
[testing coverage for orchestrator core]: Unit/integration tests for planning parsing, correlation timeouts, fallback behavior, and CloudEvents schema validation are not evident for the enhanced orchestrators.
Recommended Actions (no code changes yet)
[CloudEvents envelope & types]
Adopt 
CloudEventsAdapter
 in orchestrators/agents to publish/consume CloudEvents for all task/user response events.
Standardize reverse-DNS types:
Task plan: io.amcp.orchestration.task.plan
Task request/response: io.amcp.orchestration.task.request / io.amcp.orchestration.task.response
Orchestrator complete: io.amcp.orchestration.complete
Errors/fallbacks: io.amcp.orchestration.error, io.amcp.orchestration.fallback
Ensure required attributes per 
CloudEvent
 (specversion 1.0, type, source, id, time, datacontenttype=application/json).
Files to update: 
EnhancedCloudEventsOrchestratorAgent
, 
EnhancedOrchestratorAgent
, 
MeshChatAgent
, example agents, and any publisher code paths.
[correlation management end-to-end]
Integrate 
CorrelationTrackingManager
 in 
EnhancedCloudEventsOrchestratorAgent
:
On every task request: create correlation or add child correlation; attach correlationId (and CloudEvents extension like amcpTraceId).
On task response: route to 
trackResponse()
, decrement expected counts, and finalize with timeout fallback via 
waitForResponse(...).orTimeout(...)
.
Reject/log any response missing the proper correlationId.
Add lifecycle correlation logging at open/close.
[fallback orchestration]
Wire 
FallbackStrategyManager
 into:
LLM-plan parsing errors: use 
handleMalformedLLMOutput()
 once; if still invalid, route to default single-agent or direct LLM response.
Task execution errors/timeouts: 
handleAgentFailure()
 with retries, alternate agent routing, or compose partial response.
Emit io.amcp.orchestration.fallback CloudEvents when fallback activates.
[task planning & dispatch]
In 
EnhancedCloudEventsOrchestratorAgent
:
Replace placeholder planning/dispatch with 
TaskPlanningEngine
 output validation.
Execute tasks in parallel where dependencies permit; otherwise, enforce sequencing per the plan’s dependencies.
Strictly validate tasks against discovered capabilities/agents (unknown capabilities → fallback).
[data normalization enrichment]
Extend 
DataNormalizationEngine
 to include:
Location normalization: city + ISO country code (“Nice, FR”) and presentation mapping (“Nice, France”).
Date normalization to ISO (YYYY-MM-DD).
Language codes (lowercase two-letter).
Lightweight country code map and common city/airport resolution helpers.
Normalize both outbound request params and inbound results.
[agent-side compliance]
Update example agents (e.g., 
TravelPlannerAgent
, Weather/Stock agents if present) to:
Parse 
TaskProtocol.parseTaskRequest(event)
 for structured inputs.
Emit responses via 
TaskProtocol.createTaskResponse()
, echoing correlationId, with structured fields per domain (e.g., itinerary[], budget, currency; or temperature, conditions).
Include sourceAgent in data (either as a data field or CloudEvents extension).
On errors, send a response with error populated rather than silence.
[mesh chat multi-turn]
Define a consistent schema for multi-turn prompts:
Request data: priorMessages as a list with {role, content, timestamp, speaker?}.
Response data: {message, speaker, correlationId}.
Ensure orchestrator sequences personas (manager → tech → culture) with priorMessages passed forward.
Add a max-turn or convergence stop rule.
[prompt optimization integration]
Centralize prompts through 
PromptOptimizationEngine
:
Planning: JSON-only, few-shot, examples aligned with actual capability names.
Parameter extraction: produce structured params before dispatch.
Response synthesis: aggregate multiple agent results into cohesive answers.
Add model-type config (TinyLlama/Open-source vs GPT/Claude) through 
ModelConfiguration
 with consistent JSON enforcement.
[observability & tracing]
Add a distributed tracing extension across all CloudEvents (e.g. amcpTraceId).
Emit io.amcp.orchestration.complete with timing, agent list, correlationId.
Expose orchestrator/agent health via periodic io.amcp.agent.health events.
[test coverage]
Unit tests:
Planning parsing: valid JSON array, unknown capability handling, empty plan behavior.
Correlation timeouts: ensure timeout triggers fallback and does not hang.
Normalization utilities: location/date/lang testcases (“Nice, Fr”).
CloudEvents validation: construct events and validate required fields.
Integration tests:
Weather-only: normalized input, structured response, final user response composition.
Travel + Weather: parallel dispatch, aggregation, and timeout path.
Empathetic chat + quote: two-task plan, missing one agent fallback.
Mesh chat: sequential personas with priorMessages, missing one persona path.
Where to place: connectors/src/test/java/... and examples/src/test/java/...
Add schema assertions for event types and required attributes.
[performance & load]
Simulate multiple concurrent orchestrations and measure:
Average end-to-end latency.
Pending map cleanup and memory.
LLM response time distributions (record via 
PromptOptimizationEngine
).
[CLI/demo alignment]
Update 
cli/AMCPInteractiveCLI.java
 registrations to ensure orchestrator used is the CloudEvents-compliant variant and that demos publish CloudEvents types through 
CloudEventsAdapter
.
Suggested Implementation Targets
Orchestrator logic: connectors/.../EnhancedCloudEventsOrchestratorAgent, connectors/.../orchestration/EnhancedOrchestratorAgent
Protocol: connectors/.../orchestration/TaskProtocol.java
Correlation: connectors/.../correlation/CorrelationTrackingManager.java
Fallback: connectors/.../fallback/FallbackStrategyManager.java
Normalization: connectors/.../normalization/DataNormalizationEngine.java
Prompts: connectors/.../prompts/PromptOptimizationEngine.java
Agents: examples/.../meshchat/TravelPlannerAgent.java, similar weather/stock/chat agents
CloudEvents: core/.../cloudevents/CloudEvent.java, 
CloudEventsAdapter.java
Next Steps (proposed order)
Enforce CloudEvents publishing in orchestrator send paths using 
CloudEventsAdapter
, with reverse-DNS types and datacontenttype=application/json.
Wire correlation manager into orchestrator request/response lifecycles; add timeouts and completion rules.
Hook fallback manager for malformed LLM outputs and task failures/timeouts.
Extend normalization engine for location/date/lang specifics and apply pre/post processing.
Agent schema updates to use 
TaskProtocol
 for responses and structured fields; echo correlationId.
Mesh chat schema & sequencing with standardized priorMessages and personas.
Integrate prompt engine for planning/param extraction/synthesis; remove ad-hoc prompts.
Add observability: amcpTraceId, orchestration complete/health events.
Add tests for planning, correlation/timeout, normalization, CloudEvents compliance, and scenario flows.
Performance tests and clean-up verifications under load.
Completion Status
Mapped current implementation vs. your specification across orchestrator logic, CloudEvents, correlation, agents, prompts, mesh chat, and tests.
Proposed a prioritized action plan without making code changes.