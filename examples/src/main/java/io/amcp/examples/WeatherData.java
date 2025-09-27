package io.amcp.examples.weather;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable weather data model representing current weather conditions for a specific location.
 * Includes severe weather detection and alert level classification capabilities.
 */
public class WeatherData {
    
    public enum AlertLevel {
        LOW, MEDIUM, HIGH
    }
    
    private final String locationId;
    private final String locationName;
    private final double temperature;
    private final double humidity;
    private final double pressure;
    private final String conditions;
    private final double windSpeed;
    private final String windDirection;
    private final Instant timestamp;
    private final String source;
    
    private WeatherData(Builder builder) {
        this.locationId = builder.locationId;
        this.locationName = builder.locationName;
        this.temperature = builder.temperature;
        this.humidity = builder.humidity;
        this.pressure = builder.pressure;
        this.conditions = builder.conditions;
        this.windSpeed = builder.windSpeed;
        this.windDirection = builder.windDirection;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.source = builder.source;
    }
    
    // Getters
    public String getLocationId() { return locationId; }
    public String getLocationName() { return locationName; }
    public double getTemperature() { return temperature; }
    public double getHumidity() { return humidity; }
    public double getPressure() { return pressure; }
    public String getConditions() { return conditions; }
    public double getWindSpeed() { return windSpeed; }
    public String getWindDirection() { return windDirection; }
    public Instant getTimestamp() { return timestamp; }
    public String getSource() { return source; }
    
    /**
     * Determines if current weather conditions constitute severe weather
     */
    public boolean isSevereWeather() {
        // Extreme temperature conditions
        if (temperature > 40.0 || temperature < -20.0) {
            return true;
        }
        
        // High wind conditions
        if (windSpeed > 25.0) {
            return true;
        }
        
        // Severe weather conditions in description
        String conditionsLower = conditions != null ? conditions.toLowerCase() : "";
        if (conditionsLower.contains("storm") || 
            conditionsLower.contains("hurricane") ||
            conditionsLower.contains("tornado") ||
            conditionsLower.contains("extreme") ||
            conditionsLower.contains("severe")) {
            return true;
        }
        
        // Very low pressure (potential storm system)
        if (pressure < 980.0) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Classifies the alert level based on weather conditions
     */
    public AlertLevel getAlertLevel() {
        if (isSevereWeather()) {
            // High alert for extreme conditions
            if (temperature > 45.0 || temperature < -30.0 || windSpeed > 35.0) {
                return AlertLevel.HIGH;
            }
            // Medium alert for severe but not extreme conditions
            return AlertLevel.MEDIUM;
        }
        
        // Low alert for concerning but not severe conditions
        if (temperature > 35.0 || temperature < -10.0 || windSpeed > 20.0) {
            return AlertLevel.MEDIUM;
        }
        
        return AlertLevel.LOW;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String locationId;
        private String locationName;
        private double temperature;
        private double humidity;
        private double pressure;
        private String conditions;
        private double windSpeed;
        private String windDirection;
        private Instant timestamp;
        private String source;
        
        public Builder locationId(String locationId) {
            this.locationId = locationId;
            return this;
        }
        
        public Builder locationName(String locationName) {
            this.locationName = locationName;
            return this;
        }
        
        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder humidity(double humidity) {
            this.humidity = humidity;
            return this;
        }
        
        public Builder pressure(double pressure) {
            this.pressure = pressure;
            return this;
        }
        
        public Builder conditions(String conditions) {
            this.conditions = conditions;
            return this;
        }
        
        public Builder windSpeed(double windSpeed) {
            this.windSpeed = windSpeed;
            return this;
        }
        
        public Builder windDirection(String windDirection) {
            this.windDirection = windDirection;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder source(String source) {
            this.source = source;
            return this;
        }
        
        public WeatherData build() {
            return new WeatherData(this);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeatherData that = (WeatherData) o;
        return Double.compare(that.temperature, temperature) == 0 &&
               Double.compare(that.humidity, humidity) == 0 &&
               Double.compare(that.pressure, pressure) == 0 &&
               Double.compare(that.windSpeed, windSpeed) == 0 &&
               Objects.equals(locationId, that.locationId) &&
               Objects.equals(locationName, that.locationName) &&
               Objects.equals(conditions, that.conditions) &&
               Objects.equals(windDirection, that.windDirection) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(source, that.source);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(locationId, locationName, temperature, humidity, pressure, 
                          conditions, windSpeed, windDirection, timestamp, source);
    }
    
    @Override
    public String toString() {
        return String.format("WeatherData{%s: %.1f°C, %s, Wind: %.1f km/h %s}", 
                           locationName, temperature, conditions, windSpeed, windDirection);
    }
}