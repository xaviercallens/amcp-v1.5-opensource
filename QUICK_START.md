# 🚀 AMCP CLI Quick Start Guide

## ✅ **WORKING!** Your CLI is now ready to use!

### 🎯 **Quick Launch Commands**

```bash
# From your terminal in the project directory:
cd /Users/xcallens/xdev/private/amcp/amcp-v1.5-enterprise-edition

# Basic launch (will build if needed)
./amcp-cli --build

# Or just launch if already built
./amcp-cli
```

### 🔧 **Optional: Set API Keys for Full Functionality**

For stock and weather data, set these environment variables:

```bash
# Add to your ~/.zshrc for permanent setup:
export POLYGON_API_KEY="your_polygon_api_key_here"
export OPENWEATHER_API_KEY="your_openweather_api_key_here"

# Or set temporarily for one session:
POLYGON_API_KEY="your_key" OPENWEATHER_API_KEY="your_key" ./amcp-cli
```

### 🎮 **Essential Commands Once Inside CLI**

```bash
help                    # Show all available commands
agents                  # List all available agents
agent activate travel   # Activate specific agent
travel "Plan trip to Tokyo for 3 days"  # Interact with agents
stock AAPL             # Get stock price (needs API key)
weather "London"       # Get weather (needs API key)
status                 # Show system status
quit                   # Exit CLI
```

### 🎨 **What You'll See**

When you run `./amcp-cli --build`, you'll see:

```
  ╔══════════════════════════════════════════════════════════════════╗
  ║                    AMCP Interactive CLI v1.5                     ║
  ║              Agent Mesh Communication Protocol                   ║
  ║                     Enterprise Edition                           ║
  ╚══════════════════════════════════════════════════════════════════╝

🚀 AMCP Interactive CLI is ready!
Type 'help' for available commands or 'agents' to see registered agents.
Press Tab for autocompletion, Ctrl+C to exit.

🟢[eventbroker] 🔴[openweather] 🔴[polygon.io] amcp>
```

### 🎯 **Status Indicators**
- 🟢 **Green**: Service is active and working
- 🔴 **Red**: Service not configured or having issues  
- The broker is always green (in-memory), external APIs need keys

### 🏃‍♂️ **Ready to Go!**

Your AMCP Interactive CLI system is fully operational! The launcher script has been fixed and all components are working perfectly.

**Just run: `./amcp-cli --build`** and start interacting with your agents! 🎉