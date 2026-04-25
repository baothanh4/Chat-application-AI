# ChatApplication Realtime Chat Backend

A Spring Boot-based realtime chat backend with:

- WebSocket/STOMP realtime messaging
- Chat history persisted with JPA
- Presence tracking with Redis fallback
- Group chat and private chat
- Device token registration for push notifications
- Async message processing pipeline for high-volume traffic
- Built-in Local AI assistant (no OpenRouter dependency)
- Offline model integration via Ollama (local inference)

## Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring WebSocket + STOMP
- Spring Data JPA
- Spring Data Redis
- H2 for local development
- PostgreSQL for production

## Main WebSocket Endpoint

- `GET /ws` for STOMP handshake

Client subscriptions:

- `/topic/conversations/{conversationId}` for conversation updates

Client sends messages to:

- `/app/chat.send`

## REST API

### Users
- `POST /api/users`
- `GET /api/users/{userId}`
- `GET /api/users/search?query=...&viewerId=...&limit=10`

### Friend Requests / Friends
- `POST /api/friend-requests`
- `GET /api/friend-requests/incoming/{userId}`
- `GET /api/friend-requests/outgoing/{userId}`
- `GET /api/friend-requests/friends/{userId}`
- `POST /api/friend-requests/{requestId}/accept`
- `POST /api/friend-requests/{requestId}/reject`

### Conversations
- `POST /api/conversations/private`
- `POST /api/conversations/group`
- `GET /api/conversations/users/{userId}`
- `GET /api/conversations/{conversationId}/messages`
- `GET /api/conversations/{conversationId}/ai-config?userId=...`
- `PUT /api/conversations/{conversationId}/ai-config`

### Messages
- `POST /api/messages`

### Presence
- `POST /api/presence/heartbeat`
- `POST /api/presence/{userId}/offline`
- `GET /api/presence/{userId}`

### Redis Import / Cache
- `POST /api/redis/users/import` to copy all users from PostgreSQL into Redis hashes
- `POST /api/redis/users/{userId}` to import a single user profile into Redis
- `GET /api/redis/users/{userId}` to inspect the imported Redis hash

### Device Tokens
- `POST /api/device-tokens`

## Local Run

```powershell
./mvnw spring-boot:run
```

The app starts on port `8081` by default.

## Offline AI (Ollama)

1. Install Ollama and pull a model, for example `llama3.1:8b`.
2. Ensure Ollama runs at `http://localhost:11434`.
3. Keep these env vars in `.env`:
   - `CHATBOT_OLLAMA_ENABLED=true`
   - `CHATBOT_OLLAMA_BASE_URL=http://localhost:11434`
   - `CHATBOT_OLLAMA_MODEL=llama3.1:8b`

When Ollama is unavailable, chatbot replies automatically fall back to rule/context behavior.

## Sample Flow

1. Create users with `POST /api/users`
2. Create a private or group conversation
3. Send messages through REST or WebSocket
4. Subscribe to `/topic/conversations/{conversationId}` to receive realtime updates
5. Call presence heartbeat periodically to keep a user online
6. Register device tokens so offline recipients can receive push notifications
7. Import user profiles into Redis when you want RedisInsight or cache reads to see fresh data immediately
8. Use `/api/users/search` for autocomplete display-name search and `/api/friend-requests` to send/approve invitations

## How Redis is used in this project

- **Presence / online status**: Redis stores short-lived online markers with TTL.
- **User profile cache**: PostgreSQL remains the source of truth, and `/api/redis/users/import` can copy user records into Redis for fast inspection or cache warm-up.
- **Best practice**: keep chat history, conversations, and messages in PostgreSQL; keep Redis for ephemeral and cacheable data.

## Using RedisInsight to import or verify data

1. Connect RedisInsight to the Redis host/port/password from `application.properties` or environment variables.
2. Open **Workbench** and run Redis commands such as `HGETALL chat:user:profile:{userId}` or `SET chat:presence:{userId} online EX 120`.
3. Call `POST /api/redis/users/import` from the app to seed Redis hashes from PostgreSQL.
4. Refresh **Browser** in RedisInsight to see keys like:
   - `chat:user:profile:{userId}`
   - `chat:presence:{userId}`

## Scale Design for Millions of Users

For large-scale deployment, the backend should evolve into this architecture:

- **WebSocket gateway layer**: stateless app nodes behind a load balancer
- **Redis presence store**: presence, session mapping, and heartbeat TTL
- **Message broker**: Kafka or RabbitMQ for fan-out and buffering
- **Outbox pattern**: ensure messages are durable before async delivery
- **Database partitioning**: shard chat messages by conversation or tenant
- **Push notification worker**: separate process for FCM/APNs/Web Push
- **Read model / inbox model**: precomputed inboxes for fast timeline access
- **Observability**: metrics, tracing, and structured logs for all delivery paths

### Recommended production topology

- 1+ API/WebSocket gateway services
- 1 message broker cluster
- 1 Redis cluster
- 1 primary write database + read replicas
- 1 notification worker service
- 1 search/indexing service if message search is needed

## Notes

- The current implementation is a strong MVP and local demo backend.
- AI bot now runs in local mode by default (`CHATBOT_LOCAL_ENABLED=true`) and does not require any external OpenRouter key.
- For production push notifications, replace the logging-based notification service with FCM/APNs/Web Push integration.
- For million-user scale, use an external broker relay for WebSocket fan-out and keep application nodes stateless.
- Move Redis credentials out of `application.properties` into environment variables before deploying.
