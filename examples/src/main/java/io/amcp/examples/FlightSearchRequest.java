package io.amcp.examples.travel;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Request object for flight search operations.
 * Follows AMCP patterns for data exchange between agents.
 */
public class FlightSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String origin;
    private final String destination;
    private final LocalDate departureDate;
    private final LocalDate returnDate;
    private final int adults;
    private final int children;
    private final String cabinClass;
    private final boolean nonStop;
    private final String currencyCode;
    private final int maxResults;
    
    private FlightSearchRequest(Builder builder) {
        this.origin = builder.origin;
        this.destination = builder.destination;
        this.departureDate = builder.departureDate;
        this.returnDate = builder.returnDate;
        this.adults = builder.adults;
        this.children = builder.children;
        this.cabinClass = builder.cabinClass;
        this.nonStop = builder.nonStop;
        this.currencyCode = builder.currencyCode;
        this.maxResults = builder.maxResults;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String origin;
        private String destination;
        private LocalDate departureDate;
        private LocalDate returnDate;
        private int adults = 1;
        private int children = 0;
        private String cabinClass = "ECONOMY";
        private boolean nonStop = false;
        private String currencyCode = "EUR";
        private int maxResults = 10;
        
        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }
        
        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }
        
        public Builder departureDate(LocalDate departureDate) {
            this.departureDate = departureDate;
            return this;
        }
        
        public Builder returnDate(LocalDate returnDate) {
            this.returnDate = returnDate;
            return this;
        }
        
        public Builder adults(int adults) {
            this.adults = adults;
            return this;
        }
        
        public Builder children(int children) {
            this.children = children;
            return this;
        }
        
        public Builder cabinClass(String cabinClass) {
            this.cabinClass = cabinClass;
            return this;
        }
        
        public Builder nonStop(boolean nonStop) {
            this.nonStop = nonStop;
            return this;
        }
        
        public Builder currencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
            return this;
        }
        
        public Builder maxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }
        
        public FlightSearchRequest build() {
            if (origin == null || origin.trim().isEmpty()) {
                throw new IllegalArgumentException("Origin is required");
            }
            if (destination == null || destination.trim().isEmpty()) {
                throw new IllegalArgumentException("Destination is required");
            }
            if (departureDate == null) {
                throw new IllegalArgumentException("Departure date is required");
            }
            return new FlightSearchRequest(this);
        }
    }
    
    // Getters
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public LocalDate getDepartureDate() { return departureDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public int getAdults() { return adults; }
    public int getChildren() { return children; }
    public String getCabinClass() { return cabinClass; }
    public boolean isNonStop() { return nonStop; }
    public String getCurrencyCode() { return currencyCode; }
    public int getMaxResults() { return maxResults; }
    public boolean isRoundTrip() { return returnDate != null; }
    
    @Override
    public String toString() {
        return "FlightSearchRequest{" +
                "origin='" + origin + '\'' +
                ", destination='" + destination + '\'' +
                ", departureDate=" + departureDate +
                ", returnDate=" + returnDate +
                ", adults=" + adults +
                ", children=" + children +
                ", cabinClass='" + cabinClass + '\'' +
                ", nonStop=" + nonStop +
                ", currencyCode='" + currencyCode + '\'' +
                ", maxResults=" + maxResults +
                '}';
    }
}