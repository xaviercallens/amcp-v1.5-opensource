package io.amcp.examples.travel;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result object containing flight offers returned from Amadeus Flight Offers Search API.
 * Follows AMCP patterns for data exchange between agents.
 */
public class FlightOffersSearchResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final FlightSearchRequest originalRequest;
    private final List<FlightOffer> flightOffers;
    private final int resultCount;
    private final Instant retrievedAt;
    private final String rawResponse;
    private final String error;
    private final boolean successful;
    private final Meta meta;
    
    private FlightOffersSearchResult(Builder builder) {
        this.originalRequest = builder.originalRequest;
        this.flightOffers = builder.flightOffers != null ? builder.flightOffers : new ArrayList<FlightOffer>();
        this.resultCount = builder.resultCount;
        this.retrievedAt = builder.retrievedAt != null ? builder.retrievedAt : Instant.now();
        this.rawResponse = builder.rawResponse;
        this.error = builder.error;
        this.successful = builder.error == null || builder.error.trim().isEmpty();
        this.meta = builder.meta;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private FlightSearchRequest originalRequest;
        private List<FlightOffer> flightOffers;
        private int resultCount = 0;
        private Instant retrievedAt;
        private String rawResponse;
        private String error;
        private Meta meta;
        
        public Builder originalRequest(FlightSearchRequest originalRequest) {
            this.originalRequest = originalRequest;
            return this;
        }
        
        public Builder flightOffers(List<FlightOffer> flightOffers) {
            this.flightOffers = flightOffers;
            if (flightOffers != null) {
                this.resultCount = flightOffers.size();
            }
            return this;
        }
        
        public Builder resultCount(int resultCount) {
            this.resultCount = resultCount;
            return this;
        }
        
        public Builder retrievedAt(Instant retrievedAt) {
            this.retrievedAt = retrievedAt;
            return this;
        }
        
        public Builder rawResponse(String rawResponse) {
            this.rawResponse = rawResponse;
            return this;
        }
        
        public Builder error(String error) {
            this.error = error;
            return this;
        }
        
        public Builder meta(Meta meta) {
            this.meta = meta;
            return this;
        }
        
        public FlightOffersSearchResult build() {
            return new FlightOffersSearchResult(this);
        }
    }
    
    /**
     * Metadata about the search results.
     */
    public static class Meta implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final int count;
        private final String currency;
        private final Links links;
        
        public Meta(int count, String currency, Links links) {
            this.count = count;
            this.currency = currency;
            this.links = links;
        }
        
        public int getCount() { return count; }
        public String getCurrency() { return currency; }
        public Links getLinks() { return links; }
    }
    
    /**
     * Links for pagination (if supported by API).
     */
    public static class Links implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String self;
        private final String next;
        private final String previous;
        private final String first;
        private final String last;
        
        public Links(String self, String next, String previous, String first, String last) {
            this.self = self;
            this.next = next;
            this.previous = previous;
            this.first = first;
            this.last = last;
        }
        
        public String getSelf() { return self; }
        public String getNext() { return next; }
        public String getPrevious() { return previous; }
        public String getFirst() { return first; }
        public String getLast() { return last; }
        
        public boolean hasNext() { return next != null && !next.trim().isEmpty(); }
        public boolean hasPrevious() { return previous != null && !previous.trim().isEmpty(); }
    }
    
    // Getters
    public FlightSearchRequest getOriginalRequest() { 
        return originalRequest; 
    }
    
    public List<FlightOffer> getFlightOffers() { 
        return new ArrayList<>(flightOffers); 
    }
    
    public int getResultCount() { 
        return resultCount; 
    }
    
    public Instant getRetrievedAt() { 
        return retrievedAt; 
    }
    
    public String getRawResponse() { 
        return rawResponse; 
    }
    
    public String getError() { 
        return error; 
    }
    
    public boolean isSuccessful() { 
        return successful; 
    }
    
    public Meta getMeta() { 
        return meta; 
    }
    
    public boolean hasFlightOffers() {
        return flightOffers != null && !flightOffers.isEmpty();
    }
    
    public boolean hasError() {
        return error != null && !error.trim().isEmpty();
    }
    
    /**
     * Gets the cheapest flight offer from the results, if any.
     */
    public FlightOffer getCheapestOffer() {
        if (!hasFlightOffers()) {
            return null;
        }
        
        FlightOffer cheapest = null;
        double lowestPrice = Double.MAX_VALUE;
        
        for (FlightOffer offer : flightOffers) {
            if (offer.getPrice() != null && offer.getPrice().getTotal() != null) {
                try {
                    double price = Double.parseDouble(offer.getPrice().getTotal());
                    if (price < lowestPrice) {
                        lowestPrice = price;
                        cheapest = offer;
                    }
                } catch (NumberFormatException e) {
                    // Skip offers with invalid price format
                }
            }
        }
        
        return cheapest;
    }
    
    /**
     * Gets flight offers within a specified price range.
     */
    public List<FlightOffer> getOffersInPriceRange(double minPrice, double maxPrice) {
        List<FlightOffer> filteredOffers = new ArrayList<>();
        
        if (!hasFlightOffers()) {
            return filteredOffers;
        }
        
        for (FlightOffer offer : flightOffers) {
            if (offer.getPrice() != null && offer.getPrice().getTotal() != null) {
                try {
                    double price = Double.parseDouble(offer.getPrice().getTotal());
                    if (price >= minPrice && price <= maxPrice) {
                        filteredOffers.add(offer);
                    }
                } catch (NumberFormatException e) {
                    // Skip offers with invalid price format
                }
            }
        }
        
        return filteredOffers;
    }
    
    /**
     * Gets flight offers from a specific airline.
     */
    public List<FlightOffer> getOffersByAirline(String airlineCode) {
        List<FlightOffer> filteredOffers = new ArrayList<>();
        
        if (!hasFlightOffers() || airlineCode == null || airlineCode.trim().isEmpty()) {
            return filteredOffers;
        }
        
        String targetAirline = airlineCode.trim().toUpperCase();
        
        for (FlightOffer offer : flightOffers) {
            if (offer.getValidatingAirlineCodes() != null) {
                for (String airline : offer.getValidatingAirlineCodes()) {
                    if (targetAirline.equals(airline.trim().toUpperCase())) {
                        filteredOffers.add(offer);
                        break;
                    }
                }
            }
        }
        
        return filteredOffers;
    }
    
    /**
     * Gets a summary of the search results.
     */
    public String getSummary() {
        if (hasError()) {
            return "Flight search failed: " + error;
        }
        
        if (!hasFlightOffers()) {
            return "No flight offers found for " + 
                   (originalRequest != null ? originalRequest.getOrigin() + " -> " + originalRequest.getDestination() : "search request");
        }
        
        FlightOffer cheapest = getCheapestOffer();
        String summary = resultCount + " flight offers found";
        
        if (originalRequest != null) {
            summary += " for " + originalRequest.getOrigin() + " -> " + originalRequest.getDestination();
        }
        
        if (cheapest != null && cheapest.getPrice() != null) {
            summary += ", cheapest from " + cheapest.getPrice().getTotal() + " " + cheapest.getPrice().getCurrency();
        }
        
        return summary;
    }
    
    @Override
    public String toString() {
        return "FlightOffersSearchResult{" +
                "resultCount=" + resultCount +
                ", successful=" + successful +
                ", hasFlightOffers=" + hasFlightOffers() +
                ", retrievedAt=" + retrievedAt +
                (hasError() ? ", error='" + error + '\'' : "") +
                '}';
    }
}