# 🌐 Phase 2: A2A Gateway Prototype (HTTP Bridge)

**Timeline**: Week 2-3  
**Priority**: HIGH  
**Status**: 📋 Ready for Implementation

---

## Overview

Enable AMCP agents to communicate with external A2A-compliant agents via HTTP.

---

## 2.1 A2A Message Model

**File**: `amcp-a2a-bridge/src/main/java/io/amcp/a2a/A2AMessage.java`

```java
package io.amcp.a2a;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

public class A2AMessage {
    
    @JsonProperty("performative")
    private String performative;  // REQUEST, INFORM, REPLY, FAILURE
    
    @JsonProperty("sender")
    private String sender;  // Agent URI
    
    @JsonProperty("receiver")
    private String receiver;  // Agent URI
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    @JsonProperty("in_reply_to")
    private String inReplyTo;
    
    @JsonProperty("content")
    private Map<String, Object> content;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    // Getters/Setters
    public String getPerformative() { return performative; }
    public void setPerformative(String performative) { this.performative = performative; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    
    public String getInReplyTo() { return inReplyTo; }
    public void setInReplyTo(String inReplyTo) { this.inReplyTo = inReplyTo; }
    
    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
```

---

## 2.2 A2A Gateway Agent

**File**: `amcp-examples/src/main/java/io/amcp/examples/A2AGatewayAgent.java`

```java
package io.amcp.examples;

import io.amcp.core.AbstractMobileAgent;
import io.amcp.core.Event;
import io.amcp.a2a.A2AMessage;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class A2AGatewayAgent extends AbstractMobileAgent {
    
    @Override
    public void onActivate() {
        super.onActivate();
        subscribe("a2a.outbound.**");  // AMCP → A2A
        logMessage("🌉 A2A Gateway Agent activated");
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                A2AMessage a2aMsg = translateToA2A(event);
                logMessage("📤 Translating AMCP event to A2A: " + a2aMsg.getPerformative());
                // Send via HTTP to external agent
            } catch (Exception e) {
                logMessage("❌ Error: " + e.getMessage());
            }
        });
    }
    
    public A2AMessage translateToA2A(Event event) {
        A2AMessage msg = new A2AMessage();
        msg.setPerformative("REQUEST");
        msg.setSender("amcp://gateway");
        msg.setConversationId(UUID.randomUUID().toString());
        msg.setTimestamp(Instant.now());
        
        Map<String, Object> content = new HashMap<>();
        content.put("topic", event.getTopic());
        content.put("payload", event.getPayload(Map.class));
        msg.setContent(content);
        
        return msg;
    }
    
    public Event translateFromA2A(A2AMessage a2aMsg) {
        String topic = "a2a." + a2aMsg.getPerformative().toLowerCase();
        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", a2aMsg.getSender());
        payload.put("conversation_id", a2aMsg.getConversationId());
        payload.put("content", a2aMsg.getContent());
        
        return Event.create(topic, payload);
    }
}
```

---

## 2.3 A2A REST Endpoint

**File**: `amcp-examples/src/main/java/io/amcp/examples/A2AEndpoint.java`

```java
package io.amcp.examples;

import io.amcp.a2a.A2AMessage;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;

@Path("/a2a")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class A2AEndpoint {
    
    @Inject
    A2AGatewayAgent gatewayAgent;
    
    @POST
    @Path("/message")
    public Response sendMessage(A2AMessage message) {
        try {
            Event event = gatewayAgent.translateFromA2A(message);
            
            A2AMessage response = new A2AMessage();
            response.setPerformative("INFORM");
            response.setSender("a2a://gateway");
            response.setReceiver(message.getSender());
            response.setInReplyTo(message.getConversationId());
            response.setContent(Map.of("status", "received"));
            
            return Response.ok(response).build();
        } catch (Exception e) {
            A2AMessage error = new A2AMessage();
            error.setPerformative("FAILURE");
            error.setContent(Map.of("error", e.getMessage()));
            return Response.status(400).entity(error).build();
        }
    }
    
    @GET
    @Path("/agents")
    public Response listAgents() {
        List<Map<String, Object>> agents = new ArrayList<>();
        agents.add(Map.of(
            "name", "weather-agent",
            "uri", "a2a://amcp/weather",
            "description", "Provides weather information"
        ));
        return Response.ok(agents).build();
    }
}
```

---

## 2.4 Integration Test

```bash
#!/bin/bash

echo "Test 1: List agents"
curl -X GET http://localhost:8080/a2a/agents | jq .

echo "Test 2: Send A2A message"
curl -X POST http://localhost:8080/a2a/message \
  -H "Content-Type: application/json" \
  -d '{
    "performative": "REQUEST",
    "sender": "external-agent",
    "receiver": "amcp://weather",
    "conversation_id": "conv-123",
    "content": {"action": "get_weather", "city": "Paris"}
  }' | jq .
```

---

## Success Criteria

- ✅ Receives A2A messages via HTTP
- ✅ Translates to/from AMCP events
- ✅ Lists available agents
- ✅ Handles errors gracefully
