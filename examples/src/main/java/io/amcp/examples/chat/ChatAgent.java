package io.amcp.examples.chat;

import java.util.*;

/**
 * ChatAgent - Empathetic conversational agent providing emotional support
 * and concise, supportive responses.
 * 
 * Persona: Empathetic, supportive, concise (2-3 sentences max)
 * Focus: Emotional support, mood detection, brief encouragement
 * 
 * Note: Does NOT include famous quotes or lengthy advice - keeps it brief and empathetic
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class ChatAgent extends BaseChatAgent {
    
    private static final String AGENT_NAME = "ChatAgent";
    private static final String SPECIALTY = "empathetic_support";
    
    // Maximum response length (2-3 sentences)
    private static final int MAX_SENTENCES = 3;
    
    public ChatAgent() {
        super(AGENT_NAME);
    }
    
    @Override
    protected String getPersona() {
        return "Empathetic listener providing brief, supportive responses";
    }
    
    @Override
    protected String getSpecialty() {
        return SPECIALTY;
    }
    
    @Override
    protected void subscribeToTopics() {
        subscribe("chat.request.**");
        subscribe("orchestrator.task.chat");
        subscribe("meshchat.chat.**");
        logMessage("Subscribed to general chat topics");
    }
    
    @Override
    protected String generateResponse(String query, String conversationId, List<Map<String, Object>> priorMessages) {
        // Detect mood/sentiment from query
        String mood = detectMood(query);
        
        // Generate concise, empathetic response based on mood
        return generateEmpatheticResponse(query, mood, priorMessages);
    }
    
    /**
     * Detects emotional tone/mood from user query
     */
    private String detectMood(String query) {
        String lowerQuery = query.toLowerCase();
        
        // Negative emotions
        if (lowerQuery.contains("stress") || lowerQuery.contains("overwhelm") || lowerQuery.contains("anxious")) {
            return "stressed";
        }
        if (lowerQuery.contains("sad") || lowerQuery.contains("down") || lowerQuery.contains("depressed")) {
            return "sad";
        }
        if (lowerQuery.contains("frustrated") || lowerQuery.contains("angry") || lowerQuery.contains("annoyed")) {
            return "frustrated";
        }
        if (lowerQuery.contains("tired") || lowerQuery.contains("exhaust") || lowerQuery.contains("burnout")) {
            return "tired";
        }
        if (lowerQuery.contains("worried") || lowerQuery.contains("concern") || lowerQuery.contains("nervous")) {
            return "worried";
        }
        if (lowerQuery.contains("confused") || lowerQuery.contains("lost") || lowerQuery.contains("uncertain")) {
            return "confused";
        }
        
        // Positive emotions
        if (lowerQuery.contains("happy") || lowerQuery.contains("excited") || lowerQuery.contains("great")) {
            return "happy";
        }
        if (lowerQuery.contains("grateful") || lowerQuery.contains("thankful") || lowerQuery.contains("appreciate")) {
            return "grateful";
        }
        if (lowerQuery.contains("motivated") || lowerQuery.contains("energized") || lowerQuery.contains("inspired")) {
            return "motivated";
        }
        
        // Neutral/seeking help
        if (lowerQuery.contains("help") || lowerQuery.contains("advice") || lowerQuery.contains("suggest")) {
            return "seeking_help";
        }
        
        // Default neutral
        return "neutral";
    }
    
    /**
     * Generates brief, empathetic response (2-3 sentences max)
     */
    private String generateEmpatheticResponse(String query, String mood, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        
        switch (mood) {
            case "stressed":
                response.append("I hear that you're feeling stressed right now. ");
                response.append("It's completely normal to feel overwhelmed sometimes. ");
                response.append("Take a deep breath - you've got this.");
                break;
                
            case "sad":
                response.append("I'm sorry you're feeling down. ");
                response.append("It's okay to not be okay sometimes. ");
                response.append("Remember, this feeling is temporary and brighter days are ahead.");
                break;
                
            case "frustrated":
                response.append("I can sense your frustration, and that's completely valid. ");
                response.append("Sometimes things don't go as planned, and that's okay. ");
                response.append("Take a moment to reset - you're doing better than you think.");
                break;
                
            case "tired":
                response.append("It sounds like you're running on empty right now. ");
                response.append("Rest isn't a luxury, it's a necessity. ");
                response.append("Be kind to yourself and take the break you deserve.");
                break;
                
            case "worried":
                response.append("I understand you're feeling worried about this. ");
                response.append("Worrying shows you care, but try to focus on what you can control. ");
                response.append("You're more capable than you give yourself credit for.");
                break;
                
            case "confused":
                response.append("Feeling uncertain is part of the journey. ");
                response.append("It's okay not to have all the answers right now. ");
                response.append("Take it one step at a time, and clarity will come.");
                break;
                
            case "happy":
                response.append("That's wonderful to hear! ");
                response.append("Your positive energy is contagious. ");
                response.append("Keep celebrating those wins, big and small!");
                break;
                
            case "grateful":
                response.append("Gratitude is such a powerful mindset. ");
                response.append("It's beautiful that you're taking time to appreciate the good things. ");
                response.append("That perspective will serve you well.");
                break;
                
            case "motivated":
                response.append("I love your energy and enthusiasm! ");
                response.append("Channel that motivation into action. ");
                response.append("You're on the right track - keep going!");
                break;
                
            case "seeking_help":
                response.append("I'm here to help however I can. ");
                response.append("Asking for support is a sign of strength, not weakness. ");
                response.append("Let's work through this together.");
                break;
                
            default: // neutral
                response.append("Thanks for sharing that with me. ");
                response.append("I'm here to listen and support you. ");
                response.append("How can I help you today?");
                break;
        }
        
        return response.toString();
    }
    
    /**
     * Coordinates with QuoteAgent for inspirational quotes when appropriate
     */
    private boolean shouldInvolveQuoteAgent(String mood) {
        // Suggest quote agent for emotional support scenarios
        return mood.equals("stressed") || mood.equals("sad") || 
               mood.equals("tired") || mood.equals("worried") ||
               mood.equals("motivated");
    }
    
    /**
     * Override to add mood detection to response payload
     */
    @Override
    protected void sendChatResponse(String response, String correlationId, String conversationId) {
        Map<String, Object> responsePayload = new HashMap<>();
        
        // Standard fields
        responsePayload.put("response", response);
        responsePayload.put("conversationId", conversationId);
        responsePayload.put("correlationId", correlationId);
        responsePayload.put("sourceAgent", getAgentId().getId());
        responsePayload.put("agentType", "ChatAgent");
        responsePayload.put("specialty", getSpecialty());
        responsePayload.put("timestamp", java.time.LocalDateTime.now().toString());
        
        // Additional metadata
        responsePayload.put("responseLength", "concise"); // 2-3 sentences
        responsePayload.put("tone", "empathetic");
        
        publishEvent(io.amcp.core.Event.builder()
            .topic("agent.response.chat")
            .payload(responsePayload)
            .correlationId(correlationId)
            .sender(getAgentId())
            .build());
            
        logMessage("Sent empathetic chat response for conversation: " + conversationId);
    }
}
