# Server Communication Architecture

Three Kingdoms MMO game server communication reference — use this when setting up a new server instance or extending the architecture.

---

## 1. Server Components & Ports

| Component | Type | Port | Purpose |
|---|---|---|---|
| Game Logic Server | **TCP Socket** (Netty) | **16001** | Main game client connections |
| River HTTP Server | **HTTP** (com.sun.net.httpserver) | **46001** | Internal APIs: payments, GM commands, CD keys |
| Admin Portal (enicenuc) | **HTTP/WAR** (Tomcat) | **8080** | Web admin panel, account/guild/order management |
| User Portal (enicenuser) | **HTTP/WAR** (Tomcat) | **8080** | User-facing web services |
| Syslog | **UDP** | **514** | System logging, facility LOCAL0 |
| MongoDB | TCP | **27017** | Game state database |
| MySQL (MariaDB) | TCP | **3306** | Relational data via Hibernate |

Config file: `n2-server-master/data/conf/gls.properties`

```properties
server_logic_port=16001
river.port=46001
syslog.port=514
syslog.protocol=udp
```

---

## 2. Client ↔ Game Server (TCP, Port 16001)

### Transport

- **Protocol:** TCP plain socket (NOT WebSocket, NOT UDP)
- **Library:** Netty 4.0.29.Final
- **Server channel:** `NioServerSocketChannel` (Linux: `EpollServerSocketChannel`)
- **TCP options:**
  - `TCP_NODELAY = true` — disables Nagle's algorithm, reduces latency
  - `SO_LINGER = 0` — immediate socket close
  - Idle timeout: **300 seconds** (`IdleStateHandler`)
- **Max concurrent connections:** 3000
- **Thread model:**
  - Boss threads: 1
  - Worker threads: `availableProcessors × 2 × 4`
  - Logic handler threads: 128 (`DefaultEventExecutorGroup "wz-logic-%d"`)

Key file: `wz_game_logic/src/main/java/game/server/GameLogicServer.java`

### Packet Wire Format

```
┌────────────────────────────────────────────────┐
│  4 bytes  │  payload length (big-endian int32)  │
│  4 bytes  │  OpCode / cmd (int32)               │
│  4 bytes  │  Type (int32)                       │
│  N bytes  │  Protobuf-serialized message body   │
└────────────────────────────────────────────────┘
```

- Header is **4 bytes** (length prefix only)
- OpCode and Type are part of the payload, read after framing
- Max frame size: **1 MB** (1,048,576 bytes)
- Warning logged for packets > 10 KB
- Byte order: **BIG_ENDIAN** (configurable via `byte.order` system property)

Key files:
- `wz_game_logic/src/main/java/game/net/codec/Protocol.java` — frame decoder (2-state: STATUS_HEAD → STATUS_CONTENT)
- `wz_game_logic/src/main/java/game/packet/ResPacketCreator.java` — response packet assembly

### Serialization: Protocol Buffers v2.5.0

All game messages are serialized with **Google Protocol Buffers (proto2)**. There are **37 compiled message classes** under `wz_common/src/main/java/common/protobuf/`:

| Protobuf Class | Domain |
|---|---|
| `LoginPb.java` | Login / authentication |
| `CommonPb.java` | Shared/generic messages |
| `HeroPb.java` | Hero/character data |
| `BuildPb.java` | Building construction |
| `BattlePb.java` | Battle results |
| `WorldmapPb.java` | World map state |
| `GuildPb.java` | Guild operations |
| `ChatPb.java` | Chat messages |
| `MailPb.java` | In-game mail |
| `EquipPb.java` | Equipment |
| `TaskPb.java` | Quest / task system |
| `ActivityPb.java` | Events / activities |
| `ArenaGuildPb.java` | Guild arena |
| `FriendPb.java` | Friend system |
| `TavernPb.java` | Tavern / recruitment |
| `TeamPb.java` | Team / party |
| `BanquetPb.java` | Banquet feature |
| ... | 20+ more domain files |

> **FlatBuffers** library is imported (`wz_common/src/main/java/com/google/flatbuffers/`) but is **not actively used** in any packet handler. Ignore it for new setup.

### Obfuscation (XOR)

Incoming payloads are XOR-decoded before protobuf parsing:

```java
// GameLogicHandler.java line ~89
MathUtil.xor(content, GameLogicServer.protocolKey.getBytes());
// Key: "7a09da57be8c152c"  (defined in GameLogicServer.java line 58)
```

- **Outgoing packets are NOT XOR'd** (that line is commented out in ResPacketCreator.java:315)
- This is lightweight obfuscation only — not cryptographic encryption
- No gzip/zlib/snappy compression anywhere in the pipeline

