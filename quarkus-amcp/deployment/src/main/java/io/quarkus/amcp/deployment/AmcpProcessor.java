package io.quarkus.amcp.deployment;

import io.amcp.core.AbstractMobileAgent;
import io.quarkus.amcp.runtime.AmcpConfig;
import io.quarkus.amcp.runtime.AmcpContextProducer;
import io.quarkus.amcp.runtime.AmcpRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Build-time processor for the AMCP Quarkus extension.
 * Scans for agent classes and prepares them for runtime initialization.
 */
public class AmcpProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AmcpProcessor.class);
    
    private static final String FEATURE = "amcp";
    private static final DotName AGENT_BASE = DotName.createSimple(AbstractMobileAgent.class.getName());

    /**
     * Registers the AMCP feature.
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Registers the AgentContext producer as a CDI bean.
     */
    @BuildStep
    AdditionalBeanBuildItem registerContextProducer() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(AmcpContextProducer.class)
                .setUnremovable()
                .build();
    }

    /**
     * Discovers agent classes and registers them for reflection.
     */
    @BuildStep
    void discoverAgents(CombinedIndexBuildItem index,
                        BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
                        BuildProducer<AgentBuildItem> agentProducer) {
        
        logger.info("Scanning for AMCP agent classes...");
        
        // Find all subclasses of AbstractMobileAgent
        List<ClassInfo> agents = new ArrayList<>();
        
        // Get direct subclasses
        agents.addAll(index.getIndex().getAllKnownSubclasses(AGENT_BASE));
        
        // Get implementors if AbstractMobileAgent becomes an interface
        agents.addAll(index.getIndex().getAllKnownImplementors(AGENT_BASE));
        
        logger.info("Found {} agent class(es)", agents.size());
        
        for (ClassInfo agentInfo : agents) {
            // Skip abstract classes
            if (java.lang.reflect.Modifier.isAbstract(agentInfo.flags())) {
                logger.debug("Skipping abstract agent class: {}", agentInfo.name());
                continue;
            }
            
            String className = agentInfo.name().toString();
            logger.info("Discovered agent: {}", className);
            
            // Register for reflection (required for native image)
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(className)
                    .constructors(true)
                    .methods(true)
                    .fields(true)
                    .build());
            
            // Produce an agent build item
            agentProducer.produce(new AgentBuildItem(className));
        }
    }

    /**
     * Initializes the agent mesh at runtime.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void initializeAgentMesh(AmcpRecorder recorder,
                             AmcpConfig config,
                             List<AgentBuildItem> agents) {
        
        // Collect agent class names
        List<String> agentClassNames = new ArrayList<>();
        for (AgentBuildItem agent : agents) {
            agentClassNames.add(agent.getClassName());
        }
        
        logger.info("Initializing AMCP with {} agent(s)", agentClassNames.size());
        
        // Initialize the agent mesh and register shutdown hook
        recorder.initAgentMesh(config, agentClassNames);
        
        logger.info("AMCP initialization complete");
    }
}
