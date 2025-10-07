package io.amcp.examples.chat;

import java.util.*;

/**
 * ManagerAgent - Specialized chat agent focused on productivity, 
 * project management, and organizational efficiency.
 * 
 * Persona: Professional, structured, goal-oriented
 * Focus: Task prioritization, time management, team coordination
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class ManagerAgent extends BaseChatAgent {
    
    private static final String AGENT_NAME = "ManagerAgent";
    private static final String SPECIALTY = "productivity_management";
    
    public ManagerAgent() {
        super(AGENT_NAME);
    }
    
    @Override
    protected String getPersona() {
        return "Professional project manager focused on productivity and efficiency";
    }
    
    @Override
    protected String getSpecialty() {
        return SPECIALTY;
    }
    
    @Override
    protected void subscribeToTopics() {
        subscribe("chat.request.manager.**");
        subscribe("orchestrator.task.chat.manager");
        subscribe("meshchat.manager.**");
        logMessage("Subscribed to manager-specific topics");
    }
    
    @Override
    protected String generateResponse(String query, String conversationId, List<Map<String, Object>> priorMessages) {
        String lowerQuery = query.toLowerCase();
        
        // Analyze query for management-related topics
        if (lowerQuery.contains("priorit") || lowerQuery.contains("urgent") || lowerQuery.contains("important")) {
            return generatePrioritizationAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("deadline") || lowerQuery.contains("schedule") || lowerQuery.contains("timeline")) {
            return generateTimeManagementAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("team") || lowerQuery.contains("delegate") || lowerQuery.contains("collaboration")) {
            return generateTeamCoordinationAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("goal") || lowerQuery.contains("objective") || lowerQuery.contains("target")) {
            return generateGoalSettingAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("meeting") || lowerQuery.contains("standup") || lowerQuery.contains("sync")) {
            return generateMeetingAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("productivity") || lowerQuery.contains("efficient") || lowerQuery.contains("optimize")) {
            return generateProductivityAdvice(query, priorMessages);
        }
        
        // General management advice
        return generateGeneralManagementAdvice(query, priorMessages);
    }
    
    private String generatePrioritizationAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("📊 **Task Prioritization Framework**\n\n");
        
        response.append("Use the Eisenhower Matrix to prioritize:\n\n");
        response.append("**Urgent & Important** → Do First\n");
        response.append("• Critical deadlines\n");
        response.append("• Crisis management\n");
        response.append("• Time-sensitive opportunities\n\n");
        
        response.append("**Important but Not Urgent** → Schedule\n");
        response.append("• Strategic planning\n");
        response.append("• Skill development\n");
        response.append("• Relationship building\n\n");
        
        response.append("**Urgent but Not Important** → Delegate\n");
        response.append("• Interruptions\n");
        response.append("• Some emails/calls\n");
        response.append("• Routine tasks\n\n");
        
        response.append("**Neither Urgent nor Important** → Eliminate\n");
        response.append("• Time wasters\n");
        response.append("• Busy work\n");
        response.append("• Distractions\n\n");
        
        response.append("💡 **Pro Tip:** Review priorities daily and adjust as needed.");
        
        return response.toString();
    }
    
    private String generateTimeManagementAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("⏰ **Time Management Strategies**\n\n");
        
        response.append("**Time Blocking Technique:**\n");
        response.append("• Dedicate specific time blocks to tasks\n");
        response.append("• Include buffer time between blocks\n");
        response.append("• Protect deep work periods\n\n");
        
        response.append("**Pomodoro Method:**\n");
        response.append("• Work in 25-minute focused intervals\n");
        response.append("• Take 5-minute breaks\n");
        response.append("• Longer break after 4 pomodoros\n\n");
        
        response.append("**Meeting Efficiency:**\n");
        response.append("• Set clear agendas\n");
        response.append("• Time-box discussions\n");
        response.append("• End with action items\n\n");
        
        response.append("💡 **Remember:** Time is your most valuable resource. Guard it carefully.");
        
        return response.toString();
    }
    
    private String generateTeamCoordinationAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("👥 **Team Coordination Best Practices**\n\n");
        
        response.append("**Clear Communication:**\n");
        response.append("• Set expectations upfront\n");
        response.append("• Use appropriate channels\n");
        response.append("• Provide context and rationale\n\n");
        
        response.append("**Effective Delegation:**\n");
        response.append("• Match tasks to strengths\n");
        response.append("• Provide necessary resources\n");
        response.append("• Trust but verify progress\n\n");
        
        response.append("**Collaboration Tools:**\n");
        response.append("• Shared project boards\n");
        response.append("• Regular status updates\n");
        response.append("• Centralized documentation\n\n");
        
        response.append("💡 **Key Insight:** Empower your team with autonomy while maintaining accountability.");
        
        return response.toString();
    }
    
    private String generateGoalSettingAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("🎯 **SMART Goal Framework**\n\n");
        
        response.append("**Specific:** Clearly define what you want to achieve\n");
        response.append("**Measurable:** Include quantifiable metrics\n");
        response.append("**Achievable:** Ensure it's realistic\n");
        response.append("**Relevant:** Align with broader objectives\n");
        response.append("**Time-bound:** Set clear deadlines\n\n");
        
        response.append("**Goal Breakdown:**\n");
        response.append("• Break large goals into milestones\n");
        response.append("• Create actionable sub-tasks\n");
        response.append("• Track progress regularly\n\n");
        
        response.append("💡 **Success Tip:** Review and adjust goals quarterly based on progress and changing priorities.");
        
        return response.toString();
    }
    
    private String generateMeetingAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("📅 **Effective Meeting Management**\n\n");
        
        response.append("**Before the Meeting:**\n");
        response.append("• Send agenda 24 hours ahead\n");
        response.append("• Invite only necessary participants\n");
        response.append("• Share pre-read materials\n\n");
        
        response.append("**During the Meeting:**\n");
        response.append("• Start and end on time\n");
        response.append("• Stick to the agenda\n");
        response.append("• Capture decisions and action items\n\n");
        
        response.append("**After the Meeting:**\n");
        response.append("• Send summary within 24 hours\n");
        response.append("• Assign clear owners to action items\n");
        response.append("• Follow up on commitments\n\n");
        
        response.append("💡 **Rule of Thumb:** If it can be an email, skip the meeting.");
        
        return response.toString();
    }
    
    private String generateProductivityAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("⚡ **Productivity Optimization**\n\n");
        
        response.append("**Focus Management:**\n");
        response.append("• Eliminate distractions\n");
        response.append("• Use 'Do Not Disturb' modes\n");
        response.append("• Batch similar tasks together\n\n");
        
        response.append("**Energy Management:**\n");
        response.append("• Schedule demanding work during peak hours\n");
        response.append("• Take regular breaks\n");
        response.append("• Maintain work-life balance\n\n");
        
        response.append("**Automation & Tools:**\n");
        response.append("• Automate repetitive tasks\n");
        response.append("• Use templates and checklists\n");
        response.append("• Leverage productivity apps\n\n");
        
        response.append("💡 **Productivity Principle:** Work smarter, not harder. Focus on high-impact activities.");
        
        return response.toString();
    }
    
    private String generateGeneralManagementAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("💼 **Management Essentials**\n\n");
        
        response.append("As a manager, focus on:\n\n");
        
        response.append("**Strategic Thinking:**\n");
        response.append("• Align tasks with organizational goals\n");
        response.append("• Anticipate challenges and opportunities\n");
        response.append("• Make data-driven decisions\n\n");
        
        response.append("**People Development:**\n");
        response.append("• Provide regular feedback\n");
        response.append("• Invest in team growth\n");
        response.append("• Recognize achievements\n\n");
        
        response.append("**Continuous Improvement:**\n");
        response.append("• Reflect on processes regularly\n");
        response.append("• Seek feedback from team\n");
        response.append("• Adapt to changing circumstances\n\n");
        
        response.append("💡 **Leadership Insight:** Great managers create environments where teams can thrive.");
        
        return response.toString();
    }
}
