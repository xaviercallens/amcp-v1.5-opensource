# Enhanced Chat CLI Guide

## Overview

The Enhanced Chat CLI provides an interactive chat interface with the AMCP v1.5 multi-agent system, leveraging TinyLlama as the primary LLM. This CLI offers advanced features including conversation history, session management, command autocompletion, and real-time agent coordination console.

## Quick Start

### Prerequisites

1. **OLLAMA** must be running with TinyLlama model:
   ```bash
   # Install OLLAMA if not already installed
   curl -fsSL https://ollama.ai/install.sh | sh
   
   # Start OLLAMA and pull TinyLlama model
   ollama serve &
   ollama pull tinyllama
   ```

2. **Java 21** or higher must be installed

3. **Maven** for building the project

### Launch the CLI

```bash
cd examples
./run-chat-demo.sh
```

The script will:
- Build the project with Maven
- Compile the CLI classes
- Launch the interactive chat interface
- Display the agent coordination console

## CLI Features

### 1. Interactive Chat
- Natural language conversations with TinyLlama
- Multi-agent coordination for specialized tasks
- Real-time response processing

### 2. Session Management
```
/save <filename>     - Save current conversation to file
/load <filename>     - Load previous conversation from file
/history            - Display conversation history
/clear              - Clear current conversation
```

### 3. Agent Coordination Console
The CLI displays which agents are being called for each query:
```
[AGENT CONSOLE] EnhancedChatAgent processing query...
[AGENT CONSOLE] → Calling WeatherAgent for weather data
[AGENT CONSOLE] → Calling TravelPlannerAgent for route planning
[AGENT CONSOLE] ← Response received from agents
```

### 4. Command Autocompletion
- Tab completion for commands starting with `/`
- History navigation with up/down arrow keys
- Smart suggestions based on context

## Example Usage Scenarios

### 1. Get Weather in Nice

**Query:**
```
> What's the weather like in Nice today?
```

**Expected Response:**
```
[AGENT CONSOLE] EnhancedChatAgent processing weather query...
[AGENT CONSOLE] → Calling WeatherAgent for Nice, France weather data
[AGENT CONSOLE] ← Weather data received

Based on current weather data for Nice, France:
- Temperature: 22°C (72°F)
- Conditions: Sunny with light clouds
- Humidity: 65%
- Wind: 8 km/h from the southeast
- UV Index: 6 (High)

It's a beautiful day in Nice! Perfect weather for exploring the French Riviera.
```

**Alternative Queries:**
- "How's the weather in Nice this afternoon?"
- "Will it rain in Nice tomorrow?"
- "What should I wear for the weather in Nice?"

### 2. Get Amadeus Stock Options

**Query:**
```
> What are the current stock options for Amadeus?
```

**Expected Response:**
```
[AGENT CONSOLE] EnhancedChatAgent processing financial query...
[AGENT CONSOLE] → Calling StockPriceAgent for Amadeus IT Group data
[AGENT CONSOLE] ← Stock data received

Current Amadeus IT Group (AMS) stock information:
- Current Price: €62.45
- Daily Change: +1.23 (+2.01%)
- Volume: 1,245,678 shares
- Market Cap: €28.9B
- 52-Week Range: €54.20 - €71.30

Stock Options Analysis:
- Call Options (Near-the-money): €63 strike showing high volume
- Put Options: €60 strike with moderate interest
- Implied Volatility: 18.5%

The stock is showing bullish momentum today with strong volume.
```

**Alternative Queries:**
- "Should I buy Amadeus stock now?"
- "What's the Amadeus stock forecast?"
- "Compare Amadeus with other travel technology stocks"

### 3. Plan Travel from Nice to New York

**Query:**
```
> I need to travel from Nice to New York next week. What are my options?
```

