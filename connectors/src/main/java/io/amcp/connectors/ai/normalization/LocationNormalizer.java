package io.amcp.connectors.ai.normalization;

import java.util.*;

/**
 * Location normalization engine for standardizing location data across agents.
 * Handles city names, country codes, IATA codes, and location enrichment.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class LocationNormalizer {
    
    // Country code mappings (ISO 3166-1 alpha-2)
    private static final Map<String, String> COUNTRY_CODE_TO_NAME = new HashMap<>();
    private static final Map<String, String> COUNTRY_NAME_TO_CODE = new HashMap<>();
    
    // IATA airport code mappings
    private static final Map<String, CityInfo> IATA_TO_CITY = new HashMap<>();
    
    // City name variations and mappings
    private static final Map<String, CityInfo> CITY_VARIATIONS = new HashMap<>();
    
    static {
        initializeCountryMappings();
        initializeCityMappings();
        initializeIATAMappings();
    }
    
    /**
     * Normalizes location string to standard format: "City,CC" (e.g., "Nice,FR")
     */
    public String normalizeLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        
        String normalized = location.trim();
        
        // Check if already in correct format (City,CC)
        if (normalized.matches("^[A-Za-z\\s]+,[A-Z]{2}$")) {
            return normalized;
        }
        
        // Handle various input formats
        CityInfo cityInfo = parseLocation(normalized);
        
        if (cityInfo != null) {
            return cityInfo.city + "," + cityInfo.countryCode;
        }
        
        // If we can't parse it, return cleaned version
        return cleanLocationString(normalized);
    }
    
    /**
     * Enriches location with full country name: "Nice, France"
     */
    public String enrichLocation(String location) {
        String normalized = normalizeLocation(location);
        if (normalized == null) {
            return location;
        }
        
        String[] parts = normalized.split(",");
        if (parts.length == 2) {
            String city = parts[0].trim();
            String countryCode = parts[1].trim();
            String countryName = COUNTRY_CODE_TO_NAME.get(countryCode);
            
            if (countryName != null) {
                return city + ", " + countryName;
            }
        }
        
        return normalized;
    }
    
    /**
     * Converts IATA airport code to city location
     */
    public String iataToLocation(String iataCode) {
        if (iataCode == null || iataCode.length() != 3) {
            return null;
        }
        
        CityInfo cityInfo = IATA_TO_CITY.get(iataCode.toUpperCase());
        if (cityInfo != null) {
            return cityInfo.city + "," + cityInfo.countryCode;
        }
        
        return null;
    }
    
    /**
     * Gets country name from country code
     */
    public String getCountryName(String countryCode) {
        if (countryCode == null) {
            return null;
        }
        return COUNTRY_CODE_TO_NAME.get(countryCode.toUpperCase());
    }
    
    /**
     * Gets country code from country name
     */
    public String getCountryCode(String countryName) {
        if (countryName == null) {
            return null;
        }
        return COUNTRY_NAME_TO_CODE.get(countryName.toLowerCase());
    }
    
    /**
     * Validates if location string is in correct normalized format
     */
    public boolean isNormalized(String location) {
        if (location == null) {
            return false;
        }
        return location.matches("^[A-Za-z\\s]+,[A-Z]{2}$");
    }
    
    private CityInfo parseLocation(String location) {
        String lower = location.toLowerCase().trim();
        
        // Try direct city lookup
        CityInfo cityInfo = CITY_VARIATIONS.get(lower);
        if (cityInfo != null) {
            return cityInfo;
        }
        
        // Try parsing "City, Country" or "City,Country" format
        if (location.contains(",")) {
            String[] parts = location.split(",");
            if (parts.length >= 2) {
                String city = parts[0].trim();
                String countryPart = parts[1].trim();
                
                // Check if country part is a code or name
                String countryCode = null;
                if (countryPart.length() == 2) {
                    countryCode = countryPart.toUpperCase();
                } else {
                    countryCode = COUNTRY_NAME_TO_CODE.get(countryPart.toLowerCase());
                }
                
                if (countryCode != null) {
                    return new CityInfo(capitalizeCity(city), countryCode);
                }
            }
        }
        
        // Try IATA code
        if (location.length() == 3) {
            CityInfo iataCity = IATA_TO_CITY.get(location.toUpperCase());
            if (iataCity != null) {
                return iataCity;
            }
        }
        
        // Try partial matching
        for (Map.Entry<String, CityInfo> entry : CITY_VARIATIONS.entrySet()) {
            if (lower.contains(entry.getKey()) || entry.getKey().contains(lower)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    private String cleanLocationString(String location) {
        // Remove extra whitespace and special characters
        return location.trim().replaceAll("\\s+", " ");
    }
    
    private String capitalizeCity(String city) {
        if (city == null || city.isEmpty()) {
            return city;
        }
        
        String[] words = city.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
    
    private static void initializeCountryMappings() {
        // Major countries
        addCountry("FR", "France");
        addCountry("GB", "United Kingdom");
        addCountry("UK", "United Kingdom"); // Common alternative
        addCountry("US", "United States");
        addCountry("DE", "Germany");
        addCountry("IT", "Italy");
        addCountry("ES", "Spain");
        addCountry("PT", "Portugal");
        addCountry("NL", "Netherlands");
        addCountry("BE", "Belgium");
        addCountry("CH", "Switzerland");
        addCountry("AT", "Austria");
        addCountry("GR", "Greece");
        addCountry("JP", "Japan");
        addCountry("CN", "China");
        addCountry("KR", "South Korea");
        addCountry("IN", "India");
        addCountry("AU", "Australia");
        addCountry("NZ", "New Zealand");
        addCountry("CA", "Canada");
        addCountry("MX", "Mexico");
        addCountry("BR", "Brazil");
        addCountry("AR", "Argentina");
        addCountry("ZA", "South Africa");
        addCountry("EG", "Egypt");
        addCountry("AE", "United Arab Emirates");
        addCountry("SA", "Saudi Arabia");
        addCountry("TR", "Turkey");
        addCountry("RU", "Russia");
        addCountry("SE", "Sweden");
        addCountry("NO", "Norway");
        addCountry("DK", "Denmark");
        addCountry("FI", "Finland");
        addCountry("PL", "Poland");
        addCountry("CZ", "Czech Republic");
        addCountry("HU", "Hungary");
        addCountry("RO", "Romania");
        addCountry("BG", "Bulgaria");
        addCountry("HR", "Croatia");
        addCountry("SI", "Slovenia");
        addCountry("SK", "Slovakia");
        addCountry("IE", "Ireland");
        addCountry("IS", "Iceland");
        addCountry("LU", "Luxembourg");
        addCountry("MC", "Monaco");
        addCountry("MT", "Malta");
        addCountry("CY", "Cyprus");
    }
    
    private static void addCountry(String code, String name) {
        COUNTRY_CODE_TO_NAME.put(code, name);
        COUNTRY_NAME_TO_CODE.put(name.toLowerCase(), code);
    }
    
    private static void initializeCityMappings() {
        // France
        addCity("nice", "Nice", "FR");
        addCity("paris", "Paris", "FR");
        addCity("lyon", "Lyon", "FR");
        addCity("marseille", "Marseille", "FR");
        addCity("toulouse", "Toulouse", "FR");
        addCity("bordeaux", "Bordeaux", "FR");
        
        // United Kingdom
        addCity("london", "London", "GB");
        addCity("manchester", "Manchester", "GB");
        addCity("birmingham", "Birmingham", "GB");
        addCity("edinburgh", "Edinburgh", "GB");
        addCity("glasgow", "Glasgow", "GB");
        
        // United States
        addCity("new york", "New York", "US");
        addCity("los angeles", "Los Angeles", "US");
        addCity("chicago", "Chicago", "US");
        addCity("san francisco", "San Francisco", "US");
        addCity("miami", "Miami", "US");
        addCity("boston", "Boston", "US");
        addCity("seattle", "Seattle", "US");
        addCity("las vegas", "Las Vegas", "US");
        
        // Germany
        addCity("berlin", "Berlin", "DE");
        addCity("munich", "Munich", "DE");
        addCity("frankfurt", "Frankfurt", "DE");
        addCity("hamburg", "Hamburg", "DE");
        
        // Italy
        addCity("rome", "Rome", "IT");
        addCity("milan", "Milan", "IT");
        addCity("venice", "Venice", "IT");
        addCity("florence", "Florence", "IT");
        
        // Spain
        addCity("madrid", "Madrid", "ES");
        addCity("barcelona", "Barcelona", "ES");
        addCity("seville", "Seville", "ES");
        addCity("valencia", "Valencia", "ES");
        
        // Japan
        addCity("tokyo", "Tokyo", "JP");
        addCity("osaka", "Osaka", "JP");
        addCity("kyoto", "Kyoto", "JP");
        
        // Australia
        addCity("sydney", "Sydney", "AU");
        addCity("melbourne", "Melbourne", "AU");
        addCity("brisbane", "Brisbane", "AU");
        
        // Canada
        addCity("toronto", "Toronto", "CA");
        addCity("vancouver", "Vancouver", "CA");
        addCity("montreal", "Montreal", "CA");
        
        // Other major cities
        addCity("dubai", "Dubai", "AE");
        addCity("singapore", "Singapore", "SG");
        addCity("hong kong", "Hong Kong", "HK");
        addCity("beijing", "Beijing", "CN");
        addCity("shanghai", "Shanghai", "CN");
        addCity("seoul", "Seoul", "KR");
        addCity("bangkok", "Bangkok", "TH");
        addCity("istanbul", "Istanbul", "TR");
        addCity("moscow", "Moscow", "RU");
        addCity("amsterdam", "Amsterdam", "NL");
        addCity("brussels", "Brussels", "BE");
        addCity("zurich", "Zurich", "CH");
        addCity("vienna", "Vienna", "AT");
        addCity("prague", "Prague", "CZ");
        addCity("budapest", "Budapest", "HU");
        addCity("warsaw", "Warsaw", "PL");
        addCity("athens", "Athens", "GR");
        addCity("lisbon", "Lisbon", "PT");
        addCity("copenhagen", "Copenhagen", "DK");
        addCity("stockholm", "Stockholm", "SE");
        addCity("oslo", "Oslo", "NO");
        addCity("helsinki", "Helsinki", "FI");
    }
    
    private static void addCity(String variation, String standardName, String countryCode) {
        CityInfo cityInfo = new CityInfo(standardName, countryCode);
        CITY_VARIATIONS.put(variation.toLowerCase(), cityInfo);
    }
    
    private static void initializeIATAMappings() {
        // Major airports
        addIATA("NCE", "Nice", "FR");
        addIATA("CDG", "Paris", "FR");
        addIATA("ORY", "Paris", "FR");
        addIATA("LHR", "London", "GB");
        addIATA("LGW", "London", "GB");
        addIATA("JFK", "New York", "US");
        addIATA("LAX", "Los Angeles", "US");
        addIATA("ORD", "Chicago", "US");
        addIATA("SFO", "San Francisco", "US");
        addIATA("MIA", "Miami", "US");
        addIATA("BOS", "Boston", "US");
        addIATA("SEA", "Seattle", "US");
        addIATA("LAS", "Las Vegas", "US");
        addIATA("FRA", "Frankfurt", "DE");
        addIATA("MUC", "Munich", "DE");
        addIATA("FCO", "Rome", "IT");
        addIATA("MXP", "Milan", "IT");
        addIATA("MAD", "Madrid", "ES");
        addIATA("BCN", "Barcelona", "ES");
        addIATA("NRT", "Tokyo", "JP");
        addIATA("HND", "Tokyo", "JP");
        addIATA("SYD", "Sydney", "AU");
        addIATA("YYZ", "Toronto", "CA");
        addIATA("YVR", "Vancouver", "CA");
        addIATA("DXB", "Dubai", "AE");
        addIATA("SIN", "Singapore", "SG");
        addIATA("HKG", "Hong Kong", "HK");
        addIATA("PEK", "Beijing", "CN");
        addIATA("ICN", "Seoul", "KR");
        addIATA("BKK", "Bangkok", "TH");
        addIATA("IST", "Istanbul", "TR");
        addIATA("AMS", "Amsterdam", "NL");
        addIATA("BRU", "Brussels", "BE");
        addIATA("ZRH", "Zurich", "CH");
        addIATA("VIE", "Vienna", "AT");
        addIATA("PRG", "Prague", "CZ");
    }
    
    private static void addIATA(String code, String city, String countryCode) {
        IATA_TO_CITY.put(code, new CityInfo(city, countryCode));
    }
    
    /**
     * Internal class to hold city information
     */
    public static class CityInfo {
        public final String city;
        public final String countryCode;
        
        public CityInfo(String city, String countryCode) {
            this.city = city;
            this.countryCode = countryCode;
        }
        
        @Override
        public String toString() {
            return city + "," + countryCode;
        }
    }
}
