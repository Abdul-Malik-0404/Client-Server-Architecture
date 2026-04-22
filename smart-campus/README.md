# Smart Campus Sensor & Room Management API

A RESTful web service for managing rooms and sensors within a Smart Campus initiative, built with JAX-RS (Jakarta RESTful Web Services), Jersey, and an aggregated Grizzly server. 

## Build and Run Instructions

### Prerequisites
- JDK 11 or higher
- Maven 3.6+

### How to Start the Server
1. Clone or download the repository to your local machine.
2. Open a terminal and navigate to the project directory:
   ```bash
   cd smart-campus
   ```
3. Compile and build the project using Maven:
   ```bash
   mvn clean install
   ```
4. Run the embedded Grizzly HTTP server:
   ```bash
   mvn exec:java
   ```
5. The API will be accessible at: `http://localhost:8080/api/v1`

---

## Sample Interactions (cURL Commands)

**1. Root Discovery**
```bash
curl -X GET http://localhost:8080/api/v1
```

**2. List All Rooms**
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

**3. Create a New Room**
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
     -H "Content-Type: application/json" \
     -d '{"name": "Lecture Hall A", "capacity": 200}'
```

**4. Create a New Sensor (Dependency Validation)**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -d '{"type": "CO2", "status": "ACTIVE", "currentValue": 400.0, "roomId": "LIB-301"}'
```

**5. Filter Sensors by Type**
```bash
curl -X GET http://localhost:8080/api/v1/sensors?type=CO2
```

**6. Submit a New Reading to a Sensor**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
     -H "Content-Type: application/json" \
     -d '{"value": 22.5}'
```

---

## Coursework Questions & Answers

### Part 1.1: JAX-RS Resource Lifecycle
**Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.
**Answer:** By default, JAX-RS treats Resource classes as *per-request* instances. A new instance of the class is created for every single incoming HTTP request and destroyed after the response is sent. Because of this ephemeral lifecycle, any instance variables (like standard `HashMap` or `ArrayList`) initialized within the Resource will be lost between requests. To prevent data loss and support concurrent API calls, our in-memory data store must be extracted to a Singleton structure, utilizing thread-safe collections (`ConcurrentHashMap`) so that multiple resource instances can interact consistently with shared global memory without encountering race conditions.

### Part 1.2: HATEOAS & Discovery
**Question:** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?
**Answer:** HATEOAS (Hypermedia As The Engine Of Application State) elevates an API to REST level 3 by providing clients with dynamic URIs dictating what actions they can take next. It benefits developers because the API becomes self-discoverable; clients don't have to hardcode URLs manually based on out-of-date static documentation. Instead, they dynamically parse links received from the `Discovery` or resource payloads, allowing the backend to change its path structures without breaking client integrations.

### Part 2.1: Data Transference Implications
**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects?
**Answer:** Returning only IDs reduces the payload size significantly, conserving network bandwidth, but forces the client to make multiple follow-up HTTP requests to fetch details for each specific room (the N+1 problem), introducing latency. Returning full room objects increases the payload size and serialization overhead but provides the client with all necessary information in a single round-trip, optimizing processing time for UI rendering.

### Part 2.2: DELETE Idempotency
**Question:** Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.
**Answer:** Yes, it is idempotent. Idempotency means that making the same request multiple times achieves the exact same state on the server as making it once. If a client attempts to delete an unoccupied room `LIB-301`, the first request removes it (status 204). If the client resends the DELETE request, the room is already gone, and the server returns a 404 Not Found. Importantly, the server's state has not mutated beyond the initial deletion; whether called once or ten times, the end resulting state is identical (the room does not exist).

### Part 3.1: MediaType and @Consumes
**Question:** We explicitly use the `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?
**Answer:** If a client sends a payload with a `Content-Type` header that does not match `application/json` (like `text/plain`), the JAX-RS runtime intercepts the request before it even reaches the method. Since no method can "consume" that specific media type, JAX-RS automatically aborts the request and responds with an HTTP 415 Unsupported Media Type status code without executing any internal application logic.

### Part 3.2: Query Parameters vs Path Parameters
**Question:** Contrast this with an alternative design where the type is part of the URL path. Why is the query parameter approach generally considered superior for filtering and searching collections?
**Answer:** Path parameters strictly define structural resources (e.g., `sensors/{id}` points to a specific noun/entity). Filtering using the path (`/sensors/type/CO2`) treats the filter as a structural entity, which complicates URI routing, especially when combinations of filters are needed. Query parameters (`?type=CO2&status=ACTIVE`) are superior because they represent optional, composable modifiers applied to the base collection resource natively, allowing flexible querying without endless hardcoded path variations.

### Part 4.1: Sub-Resource Locator Pattern
**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity?
**Answer:** The Sub-Resource Locator pattern allows a parent root resource (`SensorResource`) to delegate processing of nested URIs (`/{sensorId}/readings`) to entirely separate classes (`SensorReadingResource`). This strictly adheres to the Single Responsibility Principle. Instead of creating a massive, monolithic Resource class handling all aspects of Sensors and their nested data arrays, logic is decoupled. It promotes highly cohesive, maintainable, and testable code structures by cleanly scoping functionality.

### Part 5.2: HTTP 422 vs 404 Requirements
**Question:** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?
**Answer:** HTTP 404 Not Found implies the requested URI path itself doesn't exist. When creating a Sensor via POST to `/sensors`, the URI *is* valid and exists. The problem lies inside the payload content (the `roomId` dependency is invalid). HTTP 422 Unprocessable Entity accurately conveys that the server understands the content type and request syntax, but cannot process the contained instructions because a business rule or semantic dependency (the missing room) prevents it.

### Part 5.4: Cybersecurity Risk of Stack Traces
**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers.
**Answer:** Exposing Java stack traces acts as an Information Disclosure vulnerability. Error traces reveal backend technologies, specific library versions, internal class names, underlying database implementations, and code file structures. Attackers use this blueprint to identify known CVEs associated with the revealed frameworks or construct highly targeted exploit payloads (like specific injection attacks), bypassing normal dark-box testing phases.

### Part 5.5: Benefits of JAX-RS Filters
**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?
**Answer:** JAX-RS filters intercept incoming requests and outgoing responses globally at the container level. Extracting cross-cutting concerns like logging to a centralized filter maintains the DRY (Don't Repeat Yourself) principle. It declutters core business logic, ensures that no methods are accidentally missed during implementation, and allows observability frameworks to be toggled, configured, or updated in a single place without modifying hundreds of domain-level code blocks.
