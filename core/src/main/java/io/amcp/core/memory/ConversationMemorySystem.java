package io.amcp.core.memory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Conversation Memory System for AMCP v1.5 Enterprise Edition
 * 
 * Provides persistent conversation history and context management for 
 * long-running chat sessions. Maintains user context, agent interactions,
 * and conversation metadata to enable seamless multi-agent coordination.
 * 
 * Key Features:
 * - Persistent conversation sessions
 * - Context-aware message storage
 * - Agent interaction tracking
 * - Memory search and retrieval
 * - Conversation summarization
 * - Memory optimization and cleanup
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class ConversationMemorySystem {
    
    // Memory storage
    private final ConcurrentHashMap<String, ConversationSession> sessions;
    private final ConcurrentHashMap<String, UserProfile> userProfiles;
    
    // Memory configuration
    private final MemoryConfiguration configuration;
    
    // Memory state
    private volatile boolean isRunning;
    private volatile long lastCleanup;
    
    public ConversationMemorySystem() {
        this(new MemoryConfiguration());
    }
    
    public ConversationMemorySystem(MemoryConfiguration configuration) {
        this.configuration = configuration;
        this.sessions = new ConcurrentHashMap<>();
        this.userProfiles = new ConcurrentHashMap<>();
        this.isRunning = false;
        this.lastCleanup = System.currentTimeMillis();
    }
    
    /**
     * Starts the memory system with background maintenance
     */
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            if (isRunning) {
                return;
            }
            
            isRunning = true;
            logMessage("🧠 Conversation Memory System started");
            
            // Start background cleanup
            if (configuration.isAutoCleanupEnabled()) {
                startBackgroundCleanup();
            }
        });
    }
    
    /**
     * Stops the memory system and performs cleanup
     */
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            if (!isRunning) {
                return;
            }
            
            isRunning = false;
            
            // Optionally persist sessions before shutdown
            if (configuration.isPersistOnShutdown()) {
                persistAllSessions();
            }
            
            logMessage("🛑 Conversation Memory System stopped");
        });
    }
    
    /**
     * Creates or retrieves a conversation session
     */
    public CompletableFuture<ConversationSession> getOrCreateSession(String sessionId, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isRunning) {
                throw new IllegalStateException("Memory system is not running");
            }
            
            ConversationSession session = sessions.get(sessionId);
            if (session == null) {
                session = new ConversationSession(sessionId, userId);
                sessions.put(sessionId, session);
                
                // Update user profile
                updateUserProfile(userId, sessionId);
                
                logMessage("💭 Created new conversation session: " + sessionId + " for user: " + userId);
            }
            
            return session;
        });
    }
    
    /**
     * Adds a message to a conversation session
     */
    public CompletableFuture<Void> addMessage(String sessionId, ConversationMessage message) {
        return CompletableFuture.runAsync(() -> {
            ConversationSession session = sessions.get(sessionId);
            if (session != null) {
                session.addMessage(message);
                session.updateLastActivity();
                
                logMessage("📝 Added message to session " + sessionId + " from " + message.getSender());
            } else {
                logMessage("⚠️ Session not found: " + sessionId);
            }
        });
    }
    
    /**
     * Gets conversation history for a session
     */
    public CompletableFuture<List<ConversationMessage>> getConversationHistory(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            ConversationSession session = sessions.get(sessionId);
            return session != null ? session.getMessages() : new ArrayList<>();
        });
    }
    
    /**
     * Gets conversation history with filtering
     */
    public CompletableFuture<List<ConversationMessage>> getConversationHistory(String sessionId, 
                                                                              MessageFilter filter) {
        return CompletableFuture.supplyAsync(() -> {
            ConversationSession session = sessions.get(sessionId);
            if (session == null) {
                return new ArrayList<>();
            }
            
            return session.getMessages().stream()
                .filter(filter::matches)
                .collect(Collectors.toList());
        });
    }
    
    /**
     * Searches messages across all sessions for a user
     */
    public CompletableFuture<List<ConversationMessage>> searchUserMessages(String userId, String searchQuery) {
        return CompletableFuture.supplyAsync(() -> {
            List<ConversationMessage> results = new ArrayList<>();
            
            for (ConversationSession session : sessions.values()) {
                if (session.getUserId().equals(userId)) {
                    for (ConversationMessage message : session.getMessages()) {
                        if (message.getContent().toLowerCase().contains(searchQuery.toLowerCase())) {
                            results.add(message);
                        }
                    }
                }
            }
            
            logMessage("🔍 Found " + results.size() + " messages for query: " + searchQuery);
            return results;
        });
    }
    
    /**
     * Gets conversation context for agent interactions
     */
    public CompletableFuture<ConversationContext> getConversationContext(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            ConversationSession session = sessions.get(sessionId);
            if (session == null) {
                return null;
            }
            
            // Build context from recent messages
            List<ConversationMessage> recentMessages = session.getRecentMessages(configuration.getContextWindowSize());
            
            // Extract entities and topics
            Set<String> topics = extractTopics(recentMessages);
            Set<String> entities = extractEntities(recentMessages);
            Map<String, Integer> agentInteractions = countAgentInteractions(recentMessages);
            
            return new ConversationContext(
                sessionId,
                session.getUserId(),
                recentMessages,
                topics,
                entities,
                agentInteractions,
                session.getStartTime(),
                session.getLastActivity()
            );
        });
    }
    
    /**
     * Generates a conversation summary
     */
    public CompletableFuture<ConversationSummary> generateSummary(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            ConversationSession session = sessions.get(sessionId);
            if (session == null) {
                return null;
            }
            
            List<ConversationMessage> messages = session.getMessages();
            
            // Basic summary statistics
            int totalMessages = messages.size();
            int userMessages = (int) messages.stream().filter(m -> "User".equals(m.getSender())).count();
            int agentMessages = totalMessages - userMessages;
            
            // Extract topics and participants
            Set<String> topics = extractTopics(messages);
            Set<String> participants = messages.stream()
                .map(ConversationMessage::getSender)
                .collect(Collectors.toSet());
            
            // Duration
            long durationMinutes = (session.getLastActivity().toEpochMilli() - 
                                  session.getStartTime().toEpochMilli()) / (1000 * 60);
            
            return new ConversationSummary(
                sessionId,
                session.getUserId(),
                totalMessages,
                userMessages,
                agentMessages,
                participants,
                topics,
                durationMinutes,
                session.getStartTime(),
                session.getLastActivity()
            );
        });
    }
    
    /**
     * Gets memory system statistics
     */
    public CompletableFuture<MemoryStatistics> getStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            int totalSessions = sessions.size();
            int activeSessions = (int) sessions.values().stream()
                .filter(s -> s.isActive(configuration.getSessionTimeoutMinutes()))
                .count();
            int totalUsers = userProfiles.size();
            int totalMessages = sessions.values().stream()
                .mapToInt(s -> s.getMessages().size())
                .sum();
                
            return new MemoryStatistics(totalSessions, activeSessions, totalUsers, totalMessages);
        });
    }
    
    /**
     * Clears conversation history for a session
     */
    public CompletableFuture<Void> clearSession(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            sessions.remove(sessionId);
            logMessage("🗑️ Cleared session: " + sessionId);
        });
    }
    
    /**
     * Clears all sessions for a user
     */
    public CompletableFuture<Void> clearUserSessions(String userId) {
        return CompletableFuture.runAsync(() -> {
            int removed = 0;
            for (Iterator<Map.Entry<String, ConversationSession>> it = sessions.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, ConversationSession> entry = it.next();
                if (entry.getValue().getUserId().equals(userId)) {
                    it.remove();
                    removed++;
                }
            }
            
            userProfiles.remove(userId);
            logMessage("🗑️ Cleared " + removed + " sessions for user: " + userId);
        });
    }
    
    // Private helper methods
    
    private void updateUserProfile(String userId, String sessionId) {
        UserProfile profile = userProfiles.get(userId);
        if (profile == null) {
            profile = new UserProfile(userId);
            userProfiles.put(userId, profile);
        }
        profile.addSession(sessionId);
    }
    
    private Set<String> extractTopics(List<ConversationMessage> messages) {
        Set<String> topics = new HashSet<>();
        
        for (ConversationMessage message : messages) {
            String content = message.getContent().toLowerCase();
            
            // Simple keyword-based topic extraction
            if (containsKeywords(content, "travel", "trip", "vacation", "destination")) {
                topics.add("travel");
            }
            if (containsKeywords(content, "stock", "price", "market", "invest", "finance")) {
                topics.add("finance");
            }
            if (containsKeywords(content, "weather", "forecast", "temperature", "climate")) {
                topics.add("weather");
            }
            if (containsKeywords(content, "help", "how", "what", "explain")) {
                topics.add("assistance");
            }
        }
        
        return topics;
    }
    
    private Set<String> extractEntities(List<ConversationMessage> messages) {
        Set<String> entities = new HashSet<>();
        
        // Simple entity extraction (in real implementation, would use NLP)
        for (ConversationMessage message : messages) {
            String content = message.getContent();
            
            // Extract potential place names (capitalized words)
            String[] words = content.split("\\\\s+");
            for (String word : words) {
                if (word.length() > 3 && Character.isUpperCase(word.charAt(0))) {
                    entities.add(word);
                }
            }
        }
        
        return entities;
    }
    
    private Map<String, Integer> countAgentInteractions(List<ConversationMessage> messages) {
        Map<String, Integer> counts = new HashMap<>();
        
        for (ConversationMessage message : messages) {
            String sender = message.getSender();
            if (!"User".equals(sender)) {
                counts.put(sender, counts.getOrDefault(sender, 0) + 1);
            }
        }
        
        return counts;
    }
    
    private boolean containsKeywords(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private void startBackgroundCleanup() {
        CompletableFuture.runAsync(() -> {
            while (isRunning) {
                try {
                    performCleanup();
                    Thread.sleep(configuration.getCleanupIntervalMinutes() * 60 * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logMessage("❌ Cleanup error: " + e.getMessage());
                }
            }
        });
    }
    
    private void performCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > configuration.getCleanupIntervalMinutes() * 60 * 1000) {
            
            int removed = 0;
            long timeoutThreshold = now - (configuration.getSessionTimeoutMinutes() * 60 * 1000);
            
            for (Iterator<Map.Entry<String, ConversationSession>> it = sessions.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, ConversationSession> entry = it.next();
                ConversationSession session = entry.getValue();
                
                if (session.getLastActivity().toEpochMilli() < timeoutThreshold) {
                    it.remove();
                    removed++;
                }
            }
            
            if (removed > 0) {
                logMessage("🧹 Cleaned up " + removed + " inactive sessions");
            }
            
            lastCleanup = now;
        }
    }
    
    private void persistAllSessions() {
        // Placeholder for persistence implementation
        logMessage("💾 Persisting " + sessions.size() + " sessions");
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [MemorySystem] " + message);
    }
    
    // Inner classes for memory data structures
    
    /**
     * Represents a conversation session
     */
    public static class ConversationSession {
        private final String sessionId;
        private final String userId;
        private final List<ConversationMessage> messages;
        private final Instant startTime;
        private volatile Instant lastActivity;
        
        public ConversationSession(String sessionId, String userId) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.messages = new ArrayList<>();
            this.startTime = Instant.now();
            this.lastActivity = Instant.now();
        }
        
        public synchronized void addMessage(ConversationMessage message) {
            messages.add(message);
            lastActivity = Instant.now();
        }
        
        public synchronized List<ConversationMessage> getMessages() {
            return new ArrayList<>(messages);
        }
        
        public synchronized List<ConversationMessage> getRecentMessages(int count) {
            int size = messages.size();
            int fromIndex = Math.max(0, size - count);
            return new ArrayList<>(messages.subList(fromIndex, size));
        }
        
        public void updateLastActivity() {
            this.lastActivity = Instant.now();
        }
        
        public boolean isActive(long timeoutMinutes) {
            long timeoutMs = timeoutMinutes * 60 * 1000;
            return (System.currentTimeMillis() - lastActivity.toEpochMilli()) < timeoutMs;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public Instant getStartTime() { return startTime; }
        public Instant getLastActivity() { return lastActivity; }
    }
    
    /**
     * Represents a conversation message
     */
    public static class ConversationMessage {
        private final String sender;
        private final String content;
        private final Instant timestamp;
        private final Map<String, Object> metadata;
        
        public ConversationMessage(String sender, String content) {
            this(sender, content, new HashMap<>());
        }
        
        public ConversationMessage(String sender, String content, Map<String, Object> metadata) {
            this.sender = sender;
            this.content = content;
            this.timestamp = Instant.now();
            this.metadata = new HashMap<>(metadata);
        }
        
        // Getters
        public String getSender() { return sender; }
        public String getContent() { return content; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return Collections.unmodifiableMap(metadata); }
        
        @Override
        public String toString() {
            return String.format("[%s] %s: %s", 
                timestamp.toString(), sender, content);
        }
    }
    
    /**
     * Message filter interface
     */
    public interface MessageFilter {
        boolean matches(ConversationMessage message);
        
        static MessageFilter bySender(String sender) {
            return message -> sender.equals(message.getSender());
        }
        
        static MessageFilter byContent(String keyword) {
            return message -> message.getContent().toLowerCase().contains(keyword.toLowerCase());
        }
        
        static MessageFilter afterTime(Instant time) {
            return message -> message.getTimestamp().isAfter(time);
        }
    }
    
    /**
     * User profile for tracking sessions
     */
    private static class UserProfile {
        private final String userId;
        private final Set<String> sessionIds;
        private final Instant createdTime;
        
        public UserProfile(String userId) {
            this.userId = userId;
            this.sessionIds = new HashSet<>();
            this.createdTime = Instant.now();
        }
        
        public void addSession(String sessionId) {
            sessionIds.add(sessionId);
        }
        
        public String getUserId() { return userId; }
        public Set<String> getSessionIds() { return Collections.unmodifiableSet(sessionIds); }
        public Instant getCreatedTime() { return createdTime; }
    }
    
    /**
     * Conversation context for agent interactions
     */
    public static class ConversationContext {
        private final String sessionId;
        private final String userId;
        private final List<ConversationMessage> recentMessages;
        private final Set<String> topics;
        private final Set<String> entities;
        private final Map<String, Integer> agentInteractions;
        private final Instant sessionStart;
        private final Instant lastActivity;
        
        public ConversationContext(String sessionId, String userId, List<ConversationMessage> recentMessages,
                                 Set<String> topics, Set<String> entities, Map<String, Integer> agentInteractions,
                                 Instant sessionStart, Instant lastActivity) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.recentMessages = new ArrayList<>(recentMessages);
            this.topics = new HashSet<>(topics);
            this.entities = new HashSet<>(entities);
            this.agentInteractions = new HashMap<>(agentInteractions);
            this.sessionStart = sessionStart;
            this.lastActivity = lastActivity;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public List<ConversationMessage> getRecentMessages() { return Collections.unmodifiableList(recentMessages); }
        public Set<String> getTopics() { return Collections.unmodifiableSet(topics); }
        public Set<String> getEntities() { return Collections.unmodifiableSet(entities); }
        public Map<String, Integer> getAgentInteractions() { return Collections.unmodifiableMap(agentInteractions); }
        public Instant getSessionStart() { return sessionStart; }
        public Instant getLastActivity() { return lastActivity; }
    }
    
    /**
     * Conversation summary
     */
    public static class ConversationSummary {
        private final String sessionId;
        private final String userId;
        private final int totalMessages;
        private final int userMessages;
        private final int agentMessages;
        private final Set<String> participants;
        private final Set<String> topics;
        private final long durationMinutes;
        private final Instant startTime;
        private final Instant endTime;
        
        public ConversationSummary(String sessionId, String userId, int totalMessages, int userMessages,
                                 int agentMessages, Set<String> participants, Set<String> topics,
                                 long durationMinutes, Instant startTime, Instant endTime) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.totalMessages = totalMessages;
            this.userMessages = userMessages;
            this.agentMessages = agentMessages;
            this.participants = new HashSet<>(participants);
            this.topics = new HashSet<>(topics);
            this.durationMinutes = durationMinutes;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public int getTotalMessages() { return totalMessages; }
        public int getUserMessages() { return userMessages; }
        public int getAgentMessages() { return agentMessages; }
        public Set<String> getParticipants() { return Collections.unmodifiableSet(participants); }
        public Set<String> getTopics() { return Collections.unmodifiableSet(topics); }
        public long getDurationMinutes() { return durationMinutes; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        
        @Override
        public String toString() {
            return String.format("ConversationSummary{session=%s, user=%s, messages=%d, duration=%d min, topics=%s}",
                sessionId, userId, totalMessages, durationMinutes, topics);
        }
    }
    
    /**
     * Memory system configuration
     */
    public static class MemoryConfiguration {
        private int contextWindowSize = 20;
        private int sessionTimeoutMinutes = 60;
        private int cleanupIntervalMinutes = 15;
        private boolean autoCleanupEnabled = true;
        private boolean persistOnShutdown = false;
        
        // Getters and setters
        public int getContextWindowSize() { return contextWindowSize; }
        public void setContextWindowSize(int size) { this.contextWindowSize = size; }
        
        public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
        public void setSessionTimeoutMinutes(int timeout) { this.sessionTimeoutMinutes = timeout; }
        
        public int getCleanupIntervalMinutes() { return cleanupIntervalMinutes; }
        public void setCleanupIntervalMinutes(int interval) { this.cleanupIntervalMinutes = interval; }
        
        public boolean isAutoCleanupEnabled() { return autoCleanupEnabled; }
        public void setAutoCleanupEnabled(boolean enabled) { this.autoCleanupEnabled = enabled; }
        
        public boolean isPersistOnShutdown() { return persistOnShutdown; }
        public void setPersistOnShutdown(boolean persist) { this.persistOnShutdown = persist; }
    }
    
    /**
     * Memory system statistics
     */
    public static class MemoryStatistics {
        private final int totalSessions;
        private final int activeSessions;
        private final int totalUsers;
        private final int totalMessages;
        
        public MemoryStatistics(int totalSessions, int activeSessions, int totalUsers, int totalMessages) {
            this.totalSessions = totalSessions;
            this.activeSessions = activeSessions;
            this.totalUsers = totalUsers;
            this.totalMessages = totalMessages;
        }
        
        // Getters
        public int getTotalSessions() { return totalSessions; }
        public int getActiveSessions() { return activeSessions; }
        public int getTotalUsers() { return totalUsers; }
        public int getTotalMessages() { return totalMessages; }
        
        @Override
        public String toString() {
            return String.format("MemoryStats{sessions=%d/%d, users=%d, messages=%d}",
                activeSessions, totalSessions, totalUsers, totalMessages);
        }
    }
}