**Expected Response:**
```
[AGENT CONSOLE] EnhancedChatAgent processing travel planning query...
[AGENT CONSOLE] → Calling TravelPlannerAgent for route optimization
[AGENT CONSOLE] → Calling WeatherAgent for destination weather
[AGENT CONSOLE] ← Travel options and weather data received

Here are your best travel options from Nice to New York:

**Flight Options:**
1. **Air France via Paris CDG** (Recommended)
   - Departure: Nice (NCE) 07:15 → Paris (CDG) 08:45
   - Connection: Paris (CDG) 11:30 → New York (JFK) 14:50
   - Duration: 11h 35min total
   - Price: €650-850

2. **Lufthansa via Frankfurt**
   - Departure: Nice (NCE) 14:20 → Frankfurt (FRA) 16:00
   - Connection: Frankfurt (FRA) 18:45 → New York (JFK) 22:15
   - Duration: 12h 55min total
   - Price: €700-900

**New York Weather (Destination):**
- Expected Temperature: 18°C (64°F)
- Conditions: Partly cloudy
- Recommendation: Pack layers and a light jacket

**Tips:**
- Book 2-3 weeks in advance for best prices
- Consider Premium Economy for long-haul comfort
- JFK has excellent public transport to Manhattan
```

**Alternative Queries:**
- "What's the cheapest flight from Nice to NYC?"
- "When is the best time to visit New York from Nice?"
- "Can you help me plan a complete Nice to New York itinerary?"

## Advanced Commands

### Session Commands
```
/save my-nice-trip          # Save conversation about Nice trip
/load my-nice-trip          # Load previous Nice trip conversation
/history                    # View all messages in current session
/clear                      # Start fresh conversation
/status                     # Show agent status and connections
/help                       # Display all available commands
/quit or /exit              # Exit the CLI
```

### Multi-Agent Queries
You can ask complex questions that involve multiple agents:

```
> Plan a 3-day trip to New York, check the weather forecast, and find the best Amadeus stock entry point for travel industry exposure.
```

This will trigger:
- TravelPlannerAgent for itinerary planning
- WeatherAgent for forecast data
- StockPriceAgent for Amadeus analysis
- EnhancedChatAgent for synthesis and recommendations

## Configuration

### API Keys
The system uses default API keys for demonstration purposes. For production use, set your own keys:

```bash
export OPENWEATHER_API_KEY="your_weather_api_key"
export FINNHUB_API_KEY="your_stock_api_key"
export AMADEUS_API_KEY="your_amadeus_travel_api_key"
```

### TinyLlama Model Configuration
The system is configured to use TinyLlama by default. To use a different OLLAMA model:

```bash
export OLLAMA_MODEL="llama2"  # or any other OLLAMA model
```

## Troubleshooting

### OLLAMA Not Running
```
Error: Could not connect to OLLAMA service

Solution:
1. Start OLLAMA: ollama serve &
2. Verify TinyLlama is available: ollama list
3. Pull if missing: ollama pull tinyllama
```

### Agent Connection Issues
```
[ERROR] Agent coordination failed

Solution:
1. Check all required modules are compiled
2. Verify classpath includes core and connectors
3. Restart the CLI with ./run-chat-demo.sh
```

### Memory Issues
```
OutOfMemoryError during chat processing

Solution:
1. Increase JVM heap: export MAVEN_OPTS="-Xmx2048m"
2. Clear conversation history: /clear
3. Restart with fresh session
```

## Performance Tips

1. **Batch Related Queries**: Ask follow-up questions in the same session to maintain context
2. **Use Specific Locations**: "Nice, France" vs "Nice" for better agent accuracy
3. **Save Important Sessions**: Use `/save` for conversations you want to reference later
4. **Monitor Agent Console**: Watch which agents are called to understand system behavior

## Development and Extension

The CLI is built on the AMCP v1.5 architecture and can be extended with additional agents:

1. **Add New Agents**: Implement agents following the AMCP patterns
2. **Custom Commands**: Extend the command parser for domain-specific operations
3. **Integration**: Connect with external APIs and services
4. **Persistence**: Enhance session storage with databases or cloud storage

## Support

- **Documentation**: See AMCP v1.5 specification for architecture details
- **Examples**: Additional examples in the `/examples` directory
- **Issues**: Report issues to the AMCP Enterprise Edition repository
- **Community**: Join AMCP developer discussions for support and contributions

---

**Note**: This CLI demonstrates the power of AMCP's multi-agent architecture with real-world use cases. The examples use live API data where available and provide realistic simulations for demonstration purposes.