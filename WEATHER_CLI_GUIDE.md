# AMCP v1.5 Enterprise Edition Weather System CLI Guide

## 🌤️ Overview

The Weather System CLI demonstrates AMCP v1.5's distributed agent capabilities through a real-time weather monitoring system. This example showcases:

## Features

### 🌤️ **Real-Time Weather Monitoring**
- Continuous weather data collection for multiple cities
- 5-minute collection intervals with simulated weather APIs
- Automatic severe weather alert detection
- Real-time data visualization in CLI

### 🏙️ **Dynamic Location Management**
- Add/remove cities during runtime via CLI commands
- Instant weather data collection for new locations
- Persistent monitoring state management

### 🚀 **Agent Mobility Demonstration**
- Dispatch agents to edge devices
- Agent migration simulation
- Event-driven mobility operations
- Mobility success/failure tracking

### 📊 **Event-Driven Architecture**
- Pub/Sub messaging with hierarchical topics
- Real-time event broadcasting
- Asynchronous event processing
- Comprehensive event logging

## Quick Start

### Prerequisites
- Java 8 or higher
- Maven (optional, fallback compilation available)
- Terminal with bash support

### Running the Demo

```bash
# From AMCP project root
./run-weather-demo.sh
```

The launcher will:
1. 🔨 Compile AMCP core components
2. 🌤️ Build weather system examples
3. 🚀 Start the interactive CLI

### Alternative Manual Execution

```bash
# Compile core components
cd core && mvn compile && cd ..

# Compile examples
cd examples
javac -cp ../core/target/classes -d build/classes src/main/java/io/amcp/examples/weather/*.java

# Run CLI
java -cp "build/classes:../core/target/classes" io.amcp.examples.weather.WeatherSystemCLI
```

## Interactive Commands

### General Commands
```
help, h           Show available commands
status, s         Display system status and agent state
stats             Show detailed collection statistics
clear             Clear terminal screen
quit, exit, q     Exit the application
```

### Weather Operations
```
weather [city]    Get current weather (all cities or specific)
cities, c         List all monitored cities
add <city>        Add city to monitoring (e.g., "add Berlin")
remove <city>     Remove city from monitoring
alerts            Show severe weather alerts history
```

### Advanced Features
```
dispatch <device> Demonstrate agent mobility to edge device
```

## Example Session

```
╔═══════════════════════════════════════════════════════════════╗
║                AMCP v1.4 Weather System CLI                  ║
║          Agent Mesh Communication Protocol Demo              ║
╚═══════════════════════════════════════════════════════════════╝

weather> status
╔═══════════════════ SYSTEM STATUS ═══════════════════╗
║ Agent State: ACTIVE                                  ║
║ Monitored Cities: 5                                 ║
║ Collection Cycles: 3                                ║
║ Event Broker: Active                                ║
║ CLI Interface: Running                              ║
╚══════════════════════════════════════════════════════╝

weather> weather
🌍 Current Weather for All Cities:
═══════════════════════════════════════════════════════════
London:       8.2°C  Scattered clouds   💨3.4 m/s  💧67%
New York:    12.7°C  Clear sky          💨1.8 m/s  💧45%
Paris:       15.3°C  Light rain         💨5.2 m/s  💧78%
Sydney:      22.1°C  Few clouds         💨2.7 m/s  💧52%
Tokyo:       18.9°C  Overcast           💨4.1 m/s  💧61%

weather> add Berlin
🏙️  Adding Berlin to monitoring...

weather> dispatch edge-device-1
🚀 Demonstrating agent mobility to: edge-device-1

weather> alerts
⚠️  Severe Weather Alerts:
══════════════════════════════════════════════════
   • [15:23:45] Berlin: Extreme temperature detected (-22.3°C)
   • [15:18:12] Sydney: High wind speed alert (28.7 m/s)

weather> quit
🛑 Shutting down weather monitoring system...
✅ Weather system shutdown complete.
Thank you for using AMCP v1.4 Weather System!
```

## Architecture Highlights

### Agent Implementation
- **WeatherAgent**: Implements full AMCP v1.4 Agent interface
- **Event-Driven**: Asynchronous event handling with CompletableFuture
- **Thread-Safe**: Concurrent collections for multi-threaded operations
- **Lifecycle Management**: Proper activation, deactivation, and cleanup

### Event System
- **Hierarchical Topics**: `weather.**`, `location.add`, `alert.severe`
- **Event Publishing**: Structured events with metadata and delivery options
- **Pub/Sub Pattern**: Decoupled communication between components

### Mobility Operations
- **Dispatch**: Agent migration to edge devices
- **Clone**: Agent replication across contexts
- **Retract**: Agent retrieval from remote contexts
- **Event Tracking**: Mobility success/failure notifications

### Real-Time Processing
- **Scheduled Collection**: 5-minute intervals with ScheduledExecutorService
- **Immediate Updates**: On-demand weather requests
- **Alert Detection**: Automatic severe weather threshold monitoring
- **Live Visualization**: Real-time CLI updates

## Technical Details

### Simulated Weather Data
- City-specific temperature baselines with realistic variations
- Random weather conditions (clear, cloudy, rainy, overcast)
- Humidity, wind speed, and atmospheric pressure simulation
- Severe weather threshold detection (temperature extremes, high winds)

### Event-Driven Communication
```java
// Event publishing example
Event weatherUpdate = Event.builder()
    .topic("weather.data.updated")
    .payload(weatherData)
    .sender(getAgentId())
    .deliveryOptions(DeliveryOptions.RELIABLE)
    .build();
publishEvent(weatherUpdate);
```

### Agent Mobility
```java
// Mobility demonstration
dispatch(edgeDevice).thenRun(() -> {
    logMessage("Successfully dispatched to: " + edgeDevice);
    publishEvent("mobility.dispatch.success", edgeDevice);
});
```

## Extending the Demo

### Adding New Cities
Weather data simulation supports any city name with realistic temperature baselines for major cities (Paris, London, Tokyo, New York, Sydney).

### Custom Weather Sources
Replace `simulateWeatherAPICall()` with real API integration:
```java
private WeatherData callRealWeatherAPI(String city) {
    // Integrate with OpenWeatherMap, Weather.com, etc.
    return new WeatherData(city, temp, humidity, wind, pressure, description);
}
```

### Advanced Mobility
Extend mobility operations with real network communication:
```java
@Override
public CompletableFuture<Void> dispatch(String destinationContext) {
    // Implement actual agent serialization and network transfer
    return networkTransfer(this, destinationContext);
}
```

## Troubleshooting

### Compilation Issues
- Ensure Java 8+ compatibility (avoid modern switch expressions)
- Check classpath includes both core and examples
- Use fallback direct compilation if Maven fails

### Runtime Problems
- Verify agent activation successful in logs
- Check event broker initialization
- Monitor thread pool cleanup on shutdown

### Development Tips
- Use `clear` command for better terminal visibility
- Monitor logs for event flow understanding
- Try different cities to see temperature variations
- Test mobility operations with various device names

---

*This demo showcases the AMCP v1.4 framework's capabilities for building distributed, agent-based systems with event-driven communication and mobility features.*