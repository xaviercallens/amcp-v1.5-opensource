---
name: Bug Report
about: Report a bug to help us improve AMCP v1.5
title: '[BUG] '
labels: ['bug', 'needs-triage']
assignees: ''
---

## Bug Description
A clear and concise description of what the bug is.

## Steps to Reproduce
Steps to reproduce the behavior:
1. Configure agent with '...'
2. Send event '...'
3. Subscribe to topic '...'
4. See error

## Expected Behavior
A clear and concise description of what you expected to happen.

## Actual Behavior
A clear and concise description of what actually happened.

## Environment
- **AMCP Version**: [e.g., v1.5.0]
- **Java Version**: [e.g., OpenJDK 8, Oracle JDK 11]
- **Operating System**: [e.g., macOS 13.0, Ubuntu 20.04, Windows 11]
- **Maven Version**: [e.g., 3.8.6]
- **Event Broker**: [e.g., InMemoryEventBroker, Kafka]

## Code Example
```java
// Minimal code example that demonstrates the issue
public class ReproduceIssue {
    public static void main(String[] args) {
        // Your code here
    }
}
```

## Stack Trace (if applicable)
```
Paste the complete stack trace here
```

## Configuration Files
```properties
# amcp.properties or other relevant configuration
key=value
```

## Log Output
```
[timestamp] [level] [logger] Message
```

## Screenshots (if applicable)
If applicable, add screenshots to help explain your problem.

## Additional Context
Add any other context about the problem here:
- Does this happen consistently or intermittently?
- Any workarounds you've found?
- Related issues or discussions?
- Performance impact observed?

## Checklist
- [ ] I have searched for existing issues
- [ ] I have included a minimal code example
- [ ] I have provided complete environment information
- [ ] I have included relevant log output/stack traces
- [ ] I can reproduce this issue consistently