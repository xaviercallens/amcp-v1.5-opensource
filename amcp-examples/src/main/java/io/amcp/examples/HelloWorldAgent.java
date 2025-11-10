package io.amcp.examples;

import io.amcp.core.AbstractMobileAgent;
import io.amcp.core.Event;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.CompletableFuture;

/**
 * HelloWorld agent demonstrating basic AMCP functionality in Quarkus.
 * This agent listens for hello.request events and responds with hello.response events.
 */
@ApplicationScoped
public class HelloWorldAgent extends AbstractMobileAgent {

    @Override
    public void onActivate() {
        super.onActivate();
        subscribe("hello.**");
        logMessage("🌍 HelloWorld Agent is alive!");
    }

    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Handling event: {}", event.getTopic());
            
            if (event.getTopic().equals("hello.request")) {
                String name = event.getPayload(String.class);
                logMessage("📨 Received hello request from: " + name);
                
                // Respond to the sender
                String response = "Hello, " + name + "! Welcome to AMCP v1.6 on Quarkus! 🚀";
                publishEvent("hello.response", response);
                logMessage("✅ Sent hello response");
            } else if (event.getTopic().equals("hello.ping")) {
                logMessage("🏓 Received ping, sending pong");
                publishEvent("hello.pong", "pong");
            }
        });
    }

    @Override
    public void onDeactivate() {
        logMessage("👋 HelloWorld Agent shutting down");
        super.onDeactivate();
    }
}
