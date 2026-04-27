```markdown
# Core API Guardrails

A high-performance Spring Boot microservice acting as a central API gateway and guardrail system. It handles concurrent bot-reply limiting, Redis-backed virality scoring, and a smart batching notification engine.

---

## Tech Stack

| Layer         | Technology                     |
|---------------|-------------------------------|
| Runtime       | Java 17+                       |
| Framework     | Spring Boot 3.x                |
| Database      | PostgreSQL (via JPA/Hibernate) |
| Cache / State | Redis (Spring Data Redis)      |
| Build         | Maven                          |
| Containers    | Docker / Docker Compose        |

---

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Maven 3.8+

### 1. Start infrastructure
```bash
docker-compose up -d
```
This spins up PostgreSQL on port `5432` and Redis on port `6379`.

### 2. Run the application
```bash
./mvnw spring-boot:run
```
The API will be available at `http://localhost:8080`.

### 3. Seed test data

Before running any comment or like flows, insert at minimum:
- One **User** (id = 1) — insert directly into the `users` table
- One **Bot** (id = 1) — insert directly into the `bots` table
- One **Post** authored by the User — use `POST /api/posts`

```sql
INSERT INTO users (username, is_premium) VALUES ('testuser', false);
INSERT INTO bots (name, persona_description) VALUES ('TestBot', 'A test bot persona');
```

---

## API Reference

### POST `/api/posts`
Create a new post.

```json
{
  "authorId": 1,
  "authorType": "USER",
  "content": "Hello world!"
}
```
`authorType` accepts `USER` or `BOT`.

**Response:** `201 Created` with post body. `400 Bad Request` if `content` is missing.

---

### POST `/api/posts/{postId}/comments`
Add a comment to a post.

```json
{
  "authorId": 1,
  "authorType": "BOT",
  "content": "Bot reply here.",
  "depthLevel": 1
}
```

**Guardrail responses:**

| Condition                        | HTTP Status            |
|----------------------------------|------------------------|
| Comment accepted                 | `201 Created`          |
| Bot count > 100                  | `429 Too Many Requests`|
| `depthLevel` > 20                | `429 Too Many Requests`|
| Bot → same user within 10 min    | `429 Too Many Requests`|

---

### POST `/api/posts/{postId}/like`
Like a post (adds +20 to virality score in Redis).

```
200 OK  — "post liked successfully"
404 Not Found — if post does not exist
```

---

## Architecture Overview

```
HTTP Request
     │
     ▼
PostController
     │
     ├── PostService          → creates posts, triggers ViralityService (+20 on like)
     │
     └── CommentService
              │
              ├── GuardrailService   → Redis atomic checks (must pass before DB write)
              │
              ├── ViralityService    → increments virality score in Redis
              │
              └── NotificationService → Redis throttle + pending list
```

**Data flow principle:** Redis acts as the gatekeeper. PostgreSQL is only written to if all Redis guardrails pass. No guardrail state is held in Java memory.

---

## Phase 2: Thread Safety — How Atomic Locks Work

This is the core concurrency challenge of the assignment. All three caps are enforced using atomic Redis operations to guarantee correctness under high concurrency.

### Horizontal Cap (100 bot replies per post)

A naive approach using `GET` → check → `INCR` has a race condition: two threads can both read `99`, both pass the check, and both increment to `100` and `101`.

**Solution — Atomic Lua Script via `RedisScript`:**

```lua
local count = redis.call('INCR', KEYS[1])
if count > tonumber(ARGV[1]) then
  redis.call('DECR', KEYS[1])
  return -1
end
return count
```

Redis executes Lua scripts atomically — the entire increment-check-decrement block runs as a single indivisible operation with no other commands interleaving. If the new count exceeds 100, the counter is rolled back and `-1` is returned; the Spring service throws `429`. This guarantees **exactly 100** comments are saved even under 200 simultaneous requests.

**Redis key:** `post:{id}:bot_count`

### Vertical Cap (max depth 20)

This is a stateless check on the `depthLevel` field from the request payload, enforced before any Redis or DB operation. No atomicity concern because the value comes entirely from the request and is not shared state.

**Enforcement:** `if (depthLevel > verticalCap) throw 429`

### Cooldown Cap (bot → human, 10-minute TTL)

Uses `SET NX EX` semantics via Spring Data Redis `setIfAbsent`:

```java
Boolean set = redisTemplate.opsForValue()
    .setIfAbsent(key, "1", 600, TimeUnit.SECONDS);
```

