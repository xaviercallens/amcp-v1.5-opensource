# AMCP Strong Mobility - Technical Specification
## Detailed Implementation Guide Based on Aglets and ATP Concepts

---

## Table of Contents

1. [Core Interfaces and Classes](#1-core-interfaces-and-classes)
2. [Execution State Capture](#2-execution-state-capture)
3. [Continuation Framework](#3-continuation-framework)
4. [ATP Transport Protocol](#4-atp-transport-protocol)
5. [Bytecode Instrumentation](#5-bytecode-instrumentation)
6. [Security Model](#6-security-model)
7. [Usage Examples](#7-usage-examples)

---

## 1. Core Interfaces and Classes

### 1.1 StrongMobilityAgent Interface

```java
package io.amcp.core.mobility;

import io.amcp.core.Agent;
import io.amcp.core.AgentID;
import io.amcp.mobility.*;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced Agent interface supporting strong mobility
 * Inspired by IBM Aglets dispatch() pattern
 * 
 * Backward compatible - existing Agent implementations work unchanged
 */
public interface StrongMobilityAgent extends Agent {
    
    /**
     * Self-dispatch to remote context (Aglets-style)
     * Preserves complete execution state across migration
     * 
     * Example:
     * <pre>
     * String data = fetchData();
     * dispatch("atp://remote-host:4434/context").join();
     * // Continues here with 'data' variable intact
     * processData(data);
     * </pre>
     * 
     * @param destinationURI ATP URI of destination context
     * @return CompletableFuture completing when agent resumes at destination
     */
    default CompletableFuture<Void> dispatch(String destinationURI) {
        return getContext().getMobilityManager()
            .dispatchAgent(getAgentId(), destinationURI, 
                          MigrationOptions.strongMobility());
    }
    
    /**
     * Clone this agent with complete execution state
     * Original continues, clone runs independently at destination
     * 
     * @param destinationURI ATP URI for clone
     * @return CompletableFuture with cloned agent ID
     */
    default CompletableFuture<AgentID> clone(String destinationURI) {
        return getContext().getMobilityManager()
            .cloneAgent(getAgentId(), destinationURI,
                       MigrationOptions.withCloning());
    }
    
    /**
     * Retract (migrate back) from remote context to origin
     * Aglets-style bidirectional mobility
     * 
     * @return CompletableFuture completing when back at origin
     */
    default CompletableFuture<Void> retract() {
        String originContext = getContext().getMetadata("origin.context");
        if (originContext == null) {
            throw new IllegalStateException("No origin context to retract to");
        }
        return dispatch(originContext);
    }
    
    /**
     * Indicates if this agent implementation supports strong mobility
     * Override to return true for strong mobility agents
     * 
     * @return true if agent supports mid-execution migration
     */
    default boolean supportsStrongMobility() {
        return false; // Safe default for backward compatibility
    }
    
    /**
     * Hook called after successful dispatch/migration
     * Override to add custom post-migration logic
     * 
     * @param previousContext context agent migrated from
     */
    default void onMigrationComplete(String previousContext) {
        // Default: no-op
    }
}
```

### 1.2 ExecutionState Class

```java
package io.amcp.mobility;

import java.io.Serializable;
import java.util.*;

/**
 * Complete execution state for strong mobility
 * Captures everything needed to resume agent execution mid-stream
 */
public final class ExecutionState implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Core execution state
    private final List<StackFrame> callStack;
    private final Map<Integer, LocalVariableTable> localVariables;
    private final HeapSnapshot heapSnapshot;
    private final ProgramCounter programCounter;
    private final ThreadState threadState;
    
    // Continuation information
    private final ContinuationPoint continuationPoint;
    
    // Metadata
    private final long captureTimestamp;
    private final String captureContext;
    private final String agentId;
    private final int stateVersion;
    
    /**
     * Stack frame representing one method invocation
     */
    public static class StackFrame implements Serializable {
        private final String className;
        private final String methodName;
        private final String methodDescriptor;
        private final int lineNumber;
        private final int bytecodeOffset;
        private final List<Object> operandStack;
        private final Map<String, Object> localVars;
        
        public StackFrame(String className, String methodName, 
                         String methodDescriptor, int lineNumber,
                         int bytecodeOffset, List<Object> operandStack,
                         Map<String, Object> localVars) {
            this.className = className;
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
            this.lineNumber = lineNumber;
            this.bytecodeOffset = bytecodeOffset;
            this.operandStack = new ArrayList<>(operandStack);
            this.localVars = new HashMap<>(localVars);
        }
        
        // Getters
        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public String getMethodDescriptor() { return methodDescriptor; }
        public int getLineNumber() { return lineNumber; }
        public int getBytecodeOffset() { return bytecodeOffset; }
        public List<Object> getOperandStack() { return operandStack; }
        public Map<String, Object> getLocalVars() { return localVars; }
    }
    
    /**
     * Local variable table for a stack frame
     */
    public static class LocalVariableTable implements Serializable {
        private final Map<String, Object> variables;
        private final Map<String, String> types;
        
        public LocalVariableTable(Map<String, Object> variables, 
                                 Map<String, String> types) {
            this.variables = new HashMap<>(variables);
            this.types = new HashMap<>(types);
        }
        
        public Object getVariable(String name) {
            return variables.get(name);
        }
        
        public String getType(String name) {
            return types.get(name);
        }
        
        public Set<String> getVariableNames() {
            return variables.keySet();
        }
    }
    
    /**
     * Program counter - next instruction to execute
     */
    public static class ProgramCounter implements Serializable {
        private final String currentMethod;
        private final int instructionIndex;
        private final int bytecodePosition;
        
        public ProgramCounter(String currentMethod, int instructionIndex,
                             int bytecodePosition) {
            this.currentMethod = currentMethod;
            this.instructionIndex = instructionIndex;
            this.bytecodePosition = bytecodePosition;
        }
        
        public static ProgramCounter initial() {
            return new ProgramCounter("run", 0, 0);
        }
        
        // Getters
        public String getCurrentMethod() { return currentMethod; }
        public int getInstructionIndex() { return instructionIndex; }
        public int getBytecodePosition() { return bytecodePosition; }
    }
    
    /**
     * Heap snapshot - object graph state
     */
    public static class HeapSnapshot implements Serializable {
        private final Map<Integer, ObjectState> objectGraph;
        private final List<Integer> rootObjectIds;
        
        public static class ObjectState implements Serializable {
            private final String className;
            private final Map<String, Object> fieldValues;
            private final int objectId;
            
            public ObjectState(int objectId, String className,
                             Map<String, Object> fieldValues) {
                this.objectId = objectId;
                this.className = className;
                this.fieldValues = new HashMap<>(fieldValues);
            }
            
            public String getClassName() { return className; }
            public Map<String, Object> getFieldValues() { return fieldValues; }
            public int getObjectId() { return objectId; }
        }
        
        public HeapSnapshot(Map<Integer, ObjectState> objectGraph,
                          List<Integer> rootObjectIds) {
            this.objectGraph = new HashMap<>(objectGraph);
            this.rootObjectIds = new ArrayList<>(rootObjectIds);
        }
        
        public static HeapSnapshot empty() {
            return new HeapSnapshot(Collections.emptyMap(), Collections.emptyList());
        }
        
        public ObjectState getObject(int objectId) {
            return objectGraph.get(objectId);
        }
        
        public List<Integer> getRootObjectIds() {
            return rootObjectIds;
        }
    }
    
    /**
     * Thread state - locks, monitors, etc.
     */
    public static class ThreadState implements Serializable {
        private final Set<String> heldLocks;
        private final Map<String, Integer> monitorCounts;
        private final Thread.State state;
        
        public ThreadState(Set<String> heldLocks, 
                         Map<String, Integer> monitorCounts,
                         Thread.State state) {
            this.heldLocks = new HashSet<>(heldLocks);
            this.monitorCounts = new HashMap<>(monitorCounts);
            this.state = state;
        }
        
        public static ThreadState clean() {
            return new ThreadState(
                Collections.emptySet(),
                Collections.emptyMap(),
                Thread.State.RUNNABLE
            );
        }
        
        // Getters
        public Set<String> getHeldLocks() { return heldLocks; }
        public Map<String, Integer> getMonitorCounts() { return monitorCounts; }
        public Thread.State getState() { return state; }
    }
    
    // Constructor
    public ExecutionState(List<StackFrame> callStack,
                         Map<Integer, LocalVariableTable> localVariables,
                         HeapSnapshot heapSnapshot,
                         ProgramCounter programCounter,
                         ThreadState threadState,
                         ContinuationPoint continuationPoint,
                         long captureTimestamp,
                         String captureContext,
                         String agentId,
                         int stateVersion) {
        this.callStack = new ArrayList<>(callStack);
        this.localVariables = new HashMap<>(localVariables);
        this.heapSnapshot = heapSnapshot;
        this.programCounter = programCounter;
        this.threadState = threadState;
        this.continuationPoint = continuationPoint;
        this.captureTimestamp = captureTimestamp;
        this.captureContext = captureContext;
        this.agentId = agentId;
        this.stateVersion = stateVersion;
    }
    
    /**
     * Create empty state for weak mobility fallback
     */
    public static ExecutionState empty(String agentId) {
        return new ExecutionState(
            Collections.emptyList(),
            Collections.emptyMap(),
            HeapSnapshot.empty(),
            ProgramCounter.initial(),
            ThreadState.clean(),
            ContinuationPoint.restart(),
            System.currentTimeMillis(),
            "unknown",
            agentId,
            1
        );
    }
    
    // Getters
    public List<StackFrame> getCallStack() { return callStack; }
    public Map<Integer, LocalVariableTable> getLocalVariables() { return localVariables; }
    public HeapSnapshot getHeapSnapshot() { return heapSnapshot; }
    public ProgramCounter getProgramCounter() { return programCounter; }
    public ThreadState getThreadState() { return threadState; }
    public ContinuationPoint getContinuationPoint() { return continuationPoint; }
    public long getCaptureTimestamp() { return captureTimestamp; }
    public String getCaptureContext() { return captureContext; }
    public String getAgentId() { return agentId; }
    public int getStateVersion() { return stateVersion; }
    
    /**
     * Check if this is a valid strong mobility state
     */
    public boolean isStrongMobility() {
        return !callStack.isEmpty() && continuationPoint.getType() == ContinuationPoint.ContinuationType.RESUME;
    }
}
```

### 1.3 ContinuationPoint Class

```java
package io.amcp.mobility;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Continuation point for resuming execution after migration
 * Marks where and how to continue
 */
public final class ContinuationPoint implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final ContinuationType type;
    private final String methodName;
    private final int instructionOffset;
    private final int lineNumber;
    private final String continuationId;
    
    public enum ContinuationType {
        /**
         * Restart from beginning (weak mobility)
         * Agent executes from entry point
         */
        RESTART,
        
        /**
         * Resume from exact bytecode position (strong mobility)
         * Requires full stack reconstruction
         */
        RESUME,
        
        /**
         * Resume via callback method
         * Agent-defined continuation handler
         */
        CALLBACK,
        
        /**
         * Resume using stored continuation object
         * Advanced continuation-passing style
         */
        CONTINUATION
    }
    
    private ContinuationPoint(ContinuationType type, String methodName,
                             int instructionOffset, int lineNumber,
                             String continuationId) {
        this.type = type;
        this.methodName = methodName;
        this.instructionOffset = instructionOffset;
        this.lineNumber = lineNumber;
        this.continuationId = continuationId;
    }
    
    /**
     * Create restart continuation (weak mobility)
     */
    public static ContinuationPoint restart() {
        return new ContinuationPoint(ContinuationType.RESTART, null, 0, 0, null);
    }
    
    /**
     * Create resume continuation (strong mobility)
     */
    public static ContinuationPoint resume(String methodName, int instructionOffset,
                                          int lineNumber) {
        String id = String.format("%s:L%d:I%d", methodName, lineNumber, instructionOffset);
        return new ContinuationPoint(ContinuationType.RESUME, methodName,
                                    instructionOffset, lineNumber, id);
    }
    
    /**
     * Create callback continuation
     */
    public static ContinuationPoint callback(String callbackMethod) {
        return new ContinuationPoint(ContinuationType.CALLBACK, callbackMethod,
                                    0, 0, callbackMethod);
    }
    
    // Getters
    public ContinuationType getType() { return type; }
    public String getMethodName() { return methodName; }
    public int getInstructionOffset() { return instructionOffset; }
    public int getLineNumber() { return lineNumber; }
    public String getContinuationId() { return continuationId; }
    
    @Override
    public String toString() {
        return String.format("ContinuationPoint{type=%s, method=%s, line=%d, offset=%d}",
                           type, methodName, lineNumber, instructionOffset);
    }
}
```

---

## 2. Execution State Capture

### 2.1 StateCaptureService

```java
package io.amcp.mobility.capture;

import io.amcp.core.Agent;
import io.amcp.mobility.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Service for capturing agent execution state
 * Uses JVM internals and instrumentation
 */
public class StateCaptureService {
    
    private final ThreadMXBean threadMXBean;
    private final HeapAnalyzer heapAnalyzer;
    private final StackAnalyzer stackAnalyzer;
    
    public StateCaptureService() {
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.heapAnalyzer = new HeapAnalyzer();
        this.stackAnalyzer = new StackAnalyzer();
    }
    
    /**
     * Capture complete execution state for an agent
     * 
     * @param agent agent to capture
     * @param thread agent's execution thread
     * @return complete execution state
     */
    public ExecutionState captureState(Agent agent, Thread thread) {
        long timestamp = System.currentTimeMillis();
        
        // Capture call stack
        List<ExecutionState.StackFrame> callStack = stackAnalyzer.analyzeStack(thread);
        
        // Capture local variables for each frame
        Map<Integer, ExecutionState.LocalVariableTable> localVars = 
            stackAnalyzer.extractLocalVariables(thread, callStack);
        
        // Capture heap state (object graph from agent)
        ExecutionState.HeapSnapshot heapSnapshot = 
            heapAnalyzer.snapshotHeap(agent);
        
        // Determine program counter
        ExecutionState.ProgramCounter pc = 
            stackAnalyzer.getProgramCounter(thread, callStack);
        
        // Capture thread state
        ExecutionState.ThreadState threadState = captureThreadState(thread);
        
        // Create continuation point
        ContinuationPoint continuation = createContinuationPoint(callStack, pc);
        
        return new ExecutionState(
            callStack,
            localVars,
            heapSnapshot,
            pc,
            threadState,
            continuation,
            timestamp,
            agent.getContext().getContextId(),
            agent.getAgentId().toString(),
            1
        );
    }
    
    private ExecutionState.ThreadState captureThreadState(Thread thread) {
        // Capture locks held by thread
        Set<String> heldLocks = new HashSet<>();
        Map<String, Integer> monitorCounts = new HashMap<>();
        
        // Use ThreadMXBean to get lock info
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        // Process lock information...
        
        return new ExecutionState.ThreadState(
            heldLocks,
            monitorCounts,
            thread.getState()
        );
    }
    
    private ContinuationPoint createContinuationPoint(
            List<ExecutionState.StackFrame> callStack,
            ExecutionState.ProgramCounter pc) {
        
        if (callStack.isEmpty()) {
            return ContinuationPoint.restart();
        }
        
        ExecutionState.StackFrame topFrame = callStack.get(0);
        return ContinuationPoint.resume(
            topFrame.getMethodName(),
            topFrame.getBytecodeOffset(),
            topFrame.getLineNumber()
        );
    }
}
```

---

## 3. ATP Transport Protocol

### 3.1 ATP Package Format

```
ATP Package Structure:
┌──────────────────────────────────────────┐
│            ATP Header (256 bytes)        │
│  - Protocol Version: ATP/1.0             │
│  - Package Type: DISPATCH/CLONE/RETRACT  │
│  - Agent ID: UUID                        │
│  - Source Context: atp://...             │
│  - Dest Context: atp://...               │
│  - Timestamp: epoch millis               │
│  - Mobility Type: WEAK/STRONG            │
│  - Signature Length: N bytes             │
├──────────────────────────────────────────┤
│         Agent Bytecode (variable)        │
│  - Class files (JAR format)              │
│  - Dependencies manifest                 │
├──────────────────────────────────────────┤
│        Agent State (variable)            │
│  - Serialized agent fields               │
│  - Persistent data                       │
├──────────────────────────────────────────┤
│      Execution State (variable)          │
│  - Call stack frames                     │
│  - Local variables                       │
│  - Heap snapshot                         │
│  - Program counter                       │
│  - Thread state                          │
├──────────────────────────────────────────┤
│        Digital Signature                 │
│  - RSA/ECDSA signature                   │
│  - Certificate chain                     │
└──────────────────────────────────────────┘
```

### 3.2 ATP Transport Implementation

```java
package io.amcp.transport.atp;

import io.amcp.core.AgentID;
import io.amcp.mobility.ExecutionState;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Agent Transfer Protocol transport implementation
 * Handles agent packaging, transfer, and unpacking
 */
public class ATPTransportService {
    
    private final ATPPackager packager;
    private final ATPNetworkClient networkClient;
    private final ATPSecurityManager securityManager;
    
    public ATPTransportService() {
        this.packager = new ATPPackager();
        this.networkClient = new ATPNetworkClient();
        this.securityManager = new ATPSecurityManager();
    }
    
    /**
     * Transfer agent to destination
     */
    public CompletableFuture<ATPTransferResult> transferAgent(
            AgentID agentId,
            byte[] agentCode,
            byte[] agentState,
            ExecutionState execState,
            URI destination) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create ATP package
                ATPPackage pkg = packager.createPackage(
                    agentId,
                    agentCode,
                    agentState,
                    execState,
                    destination
                );
                
                // Sign package
                ATPPackage signedPkg = securityManager.signPackage(pkg);
                
                // Transfer over network
                ATPTransferResult result = networkClient.send(signedPkg, destination);
                
                return result;
                
            } catch (Exception e) {
                return ATPTransferResult.failure(e.getMessage());
            }
        });
    }
    
    /**
     * Receive agent from source
     */
    public CompletableFuture<ATPPackage> receiveAgent(URI source) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Receive ATP package
                ATPPackage pkg = networkClient.receive(source);
                
                // Verify signature
                if (!securityManager.verifyPackage(pkg)) {
                    throw new SecurityException("Invalid package signature");
                }
                
                return pkg;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to receive agent", e);
            }
        });
    }
}
```

---

## 4. Usage Example: Complete Strong Mobility Agent

```java
package com.example.agents;

import io.amcp.core.*;
import io.amcp.core.mobility.StrongMobilityAgent;
import java.util.concurrent.CompletableFuture;

/**
 * Example demonstrating strong mobility with Aglets-style dispatch
 */
public class DistributedComputeAgent implements StrongMobilityAgent {
    
    private AgentID agentId = AgentID.named("compute-agent-001");
    private AgentContext context;
    private AgentLifecycle lifecycle = AgentLifecycle.ACTIVE;
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            if (event.getTopic().equals("compute.request")) {
                performDistributedComputation(event.getPayload(String.class));
            }
        });
    }
    
    /**
     * Demonstrates strong mobility:
     * - Local variables preserved across migrations
     * - No manual state management needed
     * - Seamless execution flow
     */
    private void performDistributedComputation(String taskData) {
        // Step 1: Initial processing on local machine
        String preprocessed = preprocessData(taskData);
        int taskSize = preprocessed.length();
        
        System.out.println("Phase 1: Preprocessing complete, size=" + taskSize);
        
        // Step 2: Migrate to compute cluster for heavy processing
        // Strong mobility: preprocessed and taskSize preserved automatically!
        dispatch("atp://compute-cluster.local:4434/workers").join();
        
        // RESUMES HERE after migration with state intact
        System.out.println("Phase 2: Now at compute cluster, processing " + taskSize + " bytes");
        
        // Step 3: Heavy computation (runs on compute cluster)
        String computed = heavyComputation(preprocessed);
        double accuracy = 0.95; // Local variable
        
        // Step 4: Migrate to visualization server
        dispatch("atp://viz-server.local:4434/renderers").join();
        
        // RESUMES HERE with computed and accuracy preserved
        System.out.println("Phase 3: At viz server, accuracy=" + accuracy);
        
        // Step 5: Generate visualization
        String visualization = generateVisualization(computed, accuracy);
        
        // Step 6: Return to origin to report results
        retract().join();
        
        // RESUMES HERE at original location
        System.out.println("Phase 4: Back at origin, reporting results");
        reportResults(visualization);
    }
    
    private String preprocessData(String data) {
        return "preprocessed:" + data;
    }
    
    private String heavyComputation(String data) {
        // Simulate intensive computation
        return "computed:" + data.toUpperCase();
    }
    
    private String generateVisualization(String data, double accuracy) {
        return String.format("viz[data=%s, accuracy=%.2f]", data, accuracy);
    }
    
    private void reportResults(String results) {
        publishEvent(Event.builder()
            .topic("compute.completed")
            .payload(results)
            .sender(agentId)
            .build());
    }
    
    @Override
    public boolean supportsStrongMobility() {
        return true; // Enable strong mobility for this agent
    }
    
    // Standard Agent interface methods
    @Override
    public AgentID getAgentId() { return agentId; }
    
    @Override
    public AgentContext getContext() { return context; }
    
    @Override
    public AgentLifecycle getLifecycleState() { return lifecycle; }
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        return context.publishEvent(event);
    }
    
    @Override
    public CompletableFuture<Void> subscribe(String topicPattern) {
        return context.subscribe(agentId, topicPattern);
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(String topicPattern) {
        return context.unsubscribe(agentId, topicPattern);
    }
    
    @Override
    public void onActivate() {
        subscribe("compute.request");
    }
    
    @Override
    public void onDeactivate() {
        unsubscribe("compute.request");
    }
    
    @Override
    public void onDestroy() {
        // Cleanup
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        // Optional: custom pre-migration logic
        System.out.println("Migrating to: " + destinationContext);
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        // Optional: custom post-migration logic
        System.out.println("Arrived from: " + sourceContext);
    }
}
```

---

## 5. Benefits Summary

### For Developers
- **No Manual State Management**: Local variables automatically preserved
- **Natural Code Flow**: Write sequential code despite distribution
- **Simplified Logic**: No need for continuation passing or callbacks
- **Type Safety**: Full compile-time checking maintained

### For Operations
- **Dynamic Load Balancing**: Move agents to available resources
- **Fault Tolerance**: Checkpoint and restore capabilities
- **Resource Optimization**: Agents migrate to appropriate hardware
- **Monitoring**: Track agent migrations and state

### For Architecture
- **True Distribution**: Agents move computation to data
- **Scalability**: Horizontal scaling via agent mobility
- **Flexibility**: Runtime topology changes
- **Innovation**: Enables new patterns and use cases

---

**Document Version**: 1.0  
**Status**: Technical Specification  
**Implementation Priority**: High  
**Estimated Effort**: 20-24 weeks full implementation