### Handler Routing (Annotation-Based)

Packet handlers are plain Java methods annotated with `@Proto`:

```java
@Proto(code = 1, type = 2, send = SendPackId.LOGIN_RES)
public byte[] handleLogin(GameClient client, WzCharacter chr, LoginPb.LoginReq req) { ... }
```

On startup `GameLogicServer.scanProtos()` scans all classes in the classpath, builds two HashMaps:
- `protosClz<Integer code, Class>` — maps opcode to handler class
- `protos<String "code,type", Method>` — maps `"code,type"` string to handler method

Dispatch flow in `GameLogicHandler.onMessage()`:
1. Read 4-byte opcode + 4-byte type from frame
2. XOR-decrypt remaining bytes
3. Look up method via `"code,type"` key
4. Deserialize protobuf: `message.getParserForType().parseFrom(content)`
5. Invoke handler method via reflection
6. Wrap response with `ResPacketCreator.wrapBuffers()` → write to channel

Key annotation file: `wz_game_logic/src/main/java/game/anotation/Proto.java`

---

## 3. River HTTP Server (Port 46001)

Built on Java's built-in `com.sun.net.httpserver.HttpServer` with an 8-thread executor. Not Spring — plain HTTP handlers.

| Endpoint Pattern | Purpose | Auth |
|---|---|---|
| `/river/sauth/*` | Payment / shipping order validation | HmacSHA256 signed |
| `/river/cdkey/*` | CD key redemption validation | HmacSHA256 signed |
| `/river/gm/*` | GM commands | HmacSHA256 signed |
| `/river/feedback/*` | Player feedback submission | None |
| `/river/phonebinding/*` | Phone number binding | None |
| `/river/script/*` | Server-side script execution | Internal |
| `/river/logclient/*` | Client error log ingest | None |

Key file: `wz_game_logic/src/main/java/game/river/River.java`

---

## 4. Admin / User Portals (Tomcat WAR, Port 8080)

Two separate Spring MVC WAR modules deployed on Tomcat:

**enicenuc** (admin panel) — `enicenuc/src/main/java/controller/`

| URL | Purpose |
|---|---|
| `/login.man` | Admin login |
| `/allList.man` | Account list (paginated) |
| `/add.man` / `/update.man` / `/delete.man` | CRUD accounts |
| `/jumpAnnounce.man` / `/announceList.man` | Announcements |
| `/jumpCDKeycode.man` / `/createCDKey.man` | CD key management |
| `/jumpGuild.man` / `/guildList.man` | Guild management |
| `/orderList.man` | Payment order management |
| `/operationLog.man` | Audit logs |
| `/serverList.man` | Game server list configuration |
| `/roleController/*` | Role / permission management |

**enicenuser** (user portal) — `enicenuser/src/main/java/controller/` — mirrors similar endpoints for user-facing operations.

---

## 5. Inter-Server Communication

Direct TCP socket using the same Netty stack (no Redis, no RabbitMQ, no Kafka).

- **Client library:** `wz_common/src/main/java/common/net/NetClient.java`
  - Uses `Bootstrap` + `NioSocketChannel`
  - Auto-reconnect with configurable interval
- **Handler:** `wz_game_logic/src/main/java/game/net/handler/InterServerHandler.java`
- **OpCode:** 33 (cross-server message opcode)

**Cross-server operations supported:**
| Type | Operation |
|---|---|
| 1 | Get immigrant card count per server |
| 2 | Get transfer condition list for target server |
| 3 | Perform player server-transfer |

Also used for: Arena cross-server, Stronghold battles, Battle Expedition coordination.

---

## 6. Database Connections

| Database | Driver | Version | Default Host | Config File |
|---|---|---|---|---|
| MongoDB | mongo-java-driver | 3.12.14 | `127.0.0.1:27017` | `data/conf/gl_db.properties` |
| MySQL (MariaDB) | Hibernate 5 / Spring Data JPA | — | `127.0.0.1:3306` | `data/conf/gls.properties` |

MongoDB ORM: Spring Data MongoDB 1.10.23  
Caching layer: `MongoCache.java` in game source

---

## 7. Async / Actor Model

Game logic runs through a custom actor model to avoid blocking Netty I/O threads:

- `wz_common/src/main/java/common/actor/Actor.java` — `BlockingQueue`-based task actor
- `wz_common/src/main/java/common/actor/ActorDispatcher.java` — schedules actors
- Each player/entity gets its own actor; tasks are enqueued and processed serially per actor

Scheduled jobs (world events, timers, resets) use **Quartz 2.2.3**.

---

