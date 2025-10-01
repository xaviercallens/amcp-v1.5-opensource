package io.amcp.connectors.ai.planning;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a complete task execution plan with dependencies and scheduling.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class TaskPlan {
    
    private final String planId;
    private final String sessionId;
    private final String userQuery;
    private final LocalDateTime creationTime;
    private final List<TaskDefinition> tasks;
    private final Map<String, Set<String>> dependencyGraph;
    private final List<List<TaskDefinition>> executionLevels;
    private final Map<String, Object> metadata;
    
    public TaskPlan(String planId, String sessionId, String userQuery, List<TaskDefinition> tasks) {
        this.planId = planId;
        this.sessionId = sessionId;
        this.userQuery = userQuery;
        this.creationTime = LocalDateTime.now();
        this.tasks = new ArrayList<>(tasks);
        this.dependencyGraph = buildDependencyGraph(tasks);
        this.executionLevels = buildExecutionLevels(tasks, dependencyGraph);
        this.metadata = new HashMap<>();
    }
    
    public TaskPlan(String planId, String sessionId, String userQuery, List<TaskDefinition> tasks, 
                   Map<String, Object> metadata) {
        this.planId = planId;
        this.sessionId = sessionId;
        this.userQuery = userQuery;
        this.creationTime = LocalDateTime.now();
        this.tasks = new ArrayList<>(tasks);
        this.dependencyGraph = buildDependencyGraph(tasks);
        this.executionLevels = buildExecutionLevels(tasks, dependencyGraph);
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    // Getters
    public String getPlanId() { return planId; }
    public String getSessionId() { return sessionId; }
    public String getUserQuery() { return userQuery; }
    public LocalDateTime getCreationTime() { return creationTime; }
    public List<TaskDefinition> getTasks() { return new ArrayList<>(tasks); }
    public int getTaskCount() { return tasks.size(); }
    public Map<String, Set<String>> getDependencyGraph() { return new HashMap<>(dependencyGraph); }
    public List<List<TaskDefinition>> getExecutionLevels() { return new ArrayList<>(executionLevels); }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    // Task access methods
    public TaskDefinition getTask(String taskId) {
        return tasks.stream()
                   .filter(task -> task.getTaskId().equals(taskId))
                   .findFirst()
                   .orElse(null);
    }
    
    public List<TaskDefinition> getTasksByAgentType(String agentType) {
        return tasks.stream()
                   .filter(task -> task.getAgentType().equals(agentType))
                   .collect(Collectors.toList());
    }
    
    public List<TaskDefinition> getRootTasks() {
        return tasks.stream()
                   .filter(task -> !task.hasDependencies())
                   .collect(Collectors.toList());
    }
    
    public List<TaskDefinition> getLeafTasks() {
        Set<String> dependentTaskIds = dependencyGraph.values().stream()
                                                     .flatMap(Set::stream)
                                                     .collect(Collectors.toSet());
        return tasks.stream()
                   .filter(task -> !dependentTaskIds.contains(task.getTaskId()))
                   .collect(Collectors.toList());
    }
    
    // Execution planning methods
    public List<TaskDefinition> getNextExecutableTasks(Set<String> completedTaskIds) {
        return tasks.stream()
                   .filter(task -> !completedTaskIds.contains(task.getTaskId()))
                   .filter(task -> completedTaskIds.containsAll(task.getDependencies()))
                   .sorted((t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()))
                   .collect(Collectors.toList());
    }
    
    public boolean isComplete(Set<String> completedTaskIds) {
        return tasks.stream()
                   .filter(task -> !task.isOptional())
                   .allMatch(task -> completedTaskIds.contains(task.getTaskId()));
    }
    
    public boolean canExecute(String taskId, Set<String> completedTaskIds) {
        TaskDefinition task = getTask(taskId);
        if (task == null) return false;
        
        return completedTaskIds.containsAll(task.getDependencies());
    }
    
    // Analysis methods
    public int getTotalTaskCount() {
        return tasks.size();
    }
    
    public int getRequiredTaskCount() {
        return (int) tasks.stream().filter(task -> !task.isOptional()).count();
    }
    
    public int getOptionalTaskCount() {
        return (int) tasks.stream().filter(TaskDefinition::isOptional).count();
    }
    
    public int getMaxParallelism() {
        return executionLevels.stream()
                             .mapToInt(List::size)
                             .max()
                             .orElse(0);
    }
    
    public long getEstimatedDurationMs() {
        return executionLevels.stream()
                             .mapToLong(level -> level.stream()
                                                     .mapToLong(TaskDefinition::getTimeoutMs)
                                                     .max()
                                                     .orElse(0))
                             .sum();
    }
    
    // Validation methods
    public List<String> validatePlan() {
        List<String> issues = new ArrayList<>();
        
        // Check for circular dependencies
        if (hasCyclicDependencies()) {
            issues.add("Plan contains circular dependencies");
        }
        
        // Check for missing dependencies
        Set<String> taskIds = tasks.stream()
                                  .map(TaskDefinition::getTaskId)
                                  .collect(Collectors.toSet());
        
        for (TaskDefinition task : tasks) {
            for (String dep : task.getDependencies()) {
                if (!taskIds.contains(dep)) {
                    issues.add("Task " + task.getTaskId() + " depends on missing task: " + dep);
                }
            }
        }
        
        // Check for duplicate task IDs
        Set<String> seenIds = new HashSet<>();
        for (TaskDefinition task : tasks) {
            if (!seenIds.add(task.getTaskId())) {
                issues.add("Duplicate task ID: " + task.getTaskId());
            }
        }
        
        return issues;
    }
    
    public boolean isValid() {
        return validatePlan().isEmpty();
    }
    
    // Metadata management
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }
    
    // Private helper methods
    private Map<String, Set<String>> buildDependencyGraph(List<TaskDefinition> tasks) {
        Map<String, Set<String>> graph = new HashMap<>();
        
        for (TaskDefinition task : tasks) {
            graph.put(task.getTaskId(), new HashSet<>(task.getDependencies()));
        }
        
        return graph;
    }
    
    private List<List<TaskDefinition>> buildExecutionLevels(List<TaskDefinition> tasks, 
                                                          Map<String, Set<String>> depGraph) {
        List<List<TaskDefinition>> levels = new ArrayList<>();
        Set<String> processedTasks = new HashSet<>();
        
        while (processedTasks.size() < tasks.size()) {
            List<TaskDefinition> currentLevel = new ArrayList<>();
            
            for (TaskDefinition task : tasks) {
                if (!processedTasks.contains(task.getTaskId()) && 
                    processedTasks.containsAll(task.getDependencies())) {
                    currentLevel.add(task);
                }
            }
            
            if (currentLevel.isEmpty()) {
                // This shouldn't happen with valid plans, but handle gracefully
                break;
            }
            
            // Sort by priority
            currentLevel.sort((t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()));
            
            levels.add(currentLevel);
            processedTasks.addAll(currentLevel.stream()
                                            .map(TaskDefinition::getTaskId)
                                            .collect(Collectors.toSet()));
        }
        
        return levels;
    }
    
    private boolean hasCyclicDependencies() {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        
        for (String taskId : dependencyGraph.keySet()) {
            if (hasCycle(taskId, visiting, visited)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasCycle(String taskId, Set<String> visiting, Set<String> visited) {
        if (visiting.contains(taskId)) {
            return true; // Cycle detected
        }
        
        if (visited.contains(taskId)) {
            return false; // Already processed
        }
        
        visiting.add(taskId);
        
        Set<String> dependencies = dependencyGraph.get(taskId);
        if (dependencies != null) {
            for (String dep : dependencies) {
                if (hasCycle(dep, visiting, visited)) {
                    return true;
                }
            }
        }
        
        visiting.remove(taskId);
        visited.add(taskId);
        
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("TaskPlan{id='%s', sessionId='%s', tasks=%d, levels=%d, maxParallelism=%d}", 
                           planId, sessionId, tasks.size(), executionLevels.size(), getMaxParallelism());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TaskPlan that = (TaskPlan) obj;
        return Objects.equals(planId, that.planId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(planId);
    }
}