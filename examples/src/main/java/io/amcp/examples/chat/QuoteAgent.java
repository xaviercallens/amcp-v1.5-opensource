package io.amcp.examples.chat;

import java.util.*;

/**
 * QuoteAgent - Specialized agent providing inspirational quotes
 * and motivational content.
 * 
 * Persona: Inspirational, uplifting, wisdom-focused
 * Focus: Motivational quotes, life wisdom, encouragement
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class QuoteAgent extends BaseChatAgent {
    
    private static final String AGENT_NAME = "QuoteAgent";
    private static final String SPECIALTY = "inspirational_quotes";
    
    private final List<Quote> quoteDatabase = new ArrayList<>();
    
    public QuoteAgent() {
        super(AGENT_NAME);
        initializeQuoteDatabase();
    }
    
    @Override
    protected String getPersona() {
        return "Inspirational guide sharing wisdom and encouragement";
    }
    
    @Override
    protected String getSpecialty() {
        return SPECIALTY;
    }
    
    @Override
    protected void subscribeToTopics() {
        subscribe("chat.request.quote.**");
        subscribe("orchestrator.task.quote");
        subscribe("meshchat.quote.**");
        logMessage("Subscribed to quote-specific topics");
    }
    
    @Override
    protected String generateResponse(String query, String conversationId, List<Map<String, Object>> priorMessages) {
        String lowerQuery = query.toLowerCase();
        
        // Determine quote category based on query
        String category = determineCategory(lowerQuery);
        
        // Get relevant quote
        Quote quote = getQuoteByCategory(category);
        
        // Format response with structured data
        return formatQuoteResponse(quote, category);
    }
    
    /**
     * Sends structured quote response with JSON data
     */
    @Override
    protected void sendChatResponse(String response, String correlationId, String conversationId) {
        // Extract quote from response for structured data
        Quote quote = extractQuoteFromResponse(response);
        
        Map<String, Object> responsePayload = new HashMap<>();
        
        // Structured quote data
        if (quote != null) {
            responsePayload.put("quote", quote.text);
            responsePayload.put("author", quote.author);
            responsePayload.put("category", quote.category);
        }
        
        // Standard fields
        responsePayload.put("formattedResponse", response);
        responsePayload.put("conversationId", conversationId);
        responsePayload.put("correlationId", correlationId);
        responsePayload.put("sourceAgent", getAgentId().getId());
        responsePayload.put("agentType", "QuoteAgent");
        responsePayload.put("specialty", getSpecialty());
        responsePayload.put("timestamp", java.time.LocalDateTime.now().toString());
        
        publishEvent(io.amcp.core.Event.builder()
            .topic("agent.response.quote")
            .payload(responsePayload)
            .correlationId(correlationId)
            .sender(getAgentId())
            .build());
            
        logMessage("Sent quote response for conversation: " + conversationId);
    }
    
    private String determineCategory(String query) {
        if (query.contains("motivat") || query.contains("inspire") || query.contains("encourage")) {
            return "motivation";
        }
        if (query.contains("success") || query.contains("achieve") || query.contains("goal")) {
            return "success";
        }
        if (query.contains("persever") || query.contains("challenge") || query.contains("difficult")) {
            return "perseverance";
        }
        if (query.contains("leader") || query.contains("manage")) {
            return "leadership";
        }
        if (query.contains("wisdom") || query.contains("life") || query.contains("philosophy")) {
            return "wisdom";
        }
        if (query.contains("change") || query.contains("innovation") || query.contains("transform")) {
            return "change";
        }
        if (query.contains("team") || query.contains("together") || query.contains("collaboration")) {
            return "teamwork";
        }
        if (query.contains("learn") || query.contains("grow") || query.contains("develop")) {
            return "growth";
        }
        
        // Default to motivation
        return "motivation";
    }
    
    private Quote getQuoteByCategory(String category) {
        List<Quote> categoryQuotes = new ArrayList<>();
        
        for (Quote quote : quoteDatabase) {
            if (quote.category.equals(category)) {
                categoryQuotes.add(quote);
            }
        }
        
        if (categoryQuotes.isEmpty()) {
            // Fallback to any quote
            return quoteDatabase.get(new Random().nextInt(quoteDatabase.size()));
        }
        
        // Return random quote from category
        return categoryQuotes.get(new Random().nextInt(categoryQuotes.size()));
    }
    
    private String formatQuoteResponse(Quote quote, String category) {
        StringBuilder response = new StringBuilder();
        response.append("✨ **Inspirational Quote**\n\n");
        response.append("\"").append(quote.text).append("\"\n\n");
        response.append("— ").append(quote.author).append("\n\n");
        
        // Add contextual message based on category
        response.append(getContextualMessage(category));
        
        return response.toString();
    }
    
    private String getContextualMessage(String category) {
        switch (category) {
            case "motivation":
                return "💪 Stay motivated and keep pushing forward!";
            case "success":
                return "🎯 Success is within your reach!";
            case "perseverance":
                return "🌟 Keep going, you're stronger than you think!";
            case "leadership":
                return "👑 Lead with purpose and inspire others!";
            case "wisdom":
                return "🧠 Wisdom guides us through life's journey.";
            case "change":
                return "🔄 Embrace change and grow from it!";
            case "teamwork":
                return "🤝 Together we achieve more!";
            case "growth":
                return "🌱 Keep learning and growing every day!";
            default:
                return "💫 May this inspire your journey!";
        }
    }
    
    private Quote extractQuoteFromResponse(String response) {
        // Simple extraction - in production, this would be more robust
        for (Quote quote : quoteDatabase) {
            if (response.contains(quote.text)) {
                return quote;
            }
        }
        return null;
    }
    
    private void initializeQuoteDatabase() {
        // Motivation
        quoteDatabase.add(new Quote("The only way to do great work is to love what you do.", "Steve Jobs", "motivation"));
        quoteDatabase.add(new Quote("Believe you can and you're halfway there.", "Theodore Roosevelt", "motivation"));
        quoteDatabase.add(new Quote("The future belongs to those who believe in the beauty of their dreams.", "Eleanor Roosevelt", "motivation"));
        
        // Success
        quoteDatabase.add(new Quote("Success is not final, failure is not fatal: it is the courage to continue that counts.", "Winston Churchill", "success"));
        quoteDatabase.add(new Quote("Success usually comes to those who are too busy to be looking for it.", "Henry David Thoreau", "success"));
        quoteDatabase.add(new Quote("The way to get started is to quit talking and begin doing.", "Walt Disney", "success"));
        
        // Perseverance
        quoteDatabase.add(new Quote("It does not matter how slowly you go as long as you do not stop.", "Confucius", "perseverance"));
        quoteDatabase.add(new Quote("Perseverance is not a long race; it is many short races one after the other.", "Walter Elliot", "perseverance"));
        quoteDatabase.add(new Quote("Fall seven times, stand up eight.", "Japanese Proverb", "perseverance"));
        
        // Leadership
        quoteDatabase.add(new Quote("A leader is one who knows the way, goes the way, and shows the way.", "John C. Maxwell", "leadership"));
        quoteDatabase.add(new Quote("Leadership is not about being in charge. It's about taking care of those in your charge.", "Simon Sinek", "leadership"));
        quoteDatabase.add(new Quote("The greatest leader is not necessarily the one who does the greatest things. He is the one that gets the people to do the greatest things.", "Ronald Reagan", "leadership"));
        
        // Wisdom
        quoteDatabase.add(new Quote("The only true wisdom is in knowing you know nothing.", "Socrates", "wisdom"));
        quoteDatabase.add(new Quote("In the middle of difficulty lies opportunity.", "Albert Einstein", "wisdom"));
        quoteDatabase.add(new Quote("The journey of a thousand miles begins with one step.", "Lao Tzu", "wisdom"));
        
        // Change
        quoteDatabase.add(new Quote("Be the change that you wish to see in the world.", "Mahatma Gandhi", "change"));
        quoteDatabase.add(new Quote("Change is the only constant in life.", "Heraclitus", "change"));
        quoteDatabase.add(new Quote("Innovation distinguishes between a leader and a follower.", "Steve Jobs", "change"));
        
        // Teamwork
        quoteDatabase.add(new Quote("Alone we can do so little; together we can do so much.", "Helen Keller", "teamwork"));
        quoteDatabase.add(new Quote("Teamwork makes the dream work.", "John C. Maxwell", "teamwork"));
        quoteDatabase.add(new Quote("Coming together is a beginning, staying together is progress, and working together is success.", "Henry Ford", "teamwork"));
        
        // Growth
        quoteDatabase.add(new Quote("The only person you are destined to become is the person you decide to be.", "Ralph Waldo Emerson", "growth"));
        quoteDatabase.add(new Quote("Growth is painful. Change is painful. But nothing is as painful as staying stuck somewhere you don't belong.", "Mandy Hale", "growth"));
        quoteDatabase.add(new Quote("What we learn with pleasure we never forget.", "Alfred Mercier", "growth"));
        
        // Additional inspirational quotes
        quoteDatabase.add(new Quote("Your time is limited, don't waste it living someone else's life.", "Steve Jobs", "motivation"));
        quoteDatabase.add(new Quote("The best time to plant a tree was 20 years ago. The second best time is now.", "Chinese Proverb", "wisdom"));
        quoteDatabase.add(new Quote("Don't watch the clock; do what it does. Keep going.", "Sam Levenson", "perseverance"));
        quoteDatabase.add(new Quote("The secret of getting ahead is getting started.", "Mark Twain", "success"));
        quoteDatabase.add(new Quote("Quality is not an act, it is a habit.", "Aristotle", "growth"));
        
        logMessage("Initialized quote database with " + quoteDatabase.size() + " quotes");
    }
    
    /**
     * Quote data class
     */
    private static class Quote {
        public final String text;
        public final String author;
        public final String category;
        
        public Quote(String text, String author, String category) {
            this.text = text;
            this.author = author;
            this.category = category;
        }
    }
}
