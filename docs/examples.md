# AMCP v1.5 Examples Documentation

## Overview

AMCP v1.5 includes three comprehensive examples that demonstrate different aspects of agent-based system development. These examples progress from simple concepts to complex real-world applications.

## Examples Hierarchy

1. **GreetingAgent** - Basic agent lifecycle and communication
2. **Travel Planner System** - Complex multi-agent coordination with external APIs
3. **Weather Monitoring System** - Real-time data collection and alerting

---

## 1. Greeting Agent

**File:** `examples/src/main/java/io/amcp/examples/GreetingAgent.java`

### Purpose
Demonstrates fundamental AMCP concepts including agent lifecycle, event handling, and basic pub/sub messaging.

### Key Features
- Simple event-driven communication
- Agent lifecycle management
- Basic topic subscription patterns
- Periodic task scheduling

### Code Structure

```java
public class GreetingAgent implements Agent {
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            switch (event.getTopic()) {
                case "greeting.request":
                    handleGreetingRequest(event);
                    break;
                case "greeting.farewell":
                    handleFarewell(event);
                    break;
            }
        });
    }
    
    @Override
    public void onActivate() {
        context.subscribe("greeting.*");
        startPeriodicGreeting();
    }
}
```

### Running the Example

```bash
# Build and run
./scripts/build-all.sh
./scripts/run-greeting-agent.sh

# Expected output:
# [10:30:15] [greeting-agent-001] Agent activated
# [10:30:15] [greeting-agent-001] Hello! I'm ready to greet.
# [10:30:20] [greeting-agent-001] Periodic greeting sent
```

### Learning Objectives
- ✅ Understanding Agent interface implementation
- ✅ Event handling with CompletableFuture
- ✅ Topic-based message routing
- ✅ Agent activation and lifecycle
- ✅ Periodic task scheduling with ScheduledExecutorService

---

## 2. Travel Planner System

**Files:** 
- `examples/src/main/java/io/amcp/examples/travel/TravelPlannerAgent.java`
- `examples/src/main/java/io/amcp/examples/travel/RouteOptimizer.java`
- `examples/src/main/java/io/amcp/examples/travel/DestinationAgent.java`

### Purpose
Demonstrates advanced multi-agent coordination, external API integration, and complex business logic implementation.

### System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  TravelPlanner  │◄──►│ RouteOptimizer  │◄──►│ DestinationAgent│
│     Agent       │    │     Agent       │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│              Event-Driven Communication Bus                     │
│  Topics: travel.*, route.*, destination.*, booking.*           │
└─────────────────────────────────────────────────────────────────┘
```

### Key Features
- **Multi-Agent Coordination**: Multiple specialized agents working together
- **External API Integration**: Maps API, weather services, booking systems
- **Complex State Management**: Trip planning state across multiple agents
- **Request-Response Patterns**: Asynchronous request/response flows
- **Error Handling**: Comprehensive error handling and fallback strategies
- **Interactive CLI**: Rich command-line interface for user interaction

### Core Components

#### TravelPlannerAgent
```java
public class TravelPlannerAgent implements Agent {
    private final Map<String, TripPlan> activePlans = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong(0);
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            switch (event.getTopic()) {
                case "travel.plan.request":
                    handlePlanRequest(event);
                    break;
                case "route.optimization.complete":
                    handleOptimizedRoute(event);
                    break;
                case "destination.info.response":
                    handleDestinationInfo(event);
                    break;
            }
        });
    }
    
    private void handlePlanRequest(Event event) {
        TravelRequest request = (TravelRequest) event.getPayload();
        String planId = generatePlanId();
        
        // Create new trip plan
        TripPlan plan = new TripPlan(planId, request);
        activePlans.put(planId, plan);
        
        // Request route optimization
        publishEvent("route.optimization.request", request)
            .thenRun(() -> logger.info("Route optimization requested for plan: {}", planId));
        
        // Request destination information
        request.getDestinations().forEach(dest -> 
            publishEvent("destination.info.request", dest));
    }
}
```

#### RouteOptimizer
```java
public class RouteOptimizer implements Agent {
    private final ExternalMapsService mapsService;
    
