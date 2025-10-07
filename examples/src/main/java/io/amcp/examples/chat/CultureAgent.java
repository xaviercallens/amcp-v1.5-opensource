package io.amcp.examples.chat;

import java.util.*;

/**
 * CultureAgent - Specialized chat agent focused on team morale,
 * company culture, and employee well-being.
 * 
 * Persona: Empathetic, supportive, people-focused
 * Focus: Team building, morale, work-life balance, recognition
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class CultureAgent extends BaseChatAgent {
    
    private static final String AGENT_NAME = "CultureAgent";
    private static final String SPECIALTY = "team_culture";
    
    public CultureAgent() {
        super(AGENT_NAME);
    }
    
    @Override
    protected String getPersona() {
        return "Empathetic culture advocate focused on team well-being and morale";
    }
    
    @Override
    protected String getSpecialty() {
        return SPECIALTY;
    }
    
    @Override
    protected void subscribeToTopics() {
        subscribe("chat.request.culture.**");
        subscribe("orchestrator.task.chat.culture");
        subscribe("meshchat.culture.**");
        logMessage("Subscribed to culture-specific topics");
    }
    
    @Override
    protected String generateResponse(String query, String conversationId, List<Map<String, Object>> priorMessages) {
        String lowerQuery = query.toLowerCase();
        
        // Analyze query for culture-related topics
        if (lowerQuery.contains("morale") || lowerQuery.contains("motivation") || lowerQuery.contains("engagement")) {
            return generateMoraleAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("team building") || lowerQuery.contains("bonding") || lowerQuery.contains("activity")) {
            return generateTeamBuildingAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("recognition") || lowerQuery.contains("appreciate") || lowerQuery.contains("celebrate")) {
            return generateRecognitionAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("work-life") || lowerQuery.contains("balance") || lowerQuery.contains("burnout")) {
            return generateWorkLifeBalanceAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("diversity") || lowerQuery.contains("inclusion") || lowerQuery.contains("belonging")) {
            return generateDiversityAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("feedback") || lowerQuery.contains("communication") || lowerQuery.contains("conflict")) {
            return generateCommunicationAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("onboard") || lowerQuery.contains("new hire") || lowerQuery.contains("welcome")) {
            return generateOnboardingAdvice(query, priorMessages);
        }
        
        // General culture advice
        return generateGeneralCultureAdvice(query, priorMessages);
    }
    
    private String generateMoraleAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("🌟 **Boosting Team Morale**\n\n");
        
        response.append("**Create Positive Environment:**\n");
        response.append("• Celebrate small wins regularly\n");
        response.append("• Foster open and honest communication\n");
        response.append("• Encourage peer-to-peer recognition\n");
        response.append("• Lead with empathy and authenticity\n\n");
        
        response.append("**Address Challenges:**\n");
        response.append("• Listen actively to team concerns\n");
        response.append("• Be transparent about changes\n");
        response.append("• Provide support during difficult times\n");
        response.append("• Remove blockers and obstacles\n\n");
        
        response.append("**Engagement Activities:**\n");
        response.append("• Regular team check-ins\n");
        response.append("• Virtual or in-person social events\n");
        response.append("• Recognition programs\n");
        response.append("• Professional development opportunities\n\n");
        
        response.append("💙 **Remember:** High morale comes from feeling valued, heard, and supported.");
        
        return response.toString();
    }
    
    private String generateTeamBuildingAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("🤝 **Team Building Activities**\n\n");
        
        response.append("**Virtual Team Building:**\n");
        response.append("• Online trivia or game nights\n");
        response.append("• Virtual coffee chats (random pairing)\n");
        response.append("• Show & tell sessions\n");
        response.append("• Collaborative online workshops\n\n");
        
        response.append("**In-Person Activities:**\n");
        response.append("• Escape room challenges\n");
        response.append("• Volunteer together for a cause\n");
        response.append("• Team lunches or dinners\n");
        response.append("• Sports or outdoor activities\n\n");
        
        response.append("**Ongoing Practices:**\n");
        response.append("• Start meetings with personal check-ins\n");
        response.append("• Create Slack channels for hobbies\n");
        response.append("• Share team wins in all-hands\n");
        response.append("• Rotate meeting facilitators\n\n");
        
        response.append("🎯 **Goal:** Build trust and psychological safety within the team.");
        
        return response.toString();
    }
    
    private String generateRecognitionAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("🏆 **Recognition & Appreciation**\n\n");
        
        response.append("**Types of Recognition:**\n");
        response.append("• Public praise in team meetings\n");
        response.append("• Written thank-you notes\n");
        response.append("• Spot bonuses or gift cards\n");
        response.append("• Extra time off\n");
        response.append("• Career advancement opportunities\n\n");
        
        response.append("**Make it Meaningful:**\n");
        response.append("• Be specific about what they did well\n");
        response.append("• Recognize effort, not just results\n");
        response.append("• Deliver recognition timely\n");
        response.append("• Personalize to individual preferences\n\n");
        
        response.append("**Peer Recognition:**\n");
        response.append("• Implement kudos channels\n");
        response.append("• Create recognition programs\n");
        response.append("• Encourage team nominations\n");
        response.append("• Share success stories\n\n");
        
        response.append("✨ **Impact:** Regular recognition increases engagement by 2-3x.");
        
        return response.toString();
    }
    
    private String generateWorkLifeBalanceAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("⚖️ **Work-Life Balance**\n\n");
        
        response.append("**Set Boundaries:**\n");
        response.append("• Define clear working hours\n");
        response.append("• Avoid after-hours messages\n");
        response.append("• Respect time off and vacations\n");
        response.append("• Lead by example from leadership\n\n");
        
        response.append("**Prevent Burnout:**\n");
        response.append("• Watch for warning signs (fatigue, cynicism)\n");
        response.append("• Encourage regular breaks\n");
        response.append("• Promote use of PTO\n");
        response.append("• Redistribute workload when needed\n\n");
        
        response.append("**Flexible Work:**\n");
        response.append("• Offer flexible hours when possible\n");
        response.append("• Support remote/hybrid options\n");
        response.append("• Focus on outcomes, not hours\n");
        response.append("• Accommodate personal needs\n\n");
        
        response.append("**Wellness Programs:**\n");
        response.append("• Mental health resources\n");
        response.append("• Fitness benefits or stipends\n");
        response.append("• Meditation or mindfulness sessions\n");
        response.append("• Employee assistance programs\n\n");
        
        response.append("🌱 **Philosophy:** Sustainable performance requires sustainable practices.");
        
        return response.toString();
    }
    
    private String generateDiversityAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("🌈 **Diversity, Equity & Inclusion**\n\n");
        
        response.append("**Create Inclusive Environment:**\n");
        response.append("• Value diverse perspectives\n");
        response.append("• Challenge unconscious bias\n");
        response.append("• Use inclusive language\n");
        response.append("• Ensure equal opportunities\n\n");
        
        response.append("**Diverse Hiring:**\n");
        response.append("• Expand candidate sourcing\n");
        response.append("• Use structured interviews\n");
        response.append("• Diverse interview panels\n");
        response.append("• Remove bias from job descriptions\n\n");
        
        response.append("**Belonging Initiatives:**\n");
        response.append("• Employee resource groups (ERGs)\n");
        response.append("• Cultural awareness training\n");
        response.append("• Celebrate diverse holidays\n");
        response.append("• Amplify underrepresented voices\n\n");
        
        response.append("💡 **Truth:** Diverse teams are more innovative and perform better.");
        
        return response.toString();
    }
    
    private String generateCommunicationAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("💬 **Effective Team Communication**\n\n");
        
        response.append("**Feedback Culture:**\n");
        response.append("• Give feedback regularly, not just annually\n");
        response.append("• Use SBI model (Situation-Behavior-Impact)\n");
        response.append("• Balance positive and constructive feedback\n");
        response.append("• Create safe space for receiving feedback\n\n");
        
        response.append("**Conflict Resolution:**\n");
        response.append("• Address conflicts early\n");
        response.append("• Focus on issues, not personalities\n");
        response.append("• Listen to understand, not to respond\n");
        response.append("• Find win-win solutions\n\n");
        
        response.append("**Transparent Communication:**\n");
        response.append("• Share company updates regularly\n");
        response.append("• Explain the 'why' behind decisions\n");
        response.append("• Admit mistakes and learn from them\n");
        response.append("• Encourage questions and dialogue\n\n");
        
        response.append("🗣️ **Key:** Psychological safety enables honest communication.");
        
        return response.toString();
    }
    
    private String generateOnboardingAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("👋 **Effective Onboarding**\n\n");
        
        response.append("**Pre-Day One:**\n");
        response.append("• Send welcome package\n");
        response.append("• Set up equipment and accounts\n");
        response.append("• Share onboarding schedule\n");
        response.append("• Assign onboarding buddy\n\n");
        
        response.append("**First Week:**\n");
        response.append("• Team introductions and meet & greets\n");
        response.append("• Overview of company culture and values\n");
        response.append("• Set up 1-on-1s with key stakeholders\n");
        response.append("• Provide clear initial tasks\n\n");
        
        response.append("**First 30-60-90 Days:**\n");
        response.append("• Regular check-ins with manager\n");
        response.append("• Gradual increase in responsibilities\n");
        response.append("• Gather feedback on onboarding experience\n");
        response.append("• Celebrate early wins\n\n");
        
        response.append("**Best Practices:**\n");
        response.append("• Make them feel welcome from day one\n");
        response.append("• Provide clear expectations\n");
        response.append("• Encourage questions\n");
        response.append("• Connect them with the team\n\n");
        
        response.append("🚀 **Impact:** Great onboarding increases retention by 82%.");
        
        return response.toString();
    }
    
    private String generateGeneralCultureAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("🏢 **Building Strong Culture**\n\n");
        
        response.append("**Core Elements:**\n");
        response.append("• Clear values and mission\n");
        response.append("• Psychological safety\n");
        response.append("• Trust and transparency\n");
        response.append("• Growth mindset\n\n");
        
        response.append("**Culture Practices:**\n");
        response.append("• Regular team rituals\n");
        response.append("• Open door policy\n");
        response.append("• Learning and development focus\n");
        response.append("• Work-life integration\n\n");
        
        response.append("**Measure & Improve:**\n");
        response.append("• Conduct regular engagement surveys\n");
        response.append("• Act on feedback received\n");
        response.append("• Track culture metrics\n");
        response.append("• Iterate based on results\n\n");
        
        response.append("💫 **Culture Truth:** Culture is what people do when no one is watching.");
        
        return response.toString();
    }
}
