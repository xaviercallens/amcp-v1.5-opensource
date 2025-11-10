Quarkus AMCP Extension (v1.6) – Implementation Plan & Alignment Proposal
Overview: We propose to create a Quarkus extension for the Agent Mesh Communication Protocol (AMCP) to seamlessly run AMCP agents inside Quarkus applications, and to align AMCP v1.6 with Quarkus and emerging standards (like A2A and MCP). This involves two parts:a
Building the Quarkus AMCP Extension (PoC): A technical implementation where Quarkus manages AMCP’s runtime – instantiating agents (e.g. the HelloWorldAgent example), configuring the event broker (Kafka/NATS/Solace), and integrating with Quarkus’s build-time and CDI frameworks. We include code examples and a multi-instance test scenario to validate agent communication across a cluster.
Aligning AMCP v1.6 with Quarkus & Open Standards: Strategic changes to AMCP for smoother integration and adoption – modularizing the Java package for clean dependency use, addressing license compatibility (from GPL concerns to a permissive license), and coordinating on Agent-to-Agent (A2A) and Model Context Protocol (MCP) bridges. This includes incorporating Red Hat’s security best practices (authentication, authorization) into agent communications, since current A2A/MCP specs lack a full security model.



1. Quarkus AMCP Extension – Technical Implementation (PoC)
Design Goal: Create a Quarkus extension that wraps the AMCP v1.6 core library to manage agent lifecycle and broker connectivity, leveraging Quarkus’s build-time optimization and CDI for seamless integration. Quarkus extensions split logic between a deployment module (build-time) and a runtime module . The deployment part will scan for agent classes and prepare configuration, while the runtime part will initialize the AMCP AgentContext and register agents at application startup. This yields minimal startup overhead and tight integration:
Fast Startup: By processing agent metadata at build-time and generating necessary bootstrap code, we retain Quarkus’s millisecond startup benefits  even with AMCP onboard.
Unified Config: We expose AMCP settings via application.properties (e.g. quarkus.amcp.broker.type=kafka rather than a separate config file), aligning with Quarkus config conventions . This allows using Quarkus config sources (Kubernetes ConfigMaps, etc.) and avoids fragmenting configuration management.
Dependency Injection: Agents can be made CDI beans, allowing them to inject other Quarkus resources (like a DataSource or REST client) if needed. Conversely, the extension can provide AMCP services (like an EventBus handle) as injectable beans for user code.
Graceful Lifecycle: Quarkus will manage starting the agent mesh on startup and shutting it down on application termination (via a shutdown hook or @PreDestroy), ensuring no dangling threads or connections.
Build-Time Augmentation: During the Quarkus build, the extension will scan the application for any classes that extend AbstractMobileAgent (the base AMCP agent class)  or implement a marker interface like MobileAgent. This can be done using the Jandex index provided by Quarkus deployment. For each such agent class found, we register it for reflection (so it can be instantiated) and possibly generate a small bootstrap call. For example, we might generate a AmcpAgentsRegistry class that has a static list of agent class references. This avoids doing classpath scanning at runtime. Quarkus’s philosophy is to do as much work as possible at build time  – scanning for agents and preparing configuration can all happen then.
We also define a Quarkus config object (using @ConfigProperties or @ConfigMapping) for AMCP settings like broker type, host/port, etc. For example:
1     @ConfigProperties(prefix = "quarkus.amcp")
2     public class AmcpConfig {
3         public BrokerType brokerType;
4         public String kafkaBootstrapServers;
5         public String natsServers;
6         // ... other fields for Solace, etc.
7     }

The extension will register this config class, mapping properties like quarkus.amcp.kafka.bootstrap.servers to AmcpConfig.kafkaBootstrapServers (which correspond to AMCP’s own config keys, e.g. amcp.kafka.bootstrap.servers ). This ensures users can configure the broker connection via Quarkus config. The extension can also set sensible defaults; for instance, if brokerType is not set, default to memory (AMCP’s in-memory broker for dev) or kafka for production.
Runtime Initialization: At runtime, the extension’s Recorder (a Quarkus mechanism to capture init logic) will execute code to start the agent environment. Pseudocode for the recorder logic:
1     public class AmcpRecorder {
2         public void initAgentMesh(AmcpConfig config, List<Class<?>> agentClasses) {
3             // 1. Initialize AMCP core with broker settings
4             EventBroker broker = BrokerFactory.connect(config.brokerType, config); 
5             AgentContext context = AgentContext.boot(broker);
6             
7             // 2. Instantiate and register each discovered agent
8             for (Class<?> agentClass : agentClasses) {
9                 AbstractMobileAgent agent = (AbstractMobileAgent) agentClass.getDeclaredConstructor().newInstance();
10                 context.registerAgent(agent);
11             }
12             // 3. Optionally: if AMCP requires explicit activation, activate agents or contexts.
13             context.start();  // e.g., start processing events
14         }
15     }