    private void optimizeRoute(RouteRequest request) {
        try {
            // Call external maps API
            OptimizedRoute route = mapsService.optimizeRoute(
                request.getStartPoint(),
                request.getDestinations(),
                request.getPreferences()
            );
            
            // Publish optimized route
            Event response = Event.builder()
                .topic("route.optimization.complete")
                .payload(route)
                .metadata("requestId", request.getId())
                .deliveryOptions(DeliveryOptions.RELIABLE)
                .build();
            
            publishEvent(response);
            
        } catch (ExternalServiceException e) {
            // Fallback to internal optimization
            OptimizedRoute fallbackRoute = performInternalOptimization(request);
            publishEvent("route.optimization.complete", fallbackRoute);
        }
    }
}
```

### Configuration

```bash
# Environment variables for enhanced features
export MAPS_API_KEY=your_google_maps_api_key
export WEATHER_API_KEY=your_weather_api_key
export BOOKING_API_KEY=your_booking_api_key

# Optional: Configure preferences
export TRAVEL_DEFAULT_MODE=driving
export TRAVEL_OPTIMIZE_FOR=time  # or distance, cost
export TRAVEL_AVOID=tolls,ferries
```

### Running the Example

```bash
# Start the travel planner system
./scripts/run-travel-planner.sh

# Interactive commands available:
# > plan trip from "New York" to "Boston,Philadelphia,Washington DC"
# > optimize route current
# > get recommendations for "Boston"
# > book hotel in "Philadelphia" 
# > show current plans
# > help
```

### Sample Session

```bash
$ ./scripts/run-travel-planner.sh
✈️  AMCP v1.5 - Travel Planner Agent
====================================

🚀 Starting Travel Planner Agent...
Type 'help' for available commands

> plan trip from "San Francisco" to "Los Angeles,San Diego"
🗺️  Creating trip plan...
📍 Optimizing route for 3 destinations
⏱️  Estimated completion: 30 seconds

> show current plans
📋 Active Trip Plans:
   Plan ID: trip-001
   Status: Optimizing
   Route: San Francisco → Los Angeles → San Diego
   Estimated Distance: 520 miles
   Estimated Time: 8h 30m

> optimize route trip-001 for time
⚡ Re-optimizing route for minimum time...
✅ Route optimized: San Francisco → San Diego → Los Angeles
   New Time: 7h 45m (45 minutes saved)
```

### Learning Objectives
- ✅ Multi-agent system design and coordination
- ✅ External API integration patterns
- ✅ Asynchronous request-response workflows
- ✅ Complex state management across agents
- ✅ Error handling and fallback strategies
- ✅ Interactive command-line interface design
- ✅ Configuration management and environment variables

---

## 3. Weather Monitoring System

**Files:**
- `examples/src/main/java/io/amcp/examples/weather/WeatherCollectorAgent.java`
- `examples/src/main/java/io/amcp/examples/weather/WeatherSystemLauncher.java`
- `examples/src/main/java/io/amcp/examples/weather/StandaloneWeatherCollector.java`

### Purpose
Demonstrates real-time data processing, external service integration, alerting systems, and production-ready agent patterns.

### System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Weather Monitoring System                    │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │
│  │ Collector   │  │ Analyzer    │  │    Alert Manager        │   │
│  │   Agent     │  │   Agent     │  │       Agent             │   │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘   │
│         │                 │                       │              │
│         ▼                 ▼                       ▼              │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │           Weather Event Communication Bus                   │ │
│  │  Topics: weather.data.*, weather.alert.*, weather.system.*│ │
│  └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                    External Integration                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │
│  │OpenWeatherMap│ │   Storage   │  │   Notification          │   │
│  │     API     │  │   Services  │  │    Services             │   │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Key Features

#### Real-Time Data Collection
```java
public class WeatherCollectorAgent implements Agent {
    private final ScheduledExecutorService scheduler = 
        Executors.newScheduledThreadPool(3);
    private final List<String> monitoredLocations = Arrays.asList(
        "New York,NY", "London,UK", "Tokyo,Japan", "Sydney,Australia"
    );
    
