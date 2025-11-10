package io.amcp.examples;

import io.amcp.core.AbstractMobileAgent;
import io.amcp.core.Event;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * OrchestratorAgent - v1.6 Quarkus Integration with Kafka Broker
 * 
 * Coordinates agent workflows and orchestrates distributed tasks.
 * Features:
 * - CloudEvents v1.0 compliance
 * - Async workflow coordination
 * - Multi-instance Kafka coordination
 * - Task scheduling and monitoring
 * - Quarkus CDI integration
 */
@ApplicationScoped
public class OrchestratorAgent extends AbstractMobileAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorAgent.class);
    
    @Override
    public void onActivate() {
        super.onActivate();
        subscribe("orchestrator.**");
        subscribe("orchestrator.workflow");
        subscribe("orchestrator.task");
        logMessage("🎼 OrchestratorAgent activated - v1.6 Quarkus + Kafka ready");
    }

    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logger.debug("OrchestratorAgent handling event: {}", topic);
                
                if (topic.equals("orchestrator.workflow")) {
                    handleWorkflow(event);
                } else if (topic.equals("orchestrator.task")) {
                    handleTask(event);
                } else if (topic.equals("orchestrator.status")) {
                    handleStatusRequest(event);
                }
            } catch (Exception e) {
                logger.error("Error handling orchestrator event: {}", e.getMessage(), e);
            }
        });
    }

    private void handleWorkflow(Event event) {
        try {
            Map<String, Object> workflow = event.getPayload(Map.class);
            String workflowId = (String) workflow.getOrDefault("workflowId", "workflow-" + System.nanoTime());
            String workflowType = (String) workflow.getOrDefault("type", "unknown");
            
            logMessage("🔄 Workflow started: " + workflowId + " (type: " + workflowType + ")");
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflowId", workflowId);
            response.put("type", workflowType);
            response.put("status", "started");
            response.put("timestamp", System.currentTimeMillis());
            response.put("tasks", 0);
            
            publishEvent("orchestrator.workflow.started", response);
            logMessage("✅ Workflow " + workflowId + " started successfully");
            
        } catch (Exception e) {
            logger.error("Error handling workflow: {}", e.getMessage(), e);
        }
    }

    private void handleTask(Event event) {
        try {
            Map<String, Object> task = event.getPayload(Map.class);
            String taskId = (String) task.getOrDefault("taskId", "task-" + System.nanoTime());
            String taskType = (String) task.getOrDefault("type", "unknown");
            String workflowId = (String) task.getOrDefault("workflowId", "unknown");
            
            logMessage("📋 Task scheduled: " + taskId + " (workflow: " + workflowId + ")");
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("workflowId", workflowId);
            response.put("type", taskType);
            response.put("status", "scheduled");
            response.put("timestamp", System.currentTimeMillis());
            
            publishEvent("orchestrator.task.scheduled", response);
            logMessage("✅ Task " + taskId + " scheduled for workflow " + workflowId);
            
        } catch (Exception e) {
            logger.error("Error handling task: {}", e.getMessage(), e);
        }
    }

    private void handleStatusRequest(Event event) {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("agent", "OrchestratorAgent");
            status.put("version", "1.6.0");
            status.put("status", "active");
            status.put("timestamp", System.currentTimeMillis());
            status.put("broker", "kafka");
            status.put("features", new String[]{"workflow_orchestration", "task_scheduling", "distributed_coordination"});
            
            publishEvent("orchestrator.status.response", status);
            logMessage("✅ OrchestratorAgent status reported");
            
        } catch (Exception e) {
            logger.error("Error handling status request: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onDeactivate() {
        logMessage("👋 OrchestratorAgent shutting down");
        super.onDeactivate();
    }
}
