# MeshChat Quick Start Guide

## What is MeshChat?

MeshChat is an intelligent conversational AI system that can help you with:
- **Travel Planning** - Plan trips, get destination info, budget estimates
- **Financial Analysis** - Stock quotes, market insights, investment advice  
- **General Chat** - Natural conversations with AI assistance
- **Multi-Agent Tasks** - Complex requests requiring multiple specialists

## 5-Minute Quick Start

### 1. Run the Demo
```bash
cd /path/to/amcp-v1.5-enterprise-edition
./run-meshchat-full-demo.sh
```

### 2. Choose Interactive Mode
When prompted, select:
```
1) Interactive MeshChat Demo
```

### 3. Start Chatting!

Try these example conversations:

#### Travel Planning
```
> Plan a 3-day trip to Tokyo with a $2000 budget

🎌 Tokyo Travel Plan (3 Days, $2000 Budget):

Day 1: Cultural Immersion
- Morning: Visit Senso-ji Temple and Asakusa district
- Afternoon: Explore traditional Ueno district and museums
- Evening: Experience Shibuya crossing and dinner in Harajuku
Estimated cost: $180 (meals, transport, entrance fees)

Day 2: Modern Tokyo
- Morning: Tokyo Skytree observation deck
- Afternoon: Shopping in Ginza district
- Evening: Dinner in Roppongi with Tokyo Tower night view
Estimated cost: $220 (activities, meals, shopping)

Day 3: Food and Markets
- Morning: Tsukiji Outer Market food tour
- Afternoon: Traditional tea ceremony experience
- Evening: Farewell dinner at a high-end sushi restaurant
Estimated cost: $200 (food experiences, cultural activities)

Accommodation: 3 nights at mid-range hotel: $450
Transportation: 3-day Tokyo Metro pass: $30
Total estimated cost: $1,080 (well under your $2000 budget!)

Best time to visit: Spring (cherry blossoms) or fall (autumn colors)
Cultural tip: Always bow when greeting, and remove shoes when entering homes or temples
```

#### Stock Analysis
```
> What's the current price of Apple stock and should I invest?

📈 Apple Inc. (AAPL) Analysis:

Current Price: $185.42 (-1.2% today)
52-Week Range: $164.08 - $199.62
Market Cap: $2.87T
P/E Ratio: 29.1

📊 Technical Analysis:
- Recent trend: Slight downward pressure but holding above key support
- Volume: Normal trading volume, no unusual activity
- Moving averages: Trading above 50-day MA ($178), below 200-day MA ($188)

💡 Investment Recommendation: MODERATE BUY
- Strong fundamentals with iPhone 15 success and Services growth
- AI initiatives positioning for future growth
- Dividend yield: 0.5% provides steady income
- Risk level: Moderate (large-cap stability with growth potential)

⚠️ Considerations:
- China market concerns affecting growth projections
- High valuation compared to historical averages
- Interest rate sensitivity for tech stocks

Best for: Long-term investors seeking quality growth with stability
Position sizing: Consider 3-5% of portfolio maximum for individual stock exposure
```

#### Multi-Agent Coordination
```
> I'm planning a business trip to New York and want to research tech stocks while I'm there

🏙️ Business Trip to New York + Tech Investment Research:

## New York Business Travel Plan:
📍 Recommended Business District: Midtown Manhattan
- Stay near: Times Square/Theater District or Financial District
- Business-friendly hotels: The Plaza, Marriott Marquis, or W New York
- Transportation: Purchase 7-day MetroCard for easy subway access
- Business dining: Le Bernardin, Eleven Madison Park for client entertainment

Estimated 5-day business trip cost: $3,500
- Accommodation: $2,000 (premium business hotel)
- Meals: $800 (business dining and entertainment)
- Transportation: $200 (taxis, subway, airport transfers)
- Miscellaneous: $500 (business supplies, entertainment)

## Tech Stock Research Opportunities:
📈 NYSE/NASDAQ Proximity Advantage:
While in NYC, you'll be in the heart of financial markets! Consider:

**Top Tech Stocks to Research:**
1. **Microsoft (MSFT)** - $378.42 (+0.8%)
   - AI leadership with OpenAI partnership
   - Cloud growth through Azure platform
   - Strong enterprise software moat

2. **NVIDIA (NVDA)** - $502.18 (+2.1%)
   - AI chip market dominance
   - Data center growth acceleration
   - Gaming and automotive expansion

3. **Alphabet (GOOGL)** - $142.38 (-0.5%)
   - Search advertising resilience
   - Cloud computing growth
   - AI Bard development

**Research Activities in NYC:**
- Visit NYSE visitor center for market insights
- Attend tech meetups in Brooklyn/Manhattan
- Network at fintech events in Financial District
- Consider visiting Nasdaq MarketSite in Times Square

💡 Investment Strategy Recommendation:
Consider a diversified tech ETF (like QQQ) alongside individual stock picks
Target allocation: 15-20% of portfolio in tech sector
Research companies with strong NYC presence for local insights

Safe travels and happy investing! 🚀
```

## Key Features Demonstrated

### 🧠 Intelligent Routing
- Travel questions automatically go to travel specialist
- Stock queries route to financial analyst
- Complex requests coordinate multiple agents

### 💭 Context Awareness
- Remembers your conversation history
- Builds on previous topics
- Provides personalized recommendations

### 🔄 Multi-Agent Coordination
- Seamlessly combines travel and financial expertise
- Synthesizes information from multiple sources
- Provides comprehensive, actionable advice

## Available Commands

While chatting, you can also use:
- `help` - Show available commands
- `clear` - Clear conversation history
- `quit` - Exit the system

## Predefined Scenarios

Instead of interactive mode, try predefined scenarios:

```bash
./run-meshchat-full-demo.sh --scenario travel
./run-meshchat-full-demo.sh --scenario stock  
./run-meshchat-full-demo.sh --scenario multiagent
```

## What's Happening Behind the Scenes?

1. **Your Message** → MeshChatAgent receives and analyzes
2. **Intent Detection** → System determines if it's travel, financial, or general
3. **Orchestration** → Routes to appropriate specialist agents
4. **Processing** → Specialist agents handle the request
5. **Response Synthesis** → Results are combined into helpful answer
6. **Memory Storage** → Conversation context is saved for follow-ups

## Next Steps

- Explore the [Complete Documentation](MESHCHAT_DOCUMENTATION.md)
- Check out the [Architecture Guide](../MULTIAGENT_SYSTEM_GUIDE.md)
- Review [Deployment Options](../deploy/) for production use
- Try the [CLI Interface](../examples/CHAT_CLI_GUIDE.md) for advanced features

## Need Help?

- Run `./run-meshchat-full-demo.sh --help` for demo options
- Check the [Troubleshooting Guide](MESHCHAT_DOCUMENTATION.md#troubleshooting)
- Review [GitHub Issues](https://github.com/xaviercallens/amcp-enterpriseedition/issues) for known problems

---

**Ready to try it?** Run the demo and start chatting! 🚀