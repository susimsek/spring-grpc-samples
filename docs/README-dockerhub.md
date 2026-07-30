# Spring gRPC Samples (Spring Boot 4 + Native)

Todo CRUD sample application built with Spring Boot 4, Spring gRPC, Spring Data JPA, Liquibase, H2, Spring Security JWT, and Protovalidate.
This image runs the app as a GraalVM native executable for fast startup and low memory usage.

This image exposes server-side gRPC APIs for authentication and Todo management. The application listens on the standard gRPC port `9090`. When running containers on the same Docker network, connect to `<container-name>:9090` with plaintext gRPC unless you add TLS at the platform edge.

## Features

- Todo CRUD over gRPC: create, list, get, update, patch, delete
- Pageable `ListTodos`
- JWT login with Spring Security OAuth2 Resource Server
- `ROLE_ADMIN` authorization for Todo APIs
- H2 in-memory database in PostgreSQL compatibility mode
- XML-based Liquibase schema migrations
- CSV seed data for users, authorities, user authorities, and sample todos
- JPA auditing with `Instant` `created_at` and `updated_at`
- MapStruct mapping between JPA entities and protobuf responses
- Protovalidate request validation
- Central gRPC exception handling with localized errors
- Internationalization: English and Turkish gRPC error messages
- gRPC health and server reflection services
- GraalVM native executable

## How to use this image

### 1. Start a PostgreSQL server

```bash
docker run --name postgresql --rm -d \
  -e POSTGRES_USER=appuser \
  -e POSTGRES_PASSWORD=appuser \
  -e POSTGRES_DB=grpcsamples \
  -p 127.0.0.1:5432:5432 \
  postgres:18-alpine
```

### 2. Generate a JWT secret

Generate at least a 256-bit secret:

```bash
openssl rand -hex 32
```

### 3. Start the application (prod mode with PostgreSQL)

```bash
docker run --rm -p 9090:9090 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/grpcsamples \
  -e SPRING_DATASOURCE_USERNAME=appuser \
  -e SPRING_DATASOURCE_PASSWORD=appuser \
  -e SECURITY_JWT_SECRET="<paste-openssl-output>" \
  suayb/spring-grpc-samples:latest-native
```

Or with H2 in-memory (no PostgreSQL required):

```bash
docker run --rm -p 9090:9090 \
  -e SECURITY_JWT_SECRET="$(openssl rand -hex 32)" \
  suayb/spring-grpc-samples:latest-native
```

The gRPC server is available at:

```text
localhost:9090
```

### 2. Check health

```bash
grpcurl -plaintext \
  -d '{"service": ""}' \
  localhost:9090 \
  grpc.health.v1.Health/Check
```

Expected response:

```json
{
  "status": "SERVING"
}
```

### 3. List available services

```bash
grpcurl -plaintext localhost:9090 list
```

Expected services include:

```text
AuthService
TodoService
grpc.health.v1.Health
grpc.reflection.v1.ServerReflection
```

### 4. Login and call Todo APIs

Seeded users:

| Username | Password | Authorities |
| --- | --- | --- |
| `admin` | `admin` | `ROLE_ADMIN`, `ROLE_USER` |
| `user` | `user` | `ROLE_USER` |

Get an admin token:

```bash
TOKEN=$(grpcurl -plaintext \
  -d '{"username":"admin","password":"admin"}' \
  localhost:9090 \
  AuthService/Login | jq -r '.access_token')
```

List todos:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"page":0,"size":5}' \
  localhost:9090 \
  TodoService/ListTodos
```

Create a todo:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"title":"Prepare gRPC release"}' \
  localhost:9090 \
  TodoService/CreateTodo
```

## Localized errors

Pass `accept-language` as gRPC metadata.

Turkish validation example:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -rpc-header "accept-language: tr" \
  -d '{"title":"ab"}' \
  localhost:9090 \
  TodoService/CreateTodo
```

English validation example:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -rpc-header "accept-language: en" \
  -d '{"title":"ab"}' \
  localhost:9090 \
  TodoService/CreateTodo
```

## Environment variables

| Name | Default | Description |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `default` | Active Spring profile |
| `SPRING_GRPC_SERVER_PORT` | `9090` | gRPC server port |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:spring-grpc-samples;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `sa` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | (empty) | Database password |
| `SPRING_LIQUIBASE_ENABLED` | `true` | Enable or disable Liquibase migrations |
| `SPRING_LIQUIBASE_CONTEXTS` | `faker` | Liquibase contexts used for sample todo data |
| `APP_SECURITY_JWT_ISSUER` | `https://auth.spring-grpc-samples.local` | JWT issuer |
| `SECURITY_JWT_SECRET` | local sample secret | JWT HMAC secret; replace for every real deployment |
| `APP_SECURITY_JWT_EXPIRES_IN` | `1h` | Access token lifetime |

Generate a 256-bit JWT secret:

```bash
openssl rand -hex 32
```

## gRPC endpoints

Auth:

- `AuthService/Login`

Todos:

- `TodoService/CreateTodo`
- `TodoService/GetTodo`
- `TodoService/ListTodos`
- `TodoService/UpdateTodo`
- `TodoService/PatchTodo`
- `TodoService/DeleteTodo`

Infrastructure:

- `grpc.health.v1.Health/Check`
- `grpc.reflection.v1.ServerReflection`

## Health checks

The image exposes the standard gRPC Health Checking service.

Kubernetes example:

```yaml
livenessProbe:
  grpc:
    port: 9090
  initialDelaySeconds: 10
  periodSeconds: 10
readinessProbe:
  grpc:
    port: 9090
  initialDelaySeconds: 10
  periodSeconds: 10
```

## Notes

- This is a gRPC server sample; it does not serve a REST API or web UI.
- The default database is in-memory H2, so data is reset when the container stops.
- Liquibase migrations and CSV seed data run on startup by default.
- `AuthService/Login`, gRPC health, and gRPC reflection are public.
- Todo APIs require a valid JWT and are restricted to `ROLE_ADMIN`.
- Use `accept-language: tr` or `accept-language: en` metadata to localize error responses.