    @Override
    public void onActivate() {
        context.subscribe("weather.collection.*");
        context.subscribe("system.shutdown");
        
        // Start periodic collection every 5 minutes
        scheduler.scheduleAtFixedRate(this::collectAllWeatherData, 
                                    0, 5, TimeUnit.MINUTES);
    }
    
    private void collectAllWeatherData() {
        monitoredLocations.parallelStream().forEach(location -> {
            try {
                WeatherData data = fetchWeatherData(location);
                publishWeatherUpdate(data);
                checkForAlerts(data);
            } catch (Exception e) {
                logger.error("Failed to collect weather for {}", location, e);
            }
        });
    }
}
```

#### Smart Alerting System
```java
private void checkForAlerts(WeatherData data) {
    List<WeatherAlert> alerts = new ArrayList<>();
    
    // Temperature alerts
    if (data.getTemperature() > 35.0) {
        alerts.add(new WeatherAlert("EXTREME_HEAT", 
            "Temperature exceeds 35°C", data));
    } else if (data.getTemperature() < -20.0) {
        alerts.add(new WeatherAlert("EXTREME_COLD", 
            "Temperature below -20°C", data));
    }
    
    // Wind alerts
    if (data.getWindSpeed() > 60) {
        alerts.add(new WeatherAlert("HIGH_WIND", 
            "Wind speed exceeds 60 km/h", data));
    }
    
    // Publish alerts
    alerts.forEach(alert -> {
        Event alertEvent = Event.builder()
            .topic("weather.alert.severe")
            .payload(alert)
            .deliveryOptions(DeliveryOptions.RELIABLE)
            .metadata("location", data.getLocation())
            .metadata("severity", alert.getSeverity().toString())
            .build();
        
        publishEvent(alertEvent);
    });
}
```

#### Interactive CLI System
```java
public class WeatherSystemLauncher {
    public static void main(String[] args) {
        WeatherSystem system = new WeatherSystem();
        Scanner scanner = new Scanner(System.in);
        
        system.start();
        
        System.out.println("🌤️  Weather System Interactive CLI");
        System.out.println("Type 'help' for commands");
        
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            
            if (input.equals("quit") || input.equals("exit")) {
                break;
            }
            
            processCommand(system, input);
        }
        
        system.shutdown();
    }
    
    private static void processCommand(WeatherSystem system, String command) {
        String[] parts = command.split("\\s+", 2);
        String action = parts[0].toLowerCase();
        
        switch (action) {
            case "weather":
                if (parts.length > 1) {
                    system.getWeatherForLocation(parts[1]);
                } else {
                    system.getCurrentWeather();
                }
                break;
                
            case "add":
                if (parts.length > 1) {
                    system.addLocation(parts[1]);
                    System.out.println("✅ Added location: " + parts[1]);
                }
                break;
                
            case "alerts":
                system.showActiveAlerts();
                break;
                
            case "stats":
                system.showStatistics();
                break;
                
            case "help":
                showHelp();
                break;
                
            default:
                System.out.println("❓ Unknown command. Type 'help' for available commands.");
        }
    }
}
```

### Configuration Options

```bash
# API Configuration
export OPENWEATHER_API_KEY=your_api_key
export WEATHER_UPDATE_INTERVAL=300  # seconds (5 minutes)
export WEATHER_ALERT_THRESHOLD_TEMP_HIGH=35  # Celsius
export WEATHER_ALERT_THRESHOLD_TEMP_LOW=-20  # Celsius
export WEATHER_ALERT_THRESHOLD_WIND=60      # km/h

# System Configuration
export WEATHER_MAX_LOCATIONS=50
export WEATHER_ENABLE_ALERTS=true
export WEATHER_LOG_LEVEL=INFO
```

### Running the Example

```bash
# With real API integration
export OPENWEATHER_API_KEY=your_actual_api_key
./scripts/run-weather-system.sh

# Demo mode (no API key required)
./scripts/run-weather-system.sh
```

### Sample CLI Session

```bash
$ ./scripts/run-weather-system.sh
🌤️  AMCP v1.5 - Weather System Demo
===================================

✅ OpenWeather API Key: configured
📍 Default Monitoring Locations:
   • New York, NY
   • London, UK
   • Tokyo, Japan
   • Sydney, Australia

