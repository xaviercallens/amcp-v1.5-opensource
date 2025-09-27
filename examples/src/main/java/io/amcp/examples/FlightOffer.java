package io.amcp.examples.travel;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Represents a flight offer returned from Amadeus Flight Offers Search API.
 * Follows AMCP patterns for data exchange between agents.
 */
public class FlightOffer implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String id;
    private final String source;
    private final boolean instantTicketingRequired;
    private final boolean nonHomogeneous;
    private final boolean oneWay;
    private final String lastTicketingDate;
    private final int numberOfBookableSeats;
    private final List<Itinerary> itineraries;
    private final Price price;
    private final List<PricingOptions> pricingOptions;
    private final List<String> validatingAirlineCodes;
    private final TravelerPricings travelerPricings;
    private final Instant retrievedAt;
    
    private FlightOffer(Builder builder) {
        this.id = builder.id;
        this.source = builder.source;
        this.instantTicketingRequired = builder.instantTicketingRequired;
        this.nonHomogeneous = builder.nonHomogeneous;
        this.oneWay = builder.oneWay;
        this.lastTicketingDate = builder.lastTicketingDate;
        this.numberOfBookableSeats = builder.numberOfBookableSeats;
        this.itineraries = builder.itineraries;
        this.price = builder.price;
        this.pricingOptions = builder.pricingOptions;
        this.validatingAirlineCodes = builder.validatingAirlineCodes;
        this.travelerPricings = builder.travelerPricings;
        this.retrievedAt = builder.retrievedAt != null ? builder.retrievedAt : Instant.now();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String source = "GDS";
        private boolean instantTicketingRequired = false;
        private boolean nonHomogeneous = false;
        private boolean oneWay = false;
        private String lastTicketingDate;
        private int numberOfBookableSeats = 0;
        private List<Itinerary> itineraries;
        private Price price;
        private List<PricingOptions> pricingOptions;
        private List<String> validatingAirlineCodes;
        private TravelerPricings travelerPricings;
        private Instant retrievedAt;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder source(String source) {
            this.source = source;
            return this;
        }
        
        public Builder instantTicketingRequired(boolean instantTicketingRequired) {
            this.instantTicketingRequired = instantTicketingRequired;
            return this;
        }
        
        public Builder nonHomogeneous(boolean nonHomogeneous) {
            this.nonHomogeneous = nonHomogeneous;
            return this;
        }
        
        public Builder oneWay(boolean oneWay) {
            this.oneWay = oneWay;
            return this;
        }
        
        public Builder lastTicketingDate(String lastTicketingDate) {
            this.lastTicketingDate = lastTicketingDate;
            return this;
        }
        
        public Builder numberOfBookableSeats(int numberOfBookableSeats) {
            this.numberOfBookableSeats = numberOfBookableSeats;
            return this;
        }
        
        public Builder itineraries(List<Itinerary> itineraries) {
            this.itineraries = itineraries;
            return this;
        }
        
        public Builder price(Price price) {
            this.price = price;
            return this;
        }
        
        public Builder pricingOptions(List<PricingOptions> pricingOptions) {
            this.pricingOptions = pricingOptions;
            return this;
        }
        
        public Builder validatingAirlineCodes(List<String> validatingAirlineCodes) {
            this.validatingAirlineCodes = validatingAirlineCodes;
            return this;
        }
        
        public Builder travelerPricings(TravelerPricings travelerPricings) {
            this.travelerPricings = travelerPricings;
            return this;
        }
        
        public Builder retrievedAt(Instant retrievedAt) {
            this.retrievedAt = retrievedAt;
            return this;
        }
        
        public FlightOffer build() {
            Objects.requireNonNull(id, "Flight offer ID is required");
            Objects.requireNonNull(itineraries, "Itineraries are required");
            Objects.requireNonNull(price, "Price is required");
            return new FlightOffer(this);
        }
    }
    
    // Nested classes for flight offer components
    
    public static class Itinerary implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String duration;
        private final List<Segment> segments;
        
        public Itinerary(String duration, List<Segment> segments) {
            this.duration = duration;
            this.segments = segments;
        }
        
        public String getDuration() { return duration; }
        public List<Segment> getSegments() { return segments; }
    }
    
    public static class Segment implements Serializable {
        private static final long serialVersionUID = 1L;
        private final FlightEndpoint departure;
        private final FlightEndpoint arrival;
        private final String carrierCode;
        private final String number;
        private final Aircraft aircraft;
        private final Operating operating;
        private final String duration;
        private final String id;
        private final int numberOfStops;
        private final boolean blacklistedInEU;
        
        public Segment(FlightEndpoint departure, FlightEndpoint arrival, String carrierCode, 
                      String number, Aircraft aircraft, Operating operating, String duration, 
                      String id, int numberOfStops, boolean blacklistedInEU) {
            this.departure = departure;
            this.arrival = arrival;
            this.carrierCode = carrierCode;
            this.number = number;
            this.aircraft = aircraft;
            this.operating = operating;
            this.duration = duration;
            this.id = id;
            this.numberOfStops = numberOfStops;
            this.blacklistedInEU = blacklistedInEU;
        }
        
        // Getters
        public FlightEndpoint getDeparture() { return departure; }
        public FlightEndpoint getArrival() { return arrival; }
        public String getCarrierCode() { return carrierCode; }
        public String getNumber() { return number; }
        public Aircraft getAircraft() { return aircraft; }
        public Operating getOperating() { return operating; }
        public String getDuration() { return duration; }
        public String getId() { return id; }
        public int getNumberOfStops() { return numberOfStops; }
        public boolean isBlacklistedInEU() { return blacklistedInEU; }
    }
    
    public static class FlightEndpoint implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String iataCode;
        private final String terminal;
        private final LocalDateTime at;
        
        public FlightEndpoint(String iataCode, String terminal, LocalDateTime at) {
            this.iataCode = iataCode;
            this.terminal = terminal;
            this.at = at;
        }
        
        public String getIataCode() { return iataCode; }
        public String getTerminal() { return terminal; }
        public LocalDateTime getAt() { return at; }
    }
    
    public static class Aircraft implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String code;
        
        public Aircraft(String code) {
            this.code = code;
        }
        
        public String getCode() { return code; }
    }
    
    public static class Operating implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String carrierCode;
        
        public Operating(String carrierCode) {
            this.carrierCode = carrierCode;
        }
        
        public String getCarrierCode() { return carrierCode; }
    }
    
    public static class Price implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String currency;
        private final String total;
        private final String base;
        private final List<Fee> fees;
        private final String grandTotal;
        
        public Price(String currency, String total, String base, List<Fee> fees, String grandTotal) {
            this.currency = currency;
            this.total = total;
            this.base = base;
            this.fees = fees;
            this.grandTotal = grandTotal;
        }
        
        public String getCurrency() { return currency; }
        public String getTotal() { return total; }
        public String getBase() { return base; }
        public List<Fee> getFees() { return fees; }
        public String getGrandTotal() { return grandTotal; }
    }
    
    public static class Fee implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String amount;
        private final String type;
        
        public Fee(String amount, String type) {
            this.amount = amount;
            this.type = type;
        }
        
        public String getAmount() { return amount; }
        public String getType() { return type; }
    }
    
    public static class PricingOptions implements Serializable {
        private static final long serialVersionUID = 1L;
        private final List<String> fareType;
        private final boolean includedCheckedBagsOnly;
        
        public PricingOptions(List<String> fareType, boolean includedCheckedBagsOnly) {
            this.fareType = fareType;
            this.includedCheckedBagsOnly = includedCheckedBagsOnly;
        }
        
        public List<String> getFareType() { return fareType; }
        public boolean isIncludedCheckedBagsOnly() { return includedCheckedBagsOnly; }
    }
    
    public static class TravelerPricings implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String travelerId;
        private final String fareOption;
        private final String travelerType;
        private final Price price;
        
        public TravelerPricings(String travelerId, String fareOption, String travelerType, Price price) {
            this.travelerId = travelerId;
            this.fareOption = fareOption;
            this.travelerType = travelerType;
            this.price = price;
        }
        
        public String getTravelerId() { return travelerId; }
        public String getFareOption() { return fareOption; }
        public String getTravelerType() { return travelerType; }
        public Price getPrice() { return price; }
    }
    
    // Main class getters
    public String getId() { return id; }
    public String getSource() { return source; }
    public boolean isInstantTicketingRequired() { return instantTicketingRequired; }
    public boolean isNonHomogeneous() { return nonHomogeneous; }
    public boolean isOneWay() { return oneWay; }
    public String getLastTicketingDate() { return lastTicketingDate; }
    public int getNumberOfBookableSeats() { return numberOfBookableSeats; }
    public List<Itinerary> getItineraries() { return itineraries; }
    public Price getPrice() { return price; }
    public List<PricingOptions> getPricingOptions() { return pricingOptions; }
    public List<String> getValidatingAirlineCodes() { return validatingAirlineCodes; }
    public TravelerPricings getTravelerPricings() { return travelerPricings; }
    public Instant getRetrievedAt() { return retrievedAt; }
    
    @Override
    public String toString() {
        return "FlightOffer{" +
                "id='" + id + '\'' +
                ", source='" + source + '\'' +
                ", price=" + (price != null ? price.getTotal() + " " + price.getCurrency() : "N/A") +
                ", itineraries=" + (itineraries != null ? itineraries.size() : 0) + 
                ", retrievedAt=" + retrievedAt +
                '}';
    }
}