(Note: Actual API of AMCP might differ; this is illustrative.)
This recorder method would be invoked at RUNTIME\_INIT phase of Quarkus startup, meaning after CDI container is up . By this point, any dependencies agents need (like config or other beans) can be injected or available. The agentClasses list was prepared at build-time and passed into the recorder. The above code connects to the message broker as configured (e.g., for Kafka it will use config.kafkaBootstrapServers), then creates an AgentContext. In AMCP v1.5, an AgentContext is implicitly created when an agent is run via the CLI; in our integration, we explicitly create it and tie it to the Quarkus app lifecycle. We then instantiate each agent. Because we registered those classes for reflection at build time, they can be newInstance() safely even in a native image. Alternatively, we could register each agent as a CDI bean (with @Singleton scope) and simply retrieve them from the container (this would allow field injection into the agent). The extension can choose either approach. For simplicity, the PoC might directly construct the agents. Finally, it registers them with the context and starts event processing.
Agent Definition in Quarkus: The developer experience will be straightforward – define an agent class by extending AMCP’s base class and optionally using Quarkus annotations. For example, the HelloWorld agent might be written as:
1     import io.amcp.core.AbstractMobileAgent;
2     import io.amcp.api.Event;
3     import java.util.concurrent.CompletableFuture;
4     @ApplicationScoped  // optional CDI annotation
5     public class HelloWorldAgent extends AbstractMobileAgent {
6         @Override 
7         public void onActivate() {
8             super.onActivate();
9             subscribe("hello.**"); 
10             logMessage("🌍 HelloWorld Agent is alive!"); 
11         }
12         @Override 
13         public CompletableFuture<Void> handleEvent(Event event) {
14             return CompletableFuture.runAsync(() -> {
15                 if (event.getTopic().equals("hello.request")) {
16                     String name = event.getPayload(String.class);
17                     logMessage("📨 Received: " + name);
18                     // Respond to the sender
19                     publishEvent("hello.response", "Hello, " + name + "!");
20                 }
21             });
22         }
23     }

