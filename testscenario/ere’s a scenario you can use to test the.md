ere’s a scenario you can use to test the LLM-based orchestration in AMCP v1.5 using the existing Weather Agent and Travel Planner Agent: 

 

🧪 Test Scenario: “Plan My Trip to Tokyo Next Week” 

🎯 Objective 

Validate that the Orchestrator Agent can: 

Interpret a natural language travel request using an LLM. 

Delegate subtasks to the Weather Agent and Travel Planner Agent. 

Aggregate results and return a coherent travel plan. 

 

🧵 Flow Overview 

User Input:\ 

"Plan my trip to Tokyo next week. I want to know the weather and get travel suggestions." 

Orchestrator LLM Planning:\ 

The LLM returns a plan like: 

1     [ 

2       { "capability": "weather.get", "params": { "location": "Tokyo", "date": "next week" } }, 

3       { "capability": "travel.plan", "params": { "destination": "Tokyo", "date": "next week" } } 

4     ] 

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

1     gateway.send_user_request("Plan my trip to Tokyo next week. I want to know the weather and get travel suggestions.") 

2      

3     # Wait for orchestrator to process and respond 

4     response = gateway.await_response(correlation_id="REQ001") 

5      

6     print(response.data["answer"]) 

 

✅ Success Criteria 

Component 

Expected Behavior 

Orchestrator 

Generates correct task plan and dispatches events 

Weather Agent 

Returns weather forecast for Tokyo 

Travel Planner 

Returns itinerary and budget 

Final Response 

Combines both results into a coherent message 

 

Would you like me to generate a full test harness or simulate the LLM prompt and response for this scenario? Or explore how to extend this with mobility (e.g., dispatching a mobile agent to Tokyo context)? 

1     # ---- Test Harness for Orchestrated Multi-Agent Scenario ---- 

2      

3     # Setup: define a minimal Event class to simulate CloudEvents structure 

4     class Event: 

5         def __init__(self, type, source, id=None, subject=None, correlation_id=None, data=None): 

6             self.type = type               # e.g., "user.request", "task.request", etc. 

7             self.source = source           # origin of the event (agent ID or user) 

8             self.id = id                   # unique event ID (for requests) 

9             self.subject = subject         # specific subject (e.g., capability for task events) 

10             self.correlation_id = correlation_id  # links task events to a user request 

11             self.data = data or {}         # payload data (dict) 

12      

13         def __repr__(self): 

14             return f"Event(type={self.type}, source={self.source}, subject={self.subject}, corr_id={self.correlation_id}, data={self.data})" 

15      

16     # Define OrchestratorAgent with an LLM (simulated) and internal registry 

17     class OrchestratorAgent: 

18         def __init__(self, agent_id): 

19             self.id = agent_id 

20             self.capability_registry = {}   # Maps capability -> agent_id 

21             self.pending = {}               # Tracks pending tasks for each request (corr_id -> count & partial results) 

22      

23         def register_agent(self, agent_id, capabilities): 

24             # Update registry for each capability 

25             for cap in capabilities: 

26                 self.capability_registry[cap] = agent_id 

27             print(f"[Orchestrator] Registered agent '{agent_id}' with capabilities {capabilities}") 

28      

29         def handle_user_request(self, event): 

30             req_id = event.id  # Use user request's ID as correlation_id for subtasks 

31             user_query = event.data.get("query") 

32             print(f"\n[Orchestrator] Received user request: '{user_query}' (id={req_id})") 

33      

34             # LLM Planning Prompt 

35             cap_list = list(self.capability_registry.keys()) 

36             llm_prompt = (f"User asks: '{user_query}'.\n" 

37                           f"Available capabilities: {cap_list}.\n" 

38                           "Plan the steps in JSON.") 

39             print(f"[LLM Prompt] {llm_prompt}") 

40      

41             # Simulate LLM Response (JSON plan for tasks) 

42             llm_plan = [ 

43                 {"capability": "weather.get", "params": {"location": "Tokyo", "date": "next week"}}, 

44                 {"capability": "travel.plan", "params": {"destination": "Tokyo", "date": "next week"}} 

45             ] 

46             import json 

47             print("[LLM Response] Plan =", json.dumps(llm_plan, indent=2)) 

48      

49             # Initialize pending tracking for this request 

50             self.pending[req_id] = {"tasks_left": len(llm_plan), "results": {}} 

51      

52             # Dispatch each task as an event to the respective agent 

53             for task in llm_plan: 

