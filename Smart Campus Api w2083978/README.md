# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures – Coursework (2025/26)
**Stack:** Java 11 · JAX-RS (Jakarta) · Jersey · Embedded Grizzly · Jackson (JSON)
**Storage:** In-memory only (`ConcurrentHashMap`) – **no database** is used, as required by the spec.

---

## 1. API Design Overview

The service exposes a versioned base path `/api/v1` and follows REST conventions:

| Resource | Path | Purpose |
|---|---|---|
| Discovery | `GET /api/v1` | HATEOAS-style entry point with metadata + links |
| Rooms | `GET/POST /api/v1/rooms`, `GET/DELETE /api/v1/rooms/{roomId}` | Room CRUD + safety-checked deletion |
| Sensors | `GET/POST /api/v1/sensors` (`?type=`), `GET /api/v1/sensors/{sensorId}` | Sensor management + filtering |
| Readings (sub-resource) | `GET/POST /api/v1/sensors/{sensorId}/readings` | Historical log via Sub-Resource Locator |

**Cross-cutting concerns** are implemented as JAX-RS providers:

- `RequestResponseLoggingFilter` – logs every HTTP method/URI in and the status code out.
- `RoomNotEmptyExceptionMapper` → **409 Conflict**
- `LinkedResourceNotFoundExceptionMapper` → **422 Unprocessable Entity**
- `SensorUnavailableExceptionMapper` → **403 Forbidden**
- `NotFoundExceptionMapper` → **404 Not Found** (clean JSON, never a server page)
- `GenericExceptionMapper` → **500 Internal Server Error** (catch-all, never leaks stack traces)

---

## 2. Build & Run

### Prerequisites
- JDK 11+
- Maven 3.6+

### Build
```bash
cd smart-campus-api
mvn clean package
```

This produces a runnable fat-jar at `target/smart-campus-api.jar`.

### Run
```bash
java -jar target/smart-campus-api.jar
```

The server starts at **`http://localhost:8080/api/v1`**. Press `Ctrl+C` to stop.

Alternatively (without packaging):
```bash
mvn exec:java -Dexec.mainClass=uk.ac.westminster.smartcampus.Main
```

---

## 3. Sample `curl` Commands

```bash
# 1. Discovery
curl -s http://localhost:8080/api/v1 | jq

# 2. List all rooms
curl -s http://localhost:8080/api/v1/rooms | jq

# 3. Create a room
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"ENG-200","name":"Engineering Lab","capacity":50}' | jq

# 4. Register a new sensor (linked to existing room)
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-200","type":"Temperature","status":"ACTIVE","currentValue":22.0,"roomId":"ENG-200"}' | jq

# 5. Filter sensors by type
curl -s "http://localhost:8080/api/v1/sensors?type=Temperature" | jq

# 6. Append a reading to a sensor (sub-resource)
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-200/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}' | jq

# 7. Fetch reading history
curl -s http://localhost:8080/api/v1/sensors/TEMP-200/readings | jq

# 8. Attempt to delete a non-empty room => 409 Conflict
curl -i -X DELETE http://localhost:8080/api/v1/rooms/LIB-301

# 9. Attempt to register a sensor with an unknown room => 422
curl -i -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type":"CO2","status":"ACTIVE","roomId":"DOES-NOT-EXIST"}'

# 10. Post a reading to a sensor in MAINTENANCE => 403
curl -i -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" -d '{"value":1}'
```

---

## 4. Conceptual Report (answers to spec questions)

### Part 1.1 – JAX-RS Resource lifecycle
By default, JAX-RS creates **a new instance of a resource class for every incoming request** (per-request lifecycle). It is *not* a singleton unless explicitly annotated (`@Singleton`) or registered as such. This means any state declared as instance fields is local to a single request and is discarded once the response is written.

Because shared state (rooms, sensors, readings) must survive across requests, we cannot keep it in resource fields. We hold it in a singleton `DataStore` whose maps are `ConcurrentHashMap`s. This guarantees thread-safe insertion, lookup and removal under concurrent load. For composite operations (e.g. "create sensor + append its id to the parent room's `sensorIds` list") the underlying `List` is wrapped/used carefully so that we don't hit `ConcurrentModificationException`. Without these precautions, two simultaneous POSTs could corrupt the structure or "lose" a write because of last-write-wins on a non-atomic update.

### Part 1.2 – Why HATEOAS / hypermedia?
Embedding navigation links in responses (HATEOAS) means the client doesn't need to hard-code the URL structure of the API. The discovery endpoint advertises `/api/v1/rooms`, `/api/v1/sensors`, etc., so a client can be written against the **link relations** rather than the literal paths. This decouples client and server: the server can later move/version a resource (e.g. add `/v2/`) and only the discovery document changes — long-lived clients keep working. It also reduces dependence on out-of-band documentation that drifts from reality, makes the API self-describing, and enables generic tools (HAL browsers, API explorers) to walk the surface automatically.

### Part 2.1 – IDs vs full objects in list endpoints
Returning only IDs is bandwidth-cheap and lets the client lazily fetch what it needs, but it forces an **N+1** request pattern (one listing call + one detail call per row), increasing latency and load. Returning full objects is "chunkier" payload but lets the UI render the whole page in one round-trip. The right answer depends on the client: dashboards favour full objects (sometimes with filtered fields), while search/auto-complete might prefer ID-only listings. Our `GET /rooms` returns full objects because the room payload is small and lists are typically rendered as tables in a facilities-management UI.