🚀 Starting Weather Collection System...
Type 'help' for commands

> weather
🌤️  Current Weather Summary:
📍 New York, NY: 22°C, Sunny, Wind: 15 km/h
📍 London, UK: 18°C, Cloudy, Wind: 25 km/h  
📍 Tokyo, Japan: 28°C, Partly Cloudy, Wind: 10 km/h
📍 Sydney, Australia: 19°C, Rainy, Wind: 35 km/h

> add "Paris, France"
✅ Added location: Paris, France
📊 Now monitoring 5 locations

> alerts
🚨 Active Weather Alerts:
   • HIGH_WIND: Sydney, Australia - Wind speed 35 km/h
   
> stats
📊 System Statistics:
   • Uptime: 00:05:23
   • Total API Calls: 23
   • Locations Monitored: 5
   • Alerts Generated: 1
   • Average Response Time: 245ms
```

### Production Features

#### Health Monitoring
```java
public class WeatherSystemHealth {
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    
    public HealthStatus getHealth() {
        long requests = totalRequests.get();
        long failed = failedRequests.get();
        double successRate = requests > 0 ? (requests - failed) * 100.0 / requests : 100.0;
        double avgResponseTime = requests > 0 ? totalResponseTime.get() / (double) requests : 0;
        
        return HealthStatus.builder()
            .status(successRate > 95 ? "HEALTHY" : "DEGRADED")
            .successRate(successRate)
            .averageResponseTime(avgResponseTime)
            .totalRequests(requests)
            .build();
    }
}
```

#### Performance Optimization
- **Parallel Processing**: Concurrent weather data collection for multiple locations
- **Connection Pooling**: Reused HTTP connections for API calls
- **Caching**: Location data and API response caching
- **Rate Limiting**: Built-in API rate limiting to avoid service limits
- **Graceful Degradation**: Fallback to cached data when APIs are unavailable

### Learning Objectives
- ✅ Real-time data processing patterns
- ✅ External API integration with error handling
- ✅ Production-grade alerting systems
- ✅ Interactive CLI application development
- ✅ Performance optimization techniques
- ✅ Health monitoring and observability
- ✅ Configuration management
- ✅ Concurrent and parallel processing

---

## Common Patterns Across Examples

### 1. Error Handling Pattern
```java
@Override
public CompletableFuture<Void> handleEvent(Event event) {
    return CompletableFuture.runAsync(() -> {
        try {
            processEvent(event);
        } catch (Exception e) {
            logger.error("Event processing failed: {}", event.getTopic(), e);
            publishErrorEvent(e, event);
        }
    });
}
```

### 2. Resource Cleanup Pattern
```java
@Override
public void cleanup() {
    if (scheduler != null && !scheduler.isShutdown()) {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### 3. Configuration Pattern
```java
private void loadConfiguration() {
    apiKey = System.getenv("API_KEY");
    updateInterval = Integer.parseInt(
        System.getProperty("update.interval", "300"));
    enableAlerts = Boolean.parseBoolean(
        System.getProperty("enable.alerts", "true"));
}
```

### 4. Monitoring Pattern
```java
private final AtomicLong eventCount = new AtomicLong(0);
private final AtomicLong errorCount = new AtomicLong(0);

private void logStats() {
    long events = eventCount.get();
    long errors = errorCount.get();
    double errorRate = events > 0 ? (errors * 100.0 / events) : 0;
    
    logger.info("Stats: {} events, {} errors ({:.2f}% error rate)", 
               events, errors, errorRate);
}
```

## Running All Examples

### Quick Start
```bash
# Build everything
./scripts/build-all.sh

# Run each example
./scripts/run-greeting-agent.sh     # Basic concepts
./scripts/run-travel-planner.sh     # Advanced coordination  
./scripts/run-weather-system.sh     # Production patterns
```

### Development Workflow
```bash
# Test the examples
./scripts/run-tests.sh

# Verify build
mvn compile test

# Package for deployment
./scripts/deploy.sh local
```

These examples provide comprehensive coverage of AMCP v1.5 capabilities, from basic agent concepts to production-ready distributed systems. Use them as templates for building your own agent-based applications.