54                 cap = task["capability"] 

55                 params = task.get("params", {}) 

56                 target_agent_id = self.capability_registry.get(cap) 

57                 if not target_agent_id: 

58                     print(f"[Orchestrator] No agent for capability '{cap}'!") 

59                     self.pending[req_id]["tasks_left"] -= 1 

60                     self.pending[req_id]["results"][cap] = {"error": "no agent available"} 

61                     continue 

62                 # Create task.request event 

63                 task_event = Event(type="task.request", source=self.id, 

64                                    subject=cap, correlation_id=req_id, data=params) 

65                 print(f"[Orchestrator] -> Dispatching {task_event.subject} task to agent '{target_agent_id}'") 

66                 agents[target_agent_id].handle_task_request(task_event)  # invoke target agent's handler 

67      

68         def handle_task_response(self, event): 

69             corr_id = event.correlation_id 

70             cap = event.subject  # which capability responded 

71             self.pending[corr_id]["results"][cap] = event.data 

72             self.pending[corr_id]["tasks_left"] -= 1 

73             print(f"[Orchestrator] <- Received response for '{cap}' (remaining tasks = {self.pending[corr_id]['tasks_left']})") 

74             # If all tasks finished, compile final answer 

75             if self.pending[corr_id]["tasks_left"] == 0: 

76                 self._compose_and_reply(corr_id) 

77      

78         def _compose_and_reply(self, corr_id): 

79             results = self.pending[corr_id]["results"] 

80             # Extract or format results for final answer 

81             weather_info = results.get("weather.get", {}) 

82             travel_info = results.get("travel.plan", {}) 

83             forecast = weather_info.get("forecast", "unknown weather") 

84             itinerary = travel_info.get("itinerary", "") 

85             budget = travel_info.get("budget", "") 

86             # LLM Final Answer Prompt 

87             compose_prompt = (f"The user wanted a Tokyo trip plan.\n" 

88                               f"Weather result: {forecast}.\n" 

89                               f"Travel result: {itinerary} Budget: {budget}.\n" 

90                               "Provide a combined answer.") 

91             print(f"\n[LLM Final Prompt] {compose_prompt}") 

92             # Simulate LLM final answer 

93             combined_answer = (f"Next week in Tokyo will be {forecast.lower()}. " 

94                                 f"{itinerary} Estimated budget: {budget}.") 

95             print(f"[LLM Final Answer] {combined_answer}") 

96             # Create user.response event 

97             response_event = Event(type="user.response", source=self.id, 

98                                    correlation_id=corr_id, data={"answer": combined_answer}) 

99             # Simulate sending response back to user (via gateway) 

100             print(f"\n[Orchestrator] Final Response Event: {response_event}") 

101             print(f"--> Final Answer to User: {combined_answer}") 

102      

103     # Define specialist agents with simple task handlers 

104     class WeatherAgent: 

105         def __init__(self, agent_id): 

106             self.id = agent_id 

107      

108         def handle_task_request(self, event): 

109             # Simulate processing a weather.get task 

110             location = event.data.get("location") 

111             date = event.data.get("date") 

112             forecast = "sunny with mild temperatures"  # Dummy forecast 

113             # Create task.response event with forecast 

114             response = Event(type="task.response", source=self.id, 

115                              subject="weather.get", correlation_id=event.correlation_id, 

116                              data={"forecast": f"Sunny and mild in {location} {date}."}) 

117             print(f"[WeatherAgent] Completed weather task for {location}.") 

118             # Send response back to orchestrator 

119             orchestrator.handle_task_response(response) 

120      

121     class TravelPlannerAgent: 

122         def __init__(self, agent_id): 

123             self.id = agent_id 

124      

125         def handle_task_request(self, event): 

126             # Simulate processing a travel.plan task 

127             destination = event.data.get("destination") 

128             date = event.data.get("date") 

129             itinerary = "Visit Shibuya Crossing, Meiji Shrine, Tsukiji Market." 

130             budget = "$1200" 

131             response = Event(type="task.response", source=self.id, 

132                              subject="travel.plan", correlation_id=event.correlation_id, 

133                              data={"itinerary": itinerary, "budget": budget}) 

134             print(f"[TravelPlannerAgent] Completed travel plan for {destination}.") 

135             orchestrator.handle_task_response(response) 

136      

137     # ---- Initialize agents and orchestrator ---- 

138     orchestrator = OrchestratorAgent(agent_id="Orchestrator1") 