### Part 2.2 – Is `DELETE` idempotent here?
Yes. The HTTP definition of idempotency is that *the resulting server state* is the same after one or many identical requests. The first `DELETE /rooms/{id}` either:
1. removes the room and returns `204 No Content`, or
2. is blocked by the safety rule (`409 Conflict`).

A repeated `DELETE` after a successful one returns `404 Not Found` (the room no longer exists), but the **server state is unchanged** — there is still no room with that id. That satisfies the idempotency contract; clients that retry due to a flaky network won't accidentally damage data. (Note: the *response* status differs between calls, but idempotency is about state, not response equality.)

### Part 3.1 – `@Consumes(APPLICATION_JSON)` mismatch
The annotation tells JAX-RS this method only accepts JSON. If a client posts `text/plain` or `application/xml`, the runtime never invokes the method — it short-circuits with **HTTP 415 Unsupported Media Type**. This is content-negotiation in action: JAX-RS inspects the request's `Content-Type` header against the declared `@Consumes`, picks a matching `MessageBodyReader` (Jackson for JSON in our case), and rejects anything it can't deserialize. This keeps deserialization safe and predictable instead of letting a wrong-format payload reach our code.

### Part 3.2 – `@QueryParam` vs path-based filtering
Query parameters express **filters/optional refinements** on a collection, while path segments identify **resources**. `/sensors` is the collection of all sensors, regardless of type; `?type=CO2` narrows the view. Putting `type` in the path (`/sensors/type/CO2`) wrongly implies a different resource, breaks caching by URL identity, doesn't compose (`?type=CO2&status=ACTIVE&roomId=LIB-301` is trivial; the path equivalent becomes a combinatorial mess), and makes pagination/sorting parameters look inconsistent. Query parameters are also OPTIONAL by convention — the same endpoint serves "all" and "filtered" without route duplication.

### Part 4.1 – Sub-Resource Locator benefits
A locator method returns *another resource class* rather than handling the nested path itself. The benefits:
- **Single-Responsibility:** `SensorResource` knows about sensors; `SensorReadingResource` knows about readings. Neither bloats.
- **Context propagation:** The locator captures the parent id (`sensorId`) and passes it via the constructor, so every method on the sub-resource implicitly operates within that parent context.
- **Pre-validation:** The locator can verify the parent exists once, returning 404 early, before the sub-resource sees the request.
- **Reusability/testability:** Each class is small and unit-testable in isolation.
- A monolithic controller that hand-rolls every nested path becomes a "god class" that mixes authorisation, validation and persistence for many resources, which is hard to maintain.

### Part 5.2 – Why 422 instead of 404 for missing references?
`404 Not Found` is about the **request URI** itself — "the resource you asked for at this URL doesn't exist". When the client posts a perfectly valid JSON body to a perfectly valid URL (`/api/v1/sensors`) but a referenced field inside that body (`roomId`) doesn't exist, the URL *was* found and the JSON *was* well-formed; the problem is **semantic**: the request can't be processed because of a referential-integrity failure. RFC 4918 defines **422 Unprocessable Entity** for exactly this case. Using 404 here would confuse clients about which thing was missing (the sensors endpoint? the sensor itself? the room?). 422 keeps the diagnostic precise.

### Part 5.4 – Risks of leaking stack traces
A raw Java stack trace reveals: framework versions (Jersey x.y, Jackson z.w) — vulnerable to known CVEs; package and class names — exposing internal architecture; file paths or line numbers — useful for fingerprinting; sometimes parts of the SQL/JPA query, secret-looking strings, or DB schema names; and the chain of called libraries — useful for crafting deserialization or injection payloads. An attacker reconnoitring a target uses these breadcrumbs to choose an exploit. Our `GenericExceptionMapper` logs the full trace **server-side** with a UUID correlation id and returns only `{status, code, message, correlationId}` to the client.

### Part 5.5 – Why filters for cross-cutting concerns?
A filter is registered once and runs around every matching request, so logging, authentication, CORS, audit trails, and rate-limit headers stay in one place instead of being copy-pasted into every method. This eliminates an entire class of bugs (someone adds a new endpoint and forgets to log it), keeps resource methods focused on business logic, and makes it trivial to disable or change the behaviour globally (e.g. swap `java.util.logging` for SLF4J without touching any resource). It's a textbook application of the Aspect-Oriented Programming idea of "advice" applied to HTTP.

---

## 5. Project Layout
```
smart-campus-api/
├── pom.xml
└── src/main/java/uk/ac/westminster/smartcampus/
    ├── Main.java                       # embedded Grizzly bootstrap
    ├── SmartCampusApplication.java     # @ApplicationPath("/api/v1")
    ├── model/         Room.java  Sensor.java  SensorReading.java
    ├── store/         DataStore.java   # in-memory ConcurrentHashMaps
    ├── resources/     DiscoveryResource.java  SensorRoom.java
    │                  SensorResource.java      SensorReadingResource.java
    ├── exceptions/    RoomNotEmptyException(+Mapper)
    │                  LinkedResourceNotFoundException(+Mapper)
    │                  SensorUnavailableException(+Mapper)
    │                  NotFoundExceptionMapper.java
    │                  GenericExceptionMapper.java
    └── filters/       RequestResponseLoggingFilter.java
```

---

## 6. Notes for the Submission

- Push the contents of the `smart-campus-api/` folder to a **public GitHub repo** (the spec forbids zip submissions).
- Record the Postman / curl walk-through video (≤10 min) and upload to Blackboard.
- The conceptual answers above also live in this `README.md` per the spec, which states the report "must be organised and written in the README.md file on GitHub".