## 8. Complete Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Network transport | Netty | 4.0.29.Final |
| Serialization | Protocol Buffers | 2.5.0 |
| Game server language | Java | 17 (compiled as 1.8 target) |
| DI framework | Spring Framework | 4.2.x |
| ORM (relational) | Hibernate | 5.x |
| ORM (document) | Spring Data MongoDB | 1.10.23 |
| MongoDB driver | mongo-java-driver | 3.12.14 |
| Scheduler | Quartz | 2.2.3 |
| HTTP server (internal) | com.sun.net.httpserver | JDK built-in |
| Web portals | Spring MVC WAR on Tomcat | — |
| Logging | Syslog over UDP | Port 514 |

---

## 9. Setting Up a New Server Instance

### Prerequisites

1. Java 17 (Eclipse Temurin recommended)
2. MongoDB 8.x on port 27017 — create user `test`/`test` in db `test`
3. MySQL/MariaDB on port 3306
4. XAMPP Apache on port 80 — place `serverlist.txt` in htdocs root
5. Tomcat (for enicenuc / enicenuser WAR deployment)

### Configuration Checklist

Edit `n2-server-master/data/conf/gls.properties`:
```properties
server_id=<unique integer per instance>
server_logic_port=16001        # client TCP port
river.port=46001               # HTTP API port
game_code=hwtw
syslog.port=514
syslog.protocol=udp
```

Edit `n2-server-master/data/conf/gl_db.properties`:
```properties
# MongoDB
mongo.host=127.0.0.1:27017
mongo.db=test
mongo.user=test
mongo.password=test
```

### Build

```bash
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.17.10-hotspot" \
  mvn -f n2-server-master/wz_game_logic/pom.xml package -DskipTests
```

### Run

```bash
cd n2-server-master/wz_game_logic
java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.io=ALL-UNNAMED \
  --add-opens java.base/java.math=ALL-UNNAMED \
  --add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED \
  --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
  -jar target/wz_game_logic-0.0.1-SNAPSHOT.jar
```

Or use `n2-server-master/start-dev.bat` on Windows.

### Startup Verification

Watch the log for these lines:
```
Opened connection [connectionId{localValue:1}] to 127.0.0.1:27017   # MongoDB OK
加载配置成功!                                                          # Config loaded
gamelogic???!                                                          # TCP port 16001 open
```

### Firewall / Port Exposure

| Port | Direction | Open to |
|---|---|---|
| 16001 (TCP) | Inbound | Game clients (Unity) |
| 46001 (HTTP) | Inbound | Payment gateway, GM tools (restrict to internal/trusted IPs) |
| 8080 (HTTP) | Inbound | Admin only — do NOT expose to public |
| 514 (UDP) | Outbound | Syslog collector |
| 27017 (TCP) | Internal | MongoDB — bind to 127.0.0.1 only |
| 3306 (TCP) | Internal | MySQL — bind to 127.0.0.1 only |

### Adding a New Packet Handler

1. Create a handler class under `wz_game_logic/src/main/java/game/net/handler/`
2. Register it in the handler scan package (or ensure it's in a scanned package)
3. Annotate each method:
   ```java
   @Proto(code = <opcode>, type = <subtype>, send = SendPackId.<RESPONSE_ENUM>)
   public byte[] myHandler(GameClient client, WzCharacter chr, MyRequestPb.MyReq req) {
       // ...
       return ResPacketCreator.create(MyResponsePb.MyRes.newBuilder()...build(), SendPackId.MY_RES);
   }
   ```
4. Add the corresponding entry to `SendPackId` enum
5. Define the proto message in `wz_common/src/main/java/common/protobuf/` and recompile

---

## 10. Key Files Quick Reference

| File | Role |
|---|---|
| `wz_game_logic/src/main/java/game/server/GameLogicServer.java` | Netty bootstrap, XOR key, proto scanning |
| `wz_game_logic/src/main/java/game/net/codec/Protocol.java` | Frame decoder |
| `wz_game_logic/src/main/java/game/net/handler/GameLogicHandler.java` | Netty pipeline, message dispatch |
| `wz_game_logic/src/main/java/game/packet/ResPacketCreator.java` | Response packet builder |
| `wz_game_logic/src/main/java/game/anotation/Proto.java` | Handler routing annotation |
| `wz_game_logic/src/main/java/game/river/River.java` | HTTP API server (port 46001) |
| `wz_common/src/main/java/common/net/NetClient.java` | Inter-server TCP client |
| `wz_common/src/main/java/common/actor/Actor.java` | Async actor model |
| `wz_common/src/main/java/common/protobuf/` | All 37 protobuf message classes |
| `data/conf/gls.properties` | Server config (ports, server_id, game_code) |
| `data/conf/gl_db.properties` | MongoDB connection config |
