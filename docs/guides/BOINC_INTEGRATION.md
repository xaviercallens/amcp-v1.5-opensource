# BOINC integration (volunteer computing bridge)

`io.amcp.connectors.boinc` connects a local [BOINC](https://boinc.berkeley.edu) client to an AMCP mesh
so agents can see and steer shared compute capacity.

## Setup
1. Run a BOINC client with GUI RPC enabled (default `127.0.0.1:31416`). Remote access needs
   `--allow_remote_gui_rpc` and the host listed in `remote_hosts.cfg`.
2. Read the password from `gui_rpc_auth.cfg` in the BOINC data directory.
3. Register the agent:

```java
BoincBridgeAgent bridge = new BoincBridgeAgent("127.0.0.1", 31416, password, 30, /*allowControl*/ false);
bridge.setContext(context);
context.registerAgent(bridge);
context.activateAgent(bridge.getAgentId());
```

## Topics
| Topic | Direction | Payload |
|---|---|---|
| `boinc.status` | published every poll interval | `{projects, tasks, ccStatus, timestamp}` |
| `boinc.command.status` | command | none; answer on `boinc.reply` |
| `boinc.command.runmode` | command (needs `allowControl`) | `{mode: always\|auto\|never, duration: seconds}` |
| `boinc.command.project` | command (needs `allowControl`) | `{url, op: suspend\|resume\|update\|nomorework\|allowmorework}` |
| `boinc.reply` / `boinc.error` | published | result / failure, same `correlationId` as the request |

The bridge is read-only by default. Commands are validated against fixed allow-lists and project URLs
are checked before being placed in XML, so a mesh message cannot inject arbitrary RPC.