`setIfAbsent` maps to the Redis `SET key value NX EX ttl` command — an atomic operation. If the key does not exist it is created and `true` is returned (interaction allowed). If it already exists, `false` is returned (cooldown active → `429`). There is no window between checking and setting.

**Redis key:** `cooldown:bot_{botId}:human_{humanId}`

---

## Phase 3: Notification Engine

### Throttler Logic (`NotificationService`)

When a bot comments on a user's post:
1. Check for key `notif:cooldown:user_{id}` in Redis.
2. **Key absent (no recent notification):** Log `"Push Notification Sent to User {id}"` to console and set the key with a **15-minute TTL**.
3. **Key present (within 15 min):** Push the notification string to the Redis List `user:{id}:pending_notifs` — no immediate push.

### CRON Sweeper (`NotificationSweeper`)

A `@Scheduled` task runs every **5 minutes** (simulating the production 15-minute sweep):

1. Scan Redis for all keys matching `user:*:pending_notifs`.
2. For each list found, pop all messages and count them.
3. Log: `"Summarized Push Notification: Bot X and [N] others interacted with your posts."`
4. Delete the list from Redis.

> **Note:** The sweeper uses `KEYS` for simplicity. In a production environment with a large keyspace, this would be replaced with an incremental `SCAN` to avoid blocking the Redis event loop.

---

## Concurrency Test

A shell script is included to fire 200 concurrent bot comment requests simultaneously:

```bash
chmod +x test200.sh
./test200.sh
```

Expected output: exactly 100 × `201` and 100 × `429`. Any `201` beyond 100 means the atomic lock failed.

---

## Statelessness Guarantee

The application holds **zero state in Java memory**. There are no `static` variables, no `HashMap` counters, and no singleton caches for guardrail data. Every counter, cooldown key, and pending notification lives exclusively in Redis, making the service horizontally scalable — multiple instances can run behind a load balancer and share the same Redis state correctly.

---

## Project Structure

```
src/main/java/com/assignment/Core_Api_Guardrails/
├── config/
│   ├── AppConfig.java          # Cap values (horizontal=100, vertical=20, cooldown TTLs)
│   └── RedisConfig.java        # RedisTemplate bean configuration
├── controller/
│   └── PostController.java     # REST endpoints
├── dto/
│   ├── request/                # PostRequest, CommentRequest, LikeRequest
│   └── response/               # PostResponse, CommentResponse
├── entity/
│   ├── AuthorType.java         # Enum: USER | BOT
│   ├── Bot.java
│   ├── Comment.java
│   ├── Post.java
│   └── User.java
├── repository/                 # Spring Data JPA repositories
├── scheduler/
│   └── NotificationSweeper.java   # @Scheduled CRON sweeper
└── service/
    ├── CommentService.java        # Comment orchestration
    ├── GuardrailService.java      # Redis atomic enforcements
    ├── NotificationService.java   # Redis throttle + pending list
    ├── PostService.java           # Post creation + like
    └── ViralityService.java       # Redis virality score updates
```

---

## Configuration (`application.yaml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/grid07
    username: grid07
    password: grid07

  data:
    redis:
      host: localhost
      port: 6379

app:
  redis:
    bot-cooldown-ttl: 600       # 10 minutes
    notif-cooldown-ttl: 900     # 15 minutes
    horizontal-cap: 100         # max bot replies per post
    vertical-cap: 20            # max comment depth

  scheduler:
    notif-sweep-cron: "0 */5 * * * *"   # every 5 minutes
```

---

## Postman Collection

Import `postman-collections.json` into Postman.

The collection is organized into four folders matching the assignment phases:

| Folder                            | Coverage                                                                                   |
|-----------------------------------|--------------------------------------------------------------------------------------------|
| Phase 1 — Core API                | Create post (user & bot), add comments, like post, 404 cases, validation errors            |
| Phase 2 — Atomic Guardrails       | Horizontal cap (100 limit), vertical cap (depth 21+), cooldown (second bot reply blocked)  |
| Phase 3 — Notification Engine     | First interaction triggers push log; second queues to Redis list                           |
| Phase 4 — Spam & Race Condition   | 200-iteration runner with per-iteration 201/429 assertions; human comments unaffected after cap |

**To run the spam test:** Open the *Phase 4* folder → *Spam Batch* request → **Run** with **200 iterations, 0 ms delay**.
```
