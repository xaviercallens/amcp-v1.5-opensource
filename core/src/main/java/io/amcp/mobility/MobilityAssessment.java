package io.amcp.mobility;

import java.io.Serializable;

/**
 * Assessment of mobility feasibility.
 */
public class MobilityAssessment implements Serializable {
    private final boolean feasible;
    private final double confidence;
    private final String recommendation;
    
    public MobilityAssessment(boolean feasible, double confidence, String recommendation) {
        this.feasible = feasible;
        this.confidence = confidence;
        this.recommendation = recommendation;
    }
    
    public boolean isFeasible() { return feasible; }
    public double getConfidence() { return confidence; }
    public String getRecommendation() { return recommendation; }
}