139     weather_agent = WeatherAgent(agent_id="WeatherAgent1") 

140     travel_agent = TravelPlannerAgent(agent_id="TravelAgent1") 

141      

142     # Register agents in orchestrator's registry (simulate agent.register events) 

143     agents = { 

144         "WeatherAgent1": weather_agent, 

145         "TravelAgent1": travel_agent 

146     } 

147     orchestrator.register_agent("WeatherAgent1", ["weather.get"]) 

148     orchestrator.register_agent("TravelAgent1", ["travel.plan"]) 

149      

150     # ---- Simulate a user request event ---- 

151     user_request = Event(type="user.request", source="UserAlice", id="REQ001", 

152                          data={"query": "Plan my trip to Tokyo next week. I want to know the weather and get travel suggestions."}) 

153     # Orchestrator handles the user request, triggering the whole workflow 

154      

1     # ---- Test Harness for Orchestrated Multi-Agent Scenario ---- 

2      

3     # Setup: define a minimal Event class to simulate CloudEvents structure 

4     class Event: 

5         def __init__(self, type, source, id=None, subject=None, correlation_id=None, data=None): 

6             self.type = type               # e.g., "user.request", "task.request", etc. 

7             self.source = source           # origin of the event (agent ID or user) 

8             self.id = id                   # unique event ID (for requests) 

9             self.subject = subject         # specific subject (e.g., capability for task events) 

10             self.correlation_id = correlation_id  # links task events to a user request 

11             self.data = data or {}         # payload data (dict) 

12      

13         def __repr__(self): 

14             return f"Event(type={self.type}, source={self.source}, subject={self.subject}, corr_id={self.correlation_id}, data={self.data})" 

15      

16     # Define OrchestratorAgent with an LLM (simulated) and internal registry 

17     class OrchestratorAgent: 

18         def __init__(self, agent_id): 

19             self.id = agent_id 

20             self.capability_registry = {}   # Maps capability -> agent_id 

21             self.pending = {}               # Tracks pending tasks for each request (corr_id -> count & partial results) 

22      

23         def register_agent(self, agent_id, capabilities): 

24             # Update registry for each capability 

25             for cap in capabilities: 

26                 self.capability_registry[cap] = agent_id 

27             print(f"[Orchestrator] Registered agent '{agent_id}' with capabilities {capabilities}") 

28      

29         def handle_user_request(self, event): 

30             req_id = event.id  # Use user request's ID as correlation_id for subtasks 

31             user_query = event.data.get("query") 

32             print(f"\n[Orchestrator] Received user request: '{user_query}' (id={req_id})") 

33      

34             # LLM Planning Prompt 

35             cap_list = list(self.capability_registry.keys()) 

36             llm_prompt = (f"User asks: '{user_query}'.\n" 

37                           f"Available capabilities: {cap_list}.\n" 

38                           "Plan the steps in JSON.") 

39             print(f"[LLM Prompt] {llm_prompt}") 

40      

41             # Simulate LLM Response (JSON plan for tasks) 

42             llm_plan = [ 

43                 {"capability": "weather.get", "params": {"location": "Tokyo", "date": "next week"}}, 

44                 {"capability": "travel.plan", "params": {"destination": "Tokyo", "date": "next week"}} 

45             ] 

46             import json 

47             print("[LLM Response] Plan =", json.dumps(llm_plan, indent=2)) 

48      

49             # Initialize pending tracking for this request 

50             self.pending[req_id] = {"tasks_left": len(llm_plan), "results": {}} 

51      

52             # Dispatch each task as an event to the respective agent 

53             for task in llm_plan: 

54                 cap = task["capability"] 

55                 params = task.get("params", {}) 

56                 target_agent_id = self.capability_registry.get(cap) 

57                 if not target_agent_id: 

58                     print(f"[Orchestrator] No agent for capability '{cap}'!") 

59                     self.pending[req_id]["tasks_left"] -= 1 

60                     self.pending[req_id]["results"][cap] = {"error": "no agent available"} 

61                     continue 

62                 # Create task.request event 

63                 task_event = Event(type="task.request", source=self.id, 

64                                    subject=cap, correlation_id=req_id, data=params) 

65                 print(f"[Orchestrator] -> Dispatching {task_event.subject} task to agent '{target_agent_id}'") 

66                 agents[target_agent_id].handle_task_request(task_event)  # invoke target agent's handler 

67      

68         def handle_task_response(self, event): 

69             corr_id = event.correlation_id 

70             cap = event.subject  # which capability responded 

71             self.pending[corr_id]["results"][cap] = event.data 

72             self.pending[corr_id]["tasks_left"] -= 1 

73             print(f"[Orchestrator] <- Received response for '{cap}' (remaining tasks = {self.pending[corr_id]['tasks_left']})") 

74             # If all tasks finished, compile final answer 

75             if self.pending[corr_id]["tasks_left"] == 0: 

76                 self._compose_and_reply(corr_id) 

77      

78         def _compose_and_reply(self, corr_id): 

79             results = self.pending[corr_id]["results"] 

80             # Extract or format results for final answer 

81             weather_info = results.get("weather.get", {}) 

82             travel_info = results.get("travel.plan", {}) 

83             forecast = weather_info.get("forecast", "unknown weather") 

84             itinerary = travel_info.get("itinerary", "") 

85             budget = travel_info.get("budget", "") 

86             # LLM Final Answer Prompt 

87             compose_prompt = (f"The user wanted a Tokyo trip plan.\n" 

88                               f"Weather result: {forecast}.\n" 

89                               f"Travel result: {itinerary} Budget: {budget}.\n" 

90                               "Provide a combined answer.") 

91             print(f"\n[LLM Final Prompt] {compose_prompt}") 

92             # Simulate LLM final answer 

93             combined_answer = (f"Next week in Tokyo will be {forecast.lower()}. " 

94                                 f"{itinerary} Estimated budget: {budget}.") 

95             print(f"[LLM Final Answer] {combined_answer}") 

96             # Create user.response event 

97             response_event = Event(type="user.response", source=self.id, 

98                                    correlation_id=corr_id, data={"answer": combined_answer}) 

99             # Simulate sending response back to user (via gateway) 

100             print(f"\n[Orchestrator] Final Response Event: {response_event}") 

101             print(f"--> Final Answer to User: {combined_answer}") 

102      

103     # Define specialist agents with simple task handlers 

104     class WeatherAgent: 

105         def __init__(self, agent_id): 

106             self.id = agent_id 

107      

108         def handle_task_request(self, event): 

109             # Simulate processing a weather.get task 

110             location = event.data.get("location") 

111             date = event.data.get("date") 

112             forecast = "sunny with mild temperatures"  # Dummy forecast 

113             # Create task.response event with forecast 

114             response = Event(type="task.response", source=self.id, 

115                              subject="weather.get", correlation_id=event.correlation_id, 

116                              data={"forecast": f"Sunny and mild in {location} {date}."}) 

117             print(f"[WeatherAgent] Completed weather task for {location}.") 

118             # Send response back to orchestrator 

119             orchestrator.handle_task_response(response) 

120      

121     class TravelPlannerAgent: 

122         def __init__(self, agent_id): 

123             self.id = agent_id 

124      

125         def handle_task_request(self, event): 

126             # Simulate processing a travel.plan task 

127             destination = event.data.get("destination") 

128             date = event.data.get("date") 

129             itinerary = "Visit Shibuya Crossing, Meiji Shrine, Tsukiji Market." 

130             budget = "$1200" 

131             response = Event(type="task.response", source=self.id, 

132                              subject="travel.plan", correlation_id=event.correlation_id, 

133                              data={"itinerary": itinerary, "budget": budget}) 

134             print(f"[TravelPlannerAgent] Completed travel plan for {destination}.") 

135             orchestrator.handle_task_response(response) 

136      

137     # ---- Initialize agents and orchestrator ---- 

138     orchestrator = OrchestratorAgent(agent_id="Orchestrator1") 

139     weather_agent = WeatherAgent(agent_id="WeatherAgent1") 

140     travel_agent = TravelPlannerAgent(agent_id="TravelAgent1") 

141      

142     # Register agents in orchestrator's registry (simulate agent.register events) 

143     agents = { 

144         "WeatherAgent1": weather_agent, 

145         "TravelAgent1": travel_agent 

146     } 

147     orchestrator.register_agent("WeatherAgent1", ["weather.get"]) 

148     orchestrator.register_agent("TravelAgent1", ["travel.plan"]) 

149      

150     # ---- Simulate a user request event ---- 

151     user_request = Event(type="user.request", source="UserAlice", id="REQ001", 

152                          data={"query": "Plan my trip to Tokyo next week. I want to know the weather and get travel suggestions."}) 

153     # Orchestrator handles the user request, triggering the whole workflow 

154      