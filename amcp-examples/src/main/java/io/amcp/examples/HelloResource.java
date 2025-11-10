package io.amcp.examples;

import io.amcp.core.AgentContext;
import io.amcp.core.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * REST endpoint for interacting with the HelloWorld agent.
 */
@Path("/hello")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HelloResource {

    @Inject
    AgentContext agentContext;

    @GET
    @Path("/status")
    public Map<String, Object> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("contextId", agentContext.getContextId());
        status.put("agentCount", agentContext.getAgents().size());
        status.put("running", agentContext.isStarted());
        status.put("brokerType", agentContext.getEventBroker().getBrokerType());
        
        Map<String, String> agents = new HashMap<>();
        agentContext.getAgents().forEach((id, agent) -> 
            agents.put(id, agent.getAgentClassName() + " [" + agent.getState() + "]")
        );
        status.put("agents", agents);
        
        return status;
    }

    @POST
    @Path("/send")
    public Map<String, String> sendHello(Map<String, String> request) throws Exception {
        String name = request.getOrDefault("name", "World");
        
        // Subscribe to response
        AtomicReference<String> response = new AtomicReference<>();
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        
        String subId = agentContext.getEventBroker().subscribe("hello.response", event -> {
            response.set(event.getPayload(String.class));
            responseFuture.complete(response.get());
        });
        
        try {
            // Send hello.request event
            Event requestEvent = Event.create("hello.request", "https://amcp.dev/rest-api", name);
            agentContext.getEventBroker().publish(requestEvent).join();
            
            // Wait for response (with timeout)
            String result = responseFuture.get(5, TimeUnit.SECONDS);
            
            return Map.of(
                "request", name,
                "response", result,
                "status", "success"
            );
        } catch (Exception e) {
            return Map.of(
                "request", name,
                "error", e.getMessage(),
                "status", "error"
            );
        } finally {
            agentContext.getEventBroker().unsubscribe(subId);
        }
    }

    @POST
    @Path("/ping")
    public Map<String, String> ping() throws Exception {
        // Subscribe to pong response
        CompletableFuture<String> pongFuture = new CompletableFuture<>();
        
        String subId = agentContext.getEventBroker().subscribe("hello.pong", event -> {
            pongFuture.complete(event.getPayload(String.class));
        });
        
        try {
            // Send ping event
            Event pingEvent = Event.create("hello.ping", "https://amcp.dev/rest-api", "ping");
            agentContext.getEventBroker().publish(pingEvent).join();
            
            // Wait for pong
            String pong = pongFuture.get(5, TimeUnit.SECONDS);
            
            return Map.of(
                "ping", "sent",
                "pong", pong,
                "status", "success"
            );
        } catch (Exception e) {
            return Map.of(
                "error", e.getMessage(),
                "status", "error"
            );
        } finally {
            agentContext.getEventBroker().unsubscribe(subId);
        }
    }
}
