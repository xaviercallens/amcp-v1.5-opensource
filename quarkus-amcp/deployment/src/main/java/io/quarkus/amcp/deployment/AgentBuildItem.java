package io.quarkus.amcp.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item representing a discovered agent class.
 */
public final class AgentBuildItem extends MultiBuildItem {
    
    private final String className;

    public AgentBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return "AgentBuildItem{className='" + className + "'}";
    }
}