This is essentially the same as the vanilla AMCP example , except we could add @ApplicationScoped to let Quarkus manage its lifecycle (not strictly required; the extension could manage it internally). When the application runs, the extension ensures this agent is running as part of the mesh. The call to subscribe("hello.**") means the agent listens on all topics under “hello.” prefix , and in handleEvent it specifically reacts to hello.request events by replying with a hello.response event.
Testing the Extension (HelloWorld Multi-Agent Scenario): To validate the extension:
We configure Quarkus to use a real broker (say Kafka) for testing. In application.properties of the test, set:
quarkus.amcp.broker.type=kafka        quarkus.amcp.kafka.bootstrap.servers=localhost:9092    (Kafka could be a Testcontainers instance or an in-memory alternative.)
We include the HelloWorldAgent class in the app. The extension should pick it up and register it.
Test Scenario 1: Single Node functionality – Start the Quarkus application with HelloWorldAgent.
Expected: On startup, logs should show “HelloWorld Agent is alive!”  indicating the agent’s onActivate ran and it subscribed successfully. The agent should be connected to Kafka (we expect to see the consumer subscription to topic hello.*).
Then, produce a test message to Kafka topic hello.request with payload "Quarkus" (this simulates an external event or another agent’s output). This can be done via a Kafka producer in the test.
Expected: The HelloWorldAgent’s log should show it received the event (“Received: Quarkus”), and we should observe an output event on topic hello.response with value "Hello, Quarkus!". The test can consume from hello.response topic to verify the message. This confirms that the agent running inside Quarkus can consume and produce events via the broker.
Test Scenario 2: Multi-Instance Mesh – Start two instances of the Quarkus app (simulate two pods in Kubernetes), both running HelloWorldAgent and pointing to the same Kafka cluster. This tests distributed agent coordination:
Both instances on startup log their “Agent is alive” message (each has its own context but same subscription).
When a hello.request event is sent (by an external producer or one of the agents), both agent instances will receive it (because by default, a Kafka topic with two consumers without a consumer group behaves like pub/sub; we may configure them as durable subscribers if needed). Each instance will then publish a hello.response. We expect two responses for one request. In a real use-case, that might be redundant, but for testing it’s fine – it shows that multiple agents can listen and react independently. (If we wanted only one agent to respond, we could configure them in a Kafka consumer group so that the event is load-balanced; testing that configuration is also valuable.)
Verify that both responses are received. We can enhance the agent to include its instance ID in the response (e.g., “Hello from AgentA” vs “AgentB”) to prove both took action. Alternatively, inspect logs from both instances to see they processed the request.
This scenario proves that our Quarkus extension properly connects multiple app instances into one logical mesh via the broker – a core feature of AMCP. Essentially, each Quarkus instance hosts an AgentContext; the Kafka broker links those contexts so the agents communicate as if in one system  .
Discussion: The HelloWorld test is simplistic (one topic). For a more complex example, we could create two agent classes (e.g., PingAgent and PongAgent) and test that they can exchange messages in a loop. But for a PoC, confirming basic pub-sub and multiple instance behavior is enough. The excellent performance of AMCP over Kafka (25k+ events/sec with p99 ~5ms  in benchmarks) should be preserved. Quarkus’s minimal overhead (the extension work is mostly at build-time) means we can achieve near-native AMCP performance inside Quarkus. For instance, if AMCP alone starts an agent in ~100ms and migrates in <500ms on Kafka , Quarkus might add only a few milliseconds overhead to startup, and zero overhead to migration (since that’s broker-protocol-bound). The extension itself is lightweight – primarily orchestrating AMCP’s initialization – so we don’t expect any bottleneck. This approach essentially turns Quarkus into an AMCP container, combining Quarkus’s efficient container runtime with AMCP’s agent model.
Code Example – Quarkus Extension Skeleton: Below is a simplified illustration of what the Quarkus extension’s deployment code might look like. This isn’t full production code, but outlines how we gather agent classes and register a recorder:
1     // Deployment module code
2     class AmcpProcessor {
3         private static final DotName AGENT_BASE = DotName.createSimple("io.amcp.core.AbstractMobileAgent");
4     
5         @BuildStep
6         FeatureBuildItem feature() {
7             return new FeatureBuildItem("amcp"); 
8             // registers the extension feature for logging/diagnostics
9         }
10     
11         @BuildStep
12         void registerAgentsAndConfig(BuildProducer<ReflectiveClassBuildItem> reflective, 
13                                      BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
14                                      CombinedIndexBuildItem index) {
15             // 1. Discover agent classes
16             List<ClassInfo> agents = index.getIndex().getAllKnownSubclasses(AGENT_BASE);
17             List<String> agentClassNames = new ArrayList<>();
18             for (ClassInfo info : agents) {
19                 if (!info.isAbstract()) {
20                     agentClassNames.add(info.name().toString());
21                     // Register for reflection (for native image instantiation)
22                     reflective.produce(new ReflectiveClassBuildItem(true, true, info.name().toString()));
23                 }
24             }
25             // 2. Register runtime init call
26             RuntimeValue<List<String>> agentsListHandle = recorderContext
27                   .newInstance(new ArrayList<String>(agentClassNames));
28             recorder.initAgentMesh(recorderContext.newInstance(AmcpConfig.class), agentsListHandle);
29             // The above line conceptually records a call AmcpRecorder.initAgentMesh(config, agentClasses)
30             // (Adjustments needed for correct Quarkus Recorder APIs not shown for brevity)
31         }
32     }

