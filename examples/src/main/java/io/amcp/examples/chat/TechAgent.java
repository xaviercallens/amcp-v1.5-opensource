package io.amcp.examples.chat;

import java.util.*;

/**
 * TechAgent - Specialized chat agent focused on tools, automation,
 * and technical problem-solving.
 * 
 * Persona: Technical expert, practical, solution-oriented
 * Focus: Development tools, automation, technical best practices
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class TechAgent extends BaseChatAgent {
    
    private static final String AGENT_NAME = "TechAgent";
    private static final String SPECIALTY = "technical_automation";
    
    public TechAgent() {
        super(AGENT_NAME);
    }
    
    @Override
    protected String getPersona() {
        return "Technical expert focused on tools, automation, and engineering excellence";
    }
    
    @Override
    protected String getSpecialty() {
        return SPECIALTY;
    }
    
    @Override
    protected void subscribeToTopics() {
        subscribe("chat.request.tech.**");
        subscribe("orchestrator.task.chat.tech");
        subscribe("meshchat.tech.**");
        logMessage("Subscribed to tech-specific topics");
    }
    
    @Override
    protected String generateResponse(String query, String conversationId, List<Map<String, Object>> priorMessages) {
        String lowerQuery = query.toLowerCase();
        
        // Analyze query for tech-related topics
        if (lowerQuery.contains("automat") || lowerQuery.contains("script") || lowerQuery.contains("ci/cd")) {
            return generateAutomationAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("tool") || lowerQuery.contains("software") || lowerQuery.contains("app")) {
            return generateToolRecommendations(query, priorMessages);
        }
        
        if (lowerQuery.contains("debug") || lowerQuery.contains("error") || lowerQuery.contains("bug")) {
            return generateDebuggingAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("performance") || lowerQuery.contains("optimize") || lowerQuery.contains("speed")) {
            return generatePerformanceAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("security") || lowerQuery.contains("vulnerab") || lowerQuery.contains("secure")) {
            return generateSecurityAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("architecture") || lowerQuery.contains("design pattern") || lowerQuery.contains("structure")) {
            return generateArchitectureAdvice(query, priorMessages);
        }
        
        if (lowerQuery.contains("test") || lowerQuery.contains("quality") || lowerQuery.contains("qa")) {
            return generateTestingAdvice(query, priorMessages);
        }
        
        // General technical advice
        return generateGeneralTechAdvice(query, priorMessages);
    }
    
    private String generateAutomationAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("🤖 **Automation Best Practices**\n\n");
        
        response.append("**CI/CD Pipeline:**\n");
        response.append("• Automate builds on every commit\n");
        response.append("• Run automated tests before deployment\n");
        response.append("• Use infrastructure as code (IaC)\n");
        response.append("• Implement automated rollbacks\n\n");
        
        response.append("**Workflow Automation:**\n");
        response.append("• Identify repetitive manual tasks\n");
        response.append("• Start with high-frequency, low-complexity tasks\n");
        response.append("• Use GitHub Actions, Jenkins, or GitLab CI\n");
        response.append("• Document automation scripts thoroughly\n\n");
        
        response.append("**Monitoring & Alerts:**\n");
        response.append("• Set up automated health checks\n");
        response.append("• Configure intelligent alerting\n");
        response.append("• Automate log aggregation\n\n");
        
        response.append("⚡ **Automation Rule:** If you do it more than twice, automate it.");
        
        return response.toString();
    }
    
    private String generateToolRecommendations(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("🛠️ **Essential Development Tools**\n\n");
        
        response.append("**Version Control:**\n");
        response.append("• Git + GitHub/GitLab/Bitbucket\n");
        response.append("• Use branching strategies (Git Flow, Trunk-Based)\n");
        response.append("• Leverage pull request reviews\n\n");
        
        response.append("**IDE & Editors:**\n");
        response.append("• VS Code (lightweight, extensible)\n");
        response.append("• IntelliJ IDEA (Java/Kotlin)\n");
        response.append("• PyCharm (Python)\n");
        response.append("• Use code formatters and linters\n\n");
        
        response.append("**Collaboration:**\n");
        response.append("• Slack/Discord for team communication\n");
        response.append("• Jira/Linear for issue tracking\n");
        response.append("• Notion/Confluence for documentation\n");
        response.append("• Miro/Figma for design collaboration\n\n");
        
        response.append("**DevOps:**\n");
        response.append("• Docker for containerization\n");
        response.append("• Kubernetes for orchestration\n");
        response.append("• Terraform for infrastructure\n");
        response.append("• Prometheus/Grafana for monitoring\n\n");
        
        response.append("💡 **Tool Selection Tip:** Choose tools that integrate well with your existing stack.");
        
        return response.toString();
    }
    
    private String generateDebuggingAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("🔍 **Systematic Debugging Approach**\n\n");
        
        response.append("**1. Reproduce the Issue:**\n");
        response.append("• Create minimal reproducible example\n");
        response.append("• Document exact steps to trigger bug\n");
        response.append("• Note environment details\n\n");
        
        response.append("**2. Isolate the Problem:**\n");
        response.append("• Use binary search to narrow scope\n");
        response.append("• Add strategic logging/breakpoints\n");
        response.append("• Check recent changes (git blame)\n\n");
        
        response.append("**3. Form Hypothesis:**\n");
        response.append("• What could cause this behavior?\n");
        response.append("• Review assumptions\n");
        response.append("• Consider edge cases\n\n");
        
        response.append("**4. Test & Verify:**\n");
        response.append("• Test your hypothesis\n");
        response.append("• Verify the fix works\n");
        response.append("• Add regression test\n\n");
        
        response.append("**Debugging Tools:**\n");
        response.append("• IDE debuggers (breakpoints, watches)\n");
        response.append("• Console logging (structured logs)\n");
        response.append("• Network inspection (browser DevTools)\n");
        response.append("• Profilers for performance issues\n\n");
        
        response.append("🐛 **Remember:** The bug is always in your code, not the compiler.");
        
        return response.toString();
    }
    
    private String generatePerformanceAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("⚡ **Performance Optimization**\n\n");
        
        response.append("**Measure First:**\n");
        response.append("• Profile before optimizing\n");
        response.append("• Identify actual bottlenecks\n");
        response.append("• Set performance budgets\n");
        response.append("• Use benchmarking tools\n\n");
        
        response.append("**Common Optimizations:**\n");
        response.append("• Cache frequently accessed data\n");
        response.append("• Use lazy loading for resources\n");
        response.append("• Optimize database queries (indexes, N+1)\n");
        response.append("• Implement pagination for large datasets\n");
        response.append("• Use CDNs for static assets\n\n");
        
        response.append("**Code-Level:**\n");
        response.append("• Avoid premature optimization\n");
        response.append("• Use appropriate data structures\n");
        response.append("• Minimize I/O operations\n");
        response.append("• Consider async/parallel processing\n\n");
        
        response.append("💡 **Optimization Principle:** Make it work, make it right, make it fast - in that order.");
        
        return response.toString();
    }
    
    private String generateSecurityAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("🔒 **Security Best Practices**\n\n");
        
        response.append("**Authentication & Authorization:**\n");
        response.append("• Use OAuth 2.0 / OpenID Connect\n");
        response.append("• Implement multi-factor authentication\n");
        response.append("• Apply principle of least privilege\n");
        response.append("• Rotate credentials regularly\n\n");
        
        response.append("**Data Protection:**\n");
        response.append("• Encrypt data at rest and in transit (TLS/SSL)\n");
        response.append("• Never store passwords in plain text\n");
        response.append("• Use environment variables for secrets\n");
        response.append("• Implement proper input validation\n\n");
        
        response.append("**Secure Coding:**\n");
        response.append("• Sanitize user inputs (prevent XSS, SQL injection)\n");
        response.append("• Use parameterized queries\n");
        response.append("• Keep dependencies updated\n");
        response.append("• Run security scanners (Snyk, OWASP)\n\n");
        
        response.append("**Monitoring:**\n");
        response.append("• Log security events\n");
        response.append("• Monitor for suspicious activity\n");
        response.append("• Have incident response plan\n\n");
        
        response.append("🛡️ **Security Mindset:** Assume breach - design for defense in depth.");
        
        return response.toString();
    }
    
    private String generateArchitectureAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("🏗️ **Software Architecture Principles**\n\n");
        
        response.append("**SOLID Principles:**\n");
        response.append("• Single Responsibility Principle\n");
        response.append("• Open/Closed Principle\n");
        response.append("• Liskov Substitution Principle\n");
        response.append("• Interface Segregation Principle\n");
        response.append("• Dependency Inversion Principle\n\n");
        
        response.append("**Design Patterns:**\n");
        response.append("• Factory for object creation\n");
        response.append("• Observer for event handling\n");
        response.append("• Strategy for algorithm selection\n");
        response.append("• Repository for data access\n\n");
        
        response.append("**Architecture Styles:**\n");
        response.append("• Microservices for scalability\n");
        response.append("• Event-driven for loose coupling\n");
        response.append("• Layered for separation of concerns\n");
        response.append("• Hexagonal for testability\n\n");
        
        response.append("💡 **Architecture Wisdom:** Good architecture enables change, not prevents it.");
        
        return response.toString();
    }
    
    private String generateTestingAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("✅ **Testing Strategy**\n\n");
        
        response.append("**Test Pyramid:**\n");
        response.append("• Unit Tests (70%) - Fast, isolated\n");
        response.append("• Integration Tests (20%) - Component interaction\n");
        response.append("• E2E Tests (10%) - Full user flows\n\n");
        
        response.append("**Best Practices:**\n");
        response.append("• Write tests before fixing bugs (TDD)\n");
        response.append("• Test behavior, not implementation\n");
        response.append("• Keep tests independent and idempotent\n");
        response.append("• Use meaningful test names\n");
        response.append("• Aim for high coverage on critical paths\n\n");
        
        response.append("**Testing Tools:**\n");
        response.append("• JUnit/TestNG (Java)\n");
        response.append("• pytest (Python)\n");
        response.append("• Jest (JavaScript)\n");
        response.append("• Selenium/Playwright (E2E)\n\n");
        
        response.append("🧪 **Testing Mantra:** If it's not tested, it's broken.");
        
        return response.toString();
    }
    
    private String generateGeneralTechAdvice(String query, List<Map<String, Object>> priorMessages) {
        StringBuilder response = new StringBuilder();
        response.append("💻 **Technical Excellence**\n\n");
        
        response.append("**Code Quality:**\n");
        response.append("• Write clean, readable code\n");
        response.append("• Follow team coding standards\n");
        response.append("• Refactor regularly\n");
        response.append("• Document complex logic\n\n");
        
        response.append("**Continuous Learning:**\n");
        response.append("• Stay updated with tech trends\n");
        response.append("• Contribute to open source\n");
        response.append("• Read technical blogs and books\n");
        response.append("• Experiment with new technologies\n\n");
        
        response.append("**Collaboration:**\n");
        response.append("• Conduct thorough code reviews\n");
        response.append("• Share knowledge with team\n");
        response.append("• Document architectural decisions\n");
        response.append("• Pair program on complex features\n\n");
        
        response.append("⚙️ **Engineering Philosophy:** Build for maintainability, not just functionality.");
        
        return response.toString();
    }
}
