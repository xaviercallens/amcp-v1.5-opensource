Test Scenario: “Plan My Trip to Tokyo Next Week”
🎯 Objective
Validate that the Orchestrator Agent can:

Interpret a natural language travel request using an LLM.
Delegate subtasks to the Weather Agent and Travel Planner Agent.
Aggregate results and return a coherent travel plan.


🧵 Flow Overview


User Input:
"Plan my trip to Tokyo next week. I want to know the weather and get travel suggestions."


Orchestrator LLM Planning:
The LLM returns a plan like:
JSON[  { "capability": "weather.get", "params": { "location": "Tokyo", "date": "next week" } },  { "capability": "travel.plan", "params": { "destination": "Tokyo", "date": "next week" } }]Show more lines


Task Dispatch:

task.request.weather.get → Weather Agent
task.request.travel.plan → Travel Planner Agent



Agent Responses:

Weather Agent returns: "Sunny with mild temperatures expected in Tokyo next week."
Travel Planner Agent returns: "Suggested itinerary: Visit Shibuya, Meiji Shrine, and Tsukiji Market. Budget: $1200."



Orchestrator Aggregation:

Composes final response using LLM:

“Next week in Tokyo will be sunny and mild. Recommended itinerary includes Shibuya, Meiji Shrine, and Tsukiji Market. Estimated budget: $1200.”





Response Delivery:

user.response event sent back to Gateway or client.




🧪 Test Script (Pseudocode)
Pythongateway.send_user_request("Plan my trip to Tokyo next week. I want to know the weather and get travel suggestions.")# Wait for orchestrator to process and respondresponse = gateway.await_response(correlation_id="REQ001")print(response.data["answer"])Show more lines

✅ Success Criteria

























ComponentExpected BehaviorOrchestratorGenerates correct task plan and dispatches eventsWeather AgentReturns weather forecast for TokyoTravel PlannerReturns itinerary and budgetFinal ResponseCombines both results into a coherent message