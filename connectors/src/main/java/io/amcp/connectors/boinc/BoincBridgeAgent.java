package io.amcp.connectors.boinc;

import io.amcp.core.Agent;
import io.amcp.core.AgentContext;
import io.amcp.core.AgentID;
import io.amcp.core.AgentLifecycle;
import io.amcp.core.Event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AMCP agent that bridges a local BOINC client to the agent mesh so volunteer
 * compute capacity can be observed and steered by other agents.
 *
 * <h3>Published topics</h3>
 * <ul>
 *   <li>{@code boinc.status} – periodic snapshot: projects, tasks, cc_status</li>
 *   <li>{@code boinc.reply} – answer to a command (carries the request's correlationId)</li>
 *   <li>{@code boinc.error} – connection or command failure</li>
 * </ul>
 *
 * <h3>Command topics (subscribed)</h3>
 * <ul>
 *   <li>{@code boinc.command.status} – publish a snapshot now</li>
 *   <li>{@code boinc.command.runmode} – payload {@code {mode: always|auto|never, duration: seconds}}</li>
 *   <li>{@code boinc.command.project} – payload {@code {url, op: suspend|resume|update|nomorework|allowmorework}}</li>
 * </ul>
 *
 * <p>Mutating commands are refused unless the agent was constructed with
 * {@code allowControl=true}; the default is read-only.</p>
 */
public class BoincBridgeAgent implements Agent {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(BoincBridgeAgent.class.getName());

    public static final String TOPIC_STATUS = "boinc.status";
    public static final String TOPIC_REPLY = "boinc.reply";
    public static final String TOPIC_ERROR = "boinc.error";

    private final AgentID agentId = AgentID.named("BoincBridgeAgent");
    private final transient Supplier<BoincRpcClient> clientFactory;
    private final long pollIntervalSeconds;
    private final boolean allowControl;

    private transient AgentContext context;
    private transient BoincRpcClient client;
    private transient ScheduledExecutorService scheduler;
    private transient ScheduledFuture<?> pollTask;
    private volatile AgentLifecycle lifecycle = AgentLifecycle.INACTIVE;

    public BoincBridgeAgent(String host, int port, String password,
                            long pollIntervalSeconds, boolean allowControl) {
        this(() -> new BoincRpcClient(host, port, password, 10_000), pollIntervalSeconds, allowControl);
    }

    /** Visible for tests and custom transports. */
    public BoincBridgeAgent(Supplier<BoincRpcClient> clientFactory,
                            long pollIntervalSeconds, boolean allowControl) {
        this.clientFactory = clientFactory;
        this.pollIntervalSeconds = pollIntervalSeconds;
        this.allowControl = allowControl;
    }

    public void setContext(AgentContext context) {
        this.context = context;
    }

    @Override public AgentID getAgentId() { return agentId; }
    @Override public AgentContext getContext() { return context; }
    @Override public AgentLifecycle getLifecycleState() { return lifecycle; }

    @Override
    public void onActivate() {
        lifecycle = AgentLifecycle.ACTIVE;
        client = clientFactory.get();
        subscribe("boinc.command.*");
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "boinc-bridge-poll");
            t.setDaemon(true);
            return t;
        });
        if (pollIntervalSeconds > 0) {
            pollTask = scheduler.scheduleWithFixedDelay(this::publishSnapshotSafely,
                    0, pollIntervalSeconds, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onDeactivate() {
        lifecycle = AgentLifecycle.INACTIVE;
        if (pollTask != null) {
            pollTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (client != null) {
            client.close();
        }
    }

    @Override public void onDestroy() { onDeactivate(); }
    @Override public void onBeforeMigration(String destinationContext) { onDeactivate(); }
    @Override public void onAfterMigration(String sourceContext) { /* re-activated by the context */ }

    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            String topic = event.getTopic();
            try {
                switch (topic) {
                    case "boinc.command.status" -> reply(event, snapshot());
                    case "boinc.command.runmode" -> {
                        requireControl();
                        Map<String, Object> p = payload(event);
                        String mode = String.valueOf(p.get("mode"));
                        double dur = p.get("duration") instanceof Number n ? n.doubleValue() : 0;
                        synchronized (this) { client.setRunMode(mode, dur); }
                        reply(event, Map.of("ok", true, "mode", mode));
                    }
                    case "boinc.command.project" -> {
                        requireControl();
                        Map<String, Object> p = payload(event);
                        String url = String.valueOf(p.get("url"));
                        String op = String.valueOf(p.get("op"));
                        synchronized (this) { client.projectOp(url, op); }
                        reply(event, Map.of("ok", true, "url", url, "op", op));
                    }
                    default -> LOG.fine("Ignoring unknown BOINC topic " + topic);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "BOINC command failed: " + topic, e);
                publish(TOPIC_ERROR, Map.of("topic", topic, "error", String.valueOf(e.getMessage())),
                        event.getCorrelationId());
            }
        });
    }

    // ---------------------------------------------------------------- internals

    /** Builds a snapshot of the BOINC client state. Visible for tests. */
    synchronized Map<String, Object> snapshot() throws java.io.IOException {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("projects", client.getProjectStatus());
        s.put("tasks", client.getResults());
        s.put("ccStatus", client.getCcStatus());
        s.put("timestamp", System.currentTimeMillis());
        return s;
    }

    private void publishSnapshotSafely() {
        try {
            publish(TOPIC_STATUS, snapshot(), null);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "BOINC poll failed", e);
            publish(TOPIC_ERROR, Map.of("topic", "poll", "error", String.valueOf(e.getMessage())), null);
        }
    }

    private void requireControl() {
        if (!allowControl) {
            throw new SecurityException("BOINC control commands are disabled (read-only bridge)");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> payload(Event event) {
        Object p = event.getPayload();
        if (p instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        throw new IllegalArgumentException("Command payload must be a map");
    }

    private void reply(Event request, Object body) {
        publish(TOPIC_REPLY, body, request.getCorrelationId());
    }

    private void publish(String topic, Object body, String correlationId) {
        if (context == null) {
            return;
        }
        Event.Builder b = Event.builder().topic(topic).payload(body).sender(agentId);
        if (correlationId != null) {
            b.correlationId(correlationId);
        }
        publishEvent(b.build());
    }

    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        return context == null ? CompletableFuture.completedFuture(null) : context.publishEvent(event);
    }

    @Override
    public CompletableFuture<Void> subscribe(String topicPattern) {
        return context == null ? CompletableFuture.completedFuture(null)
                : context.subscribe(agentId, topicPattern);
    }

    @Override
    public CompletableFuture<Void> unsubscribe(String topicPattern) {
        return context == null ? CompletableFuture.completedFuture(null)
                : context.unsubscribe(agentId, topicPattern);
    }
}