In reality, Quarkus requires splitting into deployment and runtime modules and some indirection for passing classes to the recorder, but the essence is:
We collected agentClassNames for all subclasses of AbstractMobileAgent in the application.
We marked them for reflection in native mode.
We schedule the AmcpRecorder.initAgentMesh() to be called at runtime, providing it the config and the list of agent classes. The AmcpConfig is a regular runtime config bean that Quarkus will instantiate from properties.
This extension would be packaged as io.quarkus:quarkus-amcp artifact, and when included, it automatically wires up the AMCP engine. As a result, developer usage is very simple: add the dependency and write agent classes. There’s no need to manually write a main() or manage threads – Quarkus and AMCP handle that.
Outcome: Once this extension is implemented, we have effectively created a Kubernetes-native multi-agent platform. A developer can deploy N instances of a Quarkus app, each running some agents, and thanks to AMCP’s broker-based mesh, those agents work together. Quarkus provides the glue to configure and run this reliably (much like it does for Kafka Streams or other libraries). We also get all of Quarkus’s production features for free – health checks (we can add a readiness check that the agent context connected to broker), metrics (we can expose AMCP’s metrics via MicroProfile Metrics), and easy packaging into a native binary if desired. It’s worth noting that AMCP itself is Java 21 based  and designed to be cloud-ready (they provide Docker, Helm charts ). Our extension makes it a first-class citizen inside Quarkus apps, which is ideal for Red Hat’s OpenShift ecosystem.
2. Aligning AMCP v1.6 with Quarkus and Open Standards
To maximize the benefits of the Quarkus integration and encourage broad adoption, we recommend several enhancements to AMCP in its upcoming v1.6 release. These touch on packaging, licensing, and protocol-level interoperability, as well as security improvements. The table below summarizes the key alignment areas:

Let’s discuss each of these in detail:
2.1 Packaging and Modularization
Current State: The AMCP open-source edition v1.5 appears to be organized as a Maven project with multiple components (e.g., a core, connectors, CLI, etc.) under one repository  . The entire project is licensed under MIT . For Quarkus integration, we will likely include AMCP as a dependency (as a JAR). We need that JAR to be clean and minimal – ideally just the core runtime and whichever connector we use for messaging. In v1.5, the connectors for Kafka, NATS, Solace are probably included (the config keys exist in core). If they are separate modules (e.g., an amcp-kafka submodule), we can choose to include only what’s needed.
Proposal: Ensure AMCP v1.6 has a well-defined core library that contains the agent model, event model, and interface for brokers, but not necessarily all broker implementations. Each broker support can be an optional module (AMQP, Kafka, etc.). This way, the Quarkus extension can depend only on, say, amcp-core and amcp-kafka if we plan to use Kafka – avoiding pulling in unused connectors. Similarly, if AMCP has an optional CLI utility or examples, those should be separate and not required at runtime. This modularization also helps with native image size (unused code won’t be included if not referenced).
Additionally, publish AMCP artifacts to Maven Central (or Red Hat’s maven if productized). Enterprise developers expect to add a dependency like io.amcp:amcp-core:1.6 and get the library. As of now, v1.5 might require building from source since it’s brand new (we saw references to mvn install in docs ). By v1.6, having official artifact distribution will ease adoption and integration (Quarkus extension can just pull the artifact). We should also ensure the Java package name is unique and doesn’t conflict (likely io.amcp or similar, which it seems to be from context).
In terms of Java module system (JPMS), if AMCP v1.6 can provide module info (automatic module or explicit), it’s a bonus for those who use JPMS. Quarkus itself doesn’t require JPMS, but modular JARs signal good dependency hygiene.
2.2 Licensing Considerations
Current State: The open-source AMCP is under the MIT License , which is a permissive license compatible with Apache-2.0 (Quarkus is Apache-2.0 licensed). MIT is generally enterprise-friendly; however, there were mentions of GPL in the context, possibly because earlier internal versions or some components might have been GPL. For example, if any part of AMCP was derived from GPL code or uses a GPL-licensed library, that would be a problem for Quarkus integration (Quarkus cannot include GPL code without infecting the whole distribution).
From the repository, it looks like AMCP is new code (likely no GPL dependencies, since they call out everything as open source and even make a patent non-assertion promise ). So the GPL concern may be hypothetical or precautionary. Nonetheless, we should double-check that:
All third-party libs used by AMCP are permissively licensed (Apache, MIT, BSD, EPL, etc.). E.g., if they use JClouds or something unlikely; but likely they use Apache Kafka client (Apache license), NATS client (Apache or MIT), Solace Java API (likely LGPL or Apache, need to verify).
If any GPL or AGPL library is used (e.g., some persistence or an NLP library), we either remove that or isolate it behind an optional interface that’s not included in the core build. For instance, if “Ollama integration” calls an external process, it likely doesn’t introduce GPL issues. But if they had embedded some GPL code for TinyLlama, that needs review.
Relicensing Option: Red Hat has a strong preference for Apache 2.0 in open source projects due to the explicit patent grants and familiarity. While MIT is similarly permissive, to truly harmonize, the AMCP project could consider dual-licensing MIT and Apache (contributors agree to license under both), or switch to Apache-2.0 entirely. Dual-licensing MIT/Apache is somewhat redundant (Apache 2.0 is basically a superset of MIT with patent terms), so a move to Apache-2.0 for v1.6 could be justified, especially if Red Hat becomes a major contributor. This would reassure any downstream users that all code is under Apache terms, which many enterprise procurement checklists explicitly list as acceptable.
If by chance some portion must remain GPL (worst case), one could add a GPL linking exception to allow Quarkus (Apache) to link to it without contamination. The GNU Classpath project did this for the Java standard library under GPL, for example. But ideally, we avoid this route by simply not having GPL code. Given AMCP’s current license is MIT, it’s unlikely we need a linking exception. We mention it just for completeness: e.g., “if AMCP integrated any GPL-3.0 components, add a clarification: ‘As an exception, you have permission to link this library with independent modules and not open-source those modules under GPL.’” However, since we can simply not use GPL modules, this is probably moot.
Bottom line: Keep AMCP permissive. MIT is fine; Apache 2.0 would be fine. Ensure all contributions and dependencies adhere to that. Red Hat’s legal team can help audit and advise; if there’s any doubt, an Incubator or sandbox process can be done under the Eclipse or CNCF umbrella in the future, which usually requires Apache-2.0 licensing. For now, the path of least resistance is: no GPL code, and possibly add a note that using the AMCP library does not impose copyleft on the application (which MIT already guarantees).
2.3 A2A Protocol Bridge Integration
Current State: AMCP v1.5 touts a “Protocol Bridge” for Google’s Agent-to-Agent (A2A) protocol , claiming “bidirectional compatibility.” This likely means an AMCP agent can communicate with an agent following the A2A spec (which uses JSON messages, typically over HTTP or other transports). It’s not explicit how it’s done – possibly AMCP runs an HTTP server to handle A2A requests, or it can register with an A2A Directory. Meanwhile, Red Hat (and others) have developed an official A2A Java SDK (open-sourced under Linux Foundation). To avoid duplication and ensure future-proofing, AMCP v1.6 should align with that official SDK:
Use the A2A SDK data structures and client for any A2A interactions. For example, if an AMCP agent wants to send a task to an external agent, use the SDK to format the request as per the A2A spec (which defines fields like performative, content, etc.).
Possibly incorporate the SDK’s server component: The SDK likely can produce a JAX-RS resource or similar to handle incoming agent messages. The Quarkus extension could then simply mount that resource. Alternatively, AMCP could continue using its internal event system for external messages by translating A2A -> internal event and vice versa. In v1.6, we might implement an A2A Gateway Agent within AMCP: an agent that listens on a special topic for incoming A2A messages and conversely can emit outwards. Using the SDK would simplify this, as it handles all the JSON and HTTP details. We just plug it into Quarkus or the AMCP run loop.
Ensure protocol compliance tests: The A2A spec is evolving, and there will be test suites. Red Hat and Google can help validate that AMCP’s implementation passes these, meaning any A2A-compliant agent (be it built with Google’s framework or a Python library like LangChain’s A2A support) can talk to an AMCP agent. For example, if an external agent sends an ACTION request to HelloWorldAgent via A2A (perhaps with content “send hello.request to Bob”), our system should correctly interpret or route that.
By aligning with A2A, we open AMCP’s mesh to outside agents – effectively making a Quarkus+AMCP deployment part of the larger “internet of agents.” Concretely, an AMCP agent could register itself in a global A2A directory with its capabilities, or handle tasks coming from such a directory. This goes beyond the local event mesh. From a Red Hat standpoint, this is critical for interoperability between different agent frameworks in an enterprise.
In implementation terms, we plan for the Quarkus extension to expose an HTTP endpoint (REST or gRPC) for A2A. Quarkus is very capable at HTTP, and can reuse existing security (we can protect it with OAuth2 if needed, see security section). The extension might use the A2A SDK to implement, say, POST /a2a/message that accepts messages and feeds them into the AMCP event system. For outgoing, the extension/AMCP could act as an HTTP client to send messages to other agents’ endpoints. This likely involves mapping A2A message IDs to AMCP event IDs and vice versa.
Red Hat’s Input: Red Hat can ensure that any edge cases (like large payloads, binary attachments, etc.) are handled. Also, if the A2A spec evolves, having AMCP developers looped into that community will ensure early adoption of changes.
2.4 MCP (Model Context Protocol) Integration
Current State: Model Context Protocol (MCP) is an emerging standard focusing on how AI agents invoke tools and external services in a standardized way (pioneered by Anthropic and others). AMCP v1.5 lists MCP as a planned integration  but it’s likely not implemented yet (since MCP itself is in development). By v1.6, we should aim for a basic MCP support:
Allow AMCP agents to call MCP endpoints: For example, an agent might want to use a “Calculator” tool. If there’s an MCP-defined API for that (say a REST endpoint with a certain JSON schema), the agent should be able to call it. We can either embed an MCP client or simply document how to do it. Perhaps integrate with an existing MCP client library if available.
Allow AMCP agents to expose themselves as MCP tools if appropriate: This is less straightforward, but imagine an agent that has a capability (like WeatherAgent can fetch weather). It could register as an MCP tool (“Weather”) so that external LLMs or agent frameworks could call it through MCP without fully speaking AMCP or A2A. This might overlap with A2A directory (A2A is more agent-to-agent tasks, MCP is more stateless tool usage).
Given Quarkus can easily host REST endpoints, one idea is to use Quarkus to deploy a MCP Adapter: basically a REST service that translates MCP requests to AMCP events. For instance, an MCP “Action” request might come in to get weather for Paris; the adapter turns this into an internal event weather.request(city="Paris") that our WeatherAgent (running under AMCP) handles, and then the adapter returns the result back in MCP format. This adapter could be part of the Quarkus extension or a separate microservice. For efficiency, if the AMCP context itself can speak HTTP, it might handle it directly; but decoupling via Quarkus is fine and keeps responsibilities clear.
The key is to track the MCP spec’s progress and design our integration accordingly. Red Hat is likely involved in discussions around MCP (to ensure OpenShift and tools like Open Data Hub can interact with agents). We will keep the AMCP team in sync with any reference implementations. Possibly by the time v1.6 is out, there will be early MCP libraries to leverage.
2.5 Security Architecture Enhancements
Current State: Enterprise security is mentioned as a feature of AMCP (the separate AMP project explicitly lists authentication, authorization, message signing ). However, open-source v1.5’s documentation doesn’t detail how security is implemented. It might rely on security of the transport (e.g., Kafka with SASL/SSL, or Solace with its own auth). A2A protocol has some fields for identity but not a full scheme yet. MCP similarly might rely on underlying auth (like OAuth for tool endpoints). So currently, security is not standardized across agent communications.
Challenges: In a distributed agent system, we need to address:
Authenticating agents: How does Agent A know that a message truly came from Agent B (and not a malicious impersonator)? And is Agent B authorized to talk to Agent A or invoke a certain action? In microservice land, this is often done via service identities and tokens.
Securing channels: The event broker might be secure, but bridging protocols (A2A over HTTP) could be susceptible to MITM or unauthorized access if not protected.
Human/Tool access control: If an external user or service triggers an agent, how do we propagate their identity or limit what the agent can do on their behalf?
Proposal: Implement a multi-layered security model in AMCP v1.6:
Transport-Level Security: Continue to use the security features of the chosen broker: e.g., Kafka with SSL encryption and SASL for authentication (username/password or Kerberos), NATS with NKeys or creds, Solace with client username/vpn credentials. The Quarkus extension can read credentials from config/Secrets and pass them to the AMCP broker connections. This ensures that only authorized applications join the mesh. (In Kubernetes, one might even isolate a mesh in a namespace with a specific broker instance.)
Agent Identity and Tokens: Introduce an identity for each agent or context. For instance, when registering an agent in the mesh, it could have a signed JWT representing its identity and roles. We can integrate with Keycloak (Red Hat SSO) or any OpenID Connect provider to issue these tokens. The token could include claims like agentName, agentType, permissions (e.g., “weather:read”).
For internal events within the broker, we might not attach a JWT to every message (that would be overhead), but we can establish trust at connection time. For example, an AgentContext connecting to Kafka could use a principal that maps to an agent identity. If using mTLS, the client cert’s common name could identify the context. Then the agents inside are implicitly trusted as that identity. Alternatively, within the event payload, we could include an optional metadata field with an auth token or signature for critical messages.
For A2A HTTP calls, we definitely include auth – e.g., using OAuth2 access tokens. Red Hat’s security architects might recommend an approach where each agent or calling service obtains a token from a central authority (or uses mutual TLS with known certs) for A2A communication. The Quarkus extension can integrate this easily: Quarkus has built-in OIDC token verification, so the A2A endpoint can require a valid token. We could leverage Quarkus security to map tokens to agent principals, and then enforce authorization rules (like an agent with role “planner” can forward tasks to a “payment” agent, but not vice versa, etc.).
Authorization & Policy: Build on the identity foundation to implement rules. Possibly use RBAC: e.g., define roles for agents – “admin” agents that can perform system tasks vs “guest” agents that are untrusted. If an untrusted agent attempts to dispatch itself to another context or call a sensitive tool, the system could block it unless permitted. These rules could be configured in a policy file or using an existing framework (in Quarkus, we could use MicroProfile JWT RBAC for simple cases, or even integrate OPA for complex policies).
In practice, a simple but effective check is to have each event or action labeled with a required role and have each agent identity contain roles. For example, an event “database.write” might require the agent has the “DB\_WRITE” role. If not, AMCP can refuse to deliver that event or the target agent can ignore it. Implementing this might involve hooking into the event dispatch mechanism to do a security filter.
Message Signing and Encryption: To ensure integrity (and confidentiality, if needed) of messages at the application level, AMCP can support optional signing of events. For instance, include a digital signature in the event metadata that the receiver can verify. If all agents trust a common CA or have each other’s public keys, this prevents tampering. Given that we usually trust the broker and transport encryption, this might be overkill except in open environments. But since it was mentioned in goals , it’s worth planning. In a Quarkus context, we could use the MicroProfile JWT (which is a signed token) as a vehicle: e.g., encode the event payload or a hash of it in a JWT that the sender signs. However, this can be heavy and might reduce throughput. Perhaps focus on signing critical cross-boundary messages (like A2A or API outputs) rather than every internal event.
Audit Logging: When agents take actions (especially those triggered by external requests or affecting external systems), log them in an audit trail with identities. Quarkus’s logging can be leveraged, or events can be sent to a security audit topic.
Leveraging Red Hat Security Expertise: Red Hat’s security architects can assist in designing this such that it integrates with enterprise identity systems. For example, in OpenShift, each serviceaccount can have a JWT token – we might map an agent context to a serviceaccount identity. Or use Red Hat SSO to issue service tokens for agents. The goal is that an administrator can control agent permissions centrally (revoke a token to disable an agent’s actions, etc.). This becomes critical when agents are autonomous – you want a “kill-switch” or at least a leash if they misbehave or are compromised.
Since A2A and MCP are new, we should also feed these security requirements back to their spec committees:
A2A might incorporate an optional auth token in the protocol metadata (similar to how HTTP has Authorization header; we can simply use that).
MCP tool APIs likely rely on whatever auth the tool service uses (e.g., if calling a cloud API, you provide an API key or OAuth token). Our framework should allow agents to be configured with the necessary credentials for the tools they use, without hardcoding in code. Quarkus already has a vault integration for managing secrets, which we can leverage to supply API keys to agents without exposing them in code.
Roadmap: Introduce these security features gradually:
v1.6: Basic authentication — e.g., support JWT validation on A2A messages, a simple allow/deny configuration for agent migrations (maybe require certain role to use agent.dispatch() to a protected context), and integration with OAuth2 for external API calls by agents.
v1.7+: More fine-grained authorization and possibly an identity service for agents.
By v2.0: A comprehensive security guide and tools, as hinted in the roadmap (OAuth2.0, RBAC by v1.1 in AMP’s plan , and “Enterprise governance features” by v2.0 in that roadmap ). We can align AMCP’s timeline with those goals, given the overlap in vision.
2.6 Other Considerations and Comparisons
A few additional notes to round out the proposal:
Maintaining Performance: All these integrations (Quarkus, A2A, security checks) should be designed to keep AMCP’s high performance. Quarkus build-time init actually helps here – by the time the system is running, everything is pre-wired, so adding an auth check (one JWT validation) won’t impact the 25k events/sec throughput for internal events because those can remain local to Kafka (no per-message JWT needed internally if we trust the connection). The table in the AMCP docs shows Kafka throughput 25k+/sec and P99 5ms ; we’ll strive to maintain that. With Quarkus native compilation, if we choose to compile the whole app, agent startup times could be even smaller on low-power environments, although the current Java results (<200ms startup, <50ms migration) are already excellent .
Integration with LangChain4j and AI libraries: The extension and AMCP alignment will allow us to embed LLM-powered agents effectively (as the previous report detailed). One noteworthy point: The AMCP open source includes integration with Ollama (for local LLMs) and has an example “LLM-powered Chat Agent” . It even references “Spring AI in AMCP 1.5 Chat Agent” in the repo . This indicates the project has already experimented with integrating Spring’s AI tools. With Quarkus, we can do similarly using LangChain4j. By aligning, we ensure AMCP can work with any AI provider. No specific action needed here beyond what’s done – just ensure our extension doesn’t break or limit those integrations (it shouldn’t; the agent code for calling an LLM is at application level).
Community and Governance: As part of aligning with Red Hat, we might suggest that AMCP join a foundation (like the CNCF or similar) in the future. This can help with broader adoption and contributions. If so, Apache 2 licensing is a must, which aligns with our suggestion. This is a longer-term consideration, but relevant if we want AMCP to become the de facto enterprise agent framework.
Comparison to other frameworks: The combined Quarkus+AMCP offering will be quite unique. Competing approaches (like the earlier Google AMP project we saw, or Microsoft’s Autogen, etc.) either don’t focus on mobility or don’t integrate with enterprise platforms as deeply. By tackling the issues above, we ensure:
Ease of Use: through Quarkus extension (versus manual integration required for others).
Interoperability: through A2A/MCP (versus closed ecosystems).
Security & Governance: through Red Hat’s input (an area lacking in most open agent frameworks so far).
License and Openness: by staying permissive and open (some alternatives might lock you into a cloud service, whereas this stack can run fully on-prem or at the edge).
All these are strong selling points for enterprise adoption which Red Hat can communicate: e.g., “Our AMCP + Quarkus stack is open, secure, and scalable, unlike experimental Python scripts that are hard to trust at scale.”


Conclusion and Next Steps
By implementing the Quarkus AMCP extension and making the above enhancements in AMCP v1.6, we create a robust, enterprise-friendly agentic AI framework:
Technical Validation: The HelloWorldAgent multi-instance test confirms that the Quarkus extension successfully orchestrates a distributed AMCP agent mesh over Kafka (or other brokers) with minimal config. This paves the way to run more complex agent scenarios (like the travel planner PoC) on a reliable platform.
Strategic Alignment: Addressing packaging, licensing, and standard protocols ensures that AMCP can be adopted by companies without legal hurdles and can interoperate in the growing multi-agent ecosystem. Emphasizing security integration makes it suitable for mission-critical deployments where compliance and control matter.
Immediate Next Steps:
Prototype the Extension: Implement the skeleton as described, using AMCP v1.5. Test with a simple agent. Iterate on any issues (e.g., classloading, performance). This will also give feedback to AMCP: maybe we discover we need a public API to register an agent programmatically (if not already there).
Contribute to AMCP v1.6 Planning: Work with the AMCP maintainers (now on GitHub) to prioritize the changes:
Break out modules and publish artifacts (this can be done quickly if not already).
Remove or replace any incompatible dependencies.
Incorporate the A2A SDK (coordinate with Red Hat’s A2A team).
Draft a design for security (perhaps start with OAuth2 support for the A2A gateway, since that’s a discrete piece).
Begin implementing MCP support once the spec is firm (maybe target a tech preview in v1.6).
These contributions not only improve AMCP but also tailor it for Quarkus’s needs.
Licensing Review: Initiate a legal review through Red Hat’s open source program office for AMCP. If MIT is fine, great. If Apache 2 is preferable, prepare a contributor license agreement update (the repo already has an ICLA file  which suggests they manage contributions). Community consensus would be needed for any license change, but since it’s early and has few contributors, it’s feasible.
Security Design Workshop: Set up a short design session between AMCP devs and Red Hat security architects. Outcome: a spec or blueprint for agent authentication. For example, decide on using OIDC tokens with CloudEvents extension fields for identity, and requiring all cross-context communication to present a valid token. This can then be implemented in v1.6 timeframe.
Documentation & Demo: Update documentation to include Quarkus usage (perhaps an official Guide like “Run AMCP on Quarkus”). And build a demo (maybe adapting the existing orchestrator demo  to run as two Quarkus services working together). A polished demo will help gain buy-in from stakeholders and the community.
By following this implementation plan, we will deliver a powerful proof-of-concept of Quarkus and AMCP in action, and set the stage for a production-ready solution. The synergy between Quarkus’s cloud-native strengths and AMCP’s advanced multi-agent capabilities is key to making agentic AI viable at large scale. Red Hat can then confidently promote this stack as part of its AI strategy, offering customers an open, extensible, and secure way to deploy AI agents that cooperate to solve complex tasks – all while running on the dependable foundations of Java and Kubernetes.
