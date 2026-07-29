# Spring gRPC Samples

[![Java](https://img.shields.io/badge/Java-25%2B-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9.16-C71A36?logo=apache-maven)](https://maven.apache.org/)
[![GraalVM](https://img.shields.io/badge/GraalVM-25%2B-F08820?logo=graalvm)](https://www.graalvm.org/)
[![gRPC](https://img.shields.io/badge/gRPC-High%20Performance-00ADD8?logo=grpc)](https://grpc.io/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker)](https://www.docker.com/)

This repository is a server-side Todo sample application built with Spring Boot 4.1 + Spring gRPC + Spring Data JPA + Liquibase on Java 25. It exposes gRPC APIs for authentication and Todo CRUD, stores data in an H2 in-memory database, validates protobuf requests with Protovalidate, returns localized gRPC errors, and can be compiled as a GraalVM native executable.

There is no gRPC client module in this project.

## Features

- Todo CRUD over gRPC
- Pageable `ListTodos`
- JWT login with Spring Security OAuth2 Resource Server
- `ROLE_ADMIN` authorization for `TodoApi/*`
- H2 in-memory database in PostgreSQL compatibility mode
- XML-based Liquibase schema migrations
- CSV seed data with fake Todo rows under Liquibase context `faker`
- JPA auditing with `Instant` `created_at` and `updated_at`
- MapStruct mapping between JPA entities and protobuf responses
- Protovalidate request validation
- Central gRPC exception handling with `@GrpcAdvice`
- i18n error messages in English and Turkish
- gRPC health and reflection services
- Spotless, Checkstyle, Sonar, and JaCoCo quality gates
- 100% JaCoCo coverage for handwritten application code

## Requirements

- Java: `25+`
- Maven Wrapper: `./mvnw`
- Optional:
  - `grpcurl`
  - `jq`
  - GraalVM Native Image for native builds

## Project Layout

- Application code: `src/main/java/io/github/susimsek/springgrpcsamples`
  - `config`: Spring configuration
    - `i18n`: locale resolution from gRPC metadata
    - `security`: JWT, password encoder, authentication manager, gRPC security
    - `validation`: Protovalidate gRPC interceptor configuration
  - `domain`: JPA entities and auditing base class
  - `exception`: application exceptions and central gRPC exception handler
  - `mapper`: MapStruct mappers and protobuf mapping helpers
  - `repository`: Spring Data JPA repositories
  - `security`: JWT service, user details service, constants, utilities
  - `service`: gRPC service implementations
- Protobuf contract: `src/main/proto/todo.proto`
- Configuration: `src/main/resources/application.yml`
- Liquibase: `src/main/resources/db/changelog`
- Seed data: `src/main/resources/db/data`
- i18n messages: `src/main/resources/i18n`
- Tests: `src/test/java`

## Configuration

Main configuration lives in `src/main/resources/application.yml`.

Important defaults:

- Application name: `spring-grpc-samples`
- gRPC port: `9090`
- Database: `jdbc:h2:mem:spring-grpc-samples`
- JPA DDL mode: `validate`
- Liquibase changelog: `classpath:db/changelog/db.changelog-master.xml`
- Liquibase context: `faker` for Todo sample data
- JWT issuer: `https://auth.spring-grpc-samples.local`

The checked-in JWT secret is for local sample use only. Replace it for any real deployment.

## Run Locally

Start the gRPC server:

```bash
./mvnw spring-boot:run
```

The server listens on:

```text
localhost:9090
```

Seeded users:

| Username | Password | Authorities |
| --- | --- | --- |
| `admin` | `admin` | `ROLE_ADMIN`, `ROLE_USER` |
| `user` | `user` | `ROLE_USER` |

## API Quick Overview

Auth:

- `AuthApi/Login`

Todos:

- `TodoApi/CreateTodo`
- `TodoApi/GetTodo`
- `TodoApi/ListTodos`
- `TodoApi/UpdateTodo`
- `TodoApi/PatchTodo`
- `TodoApi/DeleteTodo`

Infrastructure:

- `grpc.health.v1.Health`
- `grpc.reflection.v1.ServerReflection`

Security:

- `AuthApi/Login` is public.
- `grpc.*/*` infrastructure calls are public.
- `TodoApi/*` requires `ROLE_ADMIN`.
- Other non-public calls require authentication.

## gRPC Contract

The protobuf contract is in:

```text
src/main/proto/todo.proto
```

Generated Java classes use:

```text
io.github.susimsek.springgrpcsamples.proto
```

After changing the proto file, regenerate sources:

```bash
./mvnw generate-sources
```

Do not edit generated files under `target/`.

## Try with grpcurl

List services:

```bash
grpcurl -plaintext localhost:9090 list
```

Expected services include:

```text
AuthApi
TodoApi
grpc.health.v1.Health
grpc.reflection.v1.ServerReflection
```

Health check:

```bash
grpcurl -plaintext \
  -d '{"service": ""}' \
  localhost:9090 \
  grpc.health.v1.Health/Check
```

Login as admin:

```bash
TOKEN=$(grpcurl -plaintext \
  -d '{"username":"admin","password":"admin"}' \
  localhost:9090 \
  AuthApi/Login | jq -r '.access_token')

echo "$TOKEN"
```

List todos:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"page":0,"size":5}' \
  localhost:9090 \
  TodoApi/ListTodos
```

Create todo:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"title":"Write README"}' \
  localhost:9090 \
  TodoApi/CreateTodo
```

Get todo:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"id":1}' \
  localhost:9090 \
  TodoApi/GetTodo
```

Update todo:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"id":1,"title":"Update README","completed":true}' \
  localhost:9090 \
  TodoApi/UpdateTodo
```

Patch todo:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"id":1,"completed":false}' \
  localhost:9090 \
  TodoApi/PatchTodo
```

Delete todo:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"id":1}' \
  localhost:9090 \
  TodoApi/DeleteTodo
```

Invalid token example:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer invalid-token" \
  -rpc-header "accept-language: tr" \
  -d '{"page":0,"size":5}' \
  localhost:9090 \
  TodoApi/ListTodos
```

Access denied example with `ROLE_USER`:

```bash
USER_TOKEN=$(grpcurl -plaintext \
  -d '{"username":"user","password":"user"}' \
  localhost:9090 \
  AuthApi/Login | jq -r '.access_token')

grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${USER_TOKEN}" \
  -rpc-header "accept-language: tr" \
  -d '{"page":0,"size":5}' \
  localhost:9090 \
  TodoApi/ListTodos
```

## Validation and Error Details

Request validation is handled by Protovalidate in `GrpcValidationServerInterceptor`.

Example invalid request:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -rpc-header "accept-language: tr" \
  -d '{"title":"ab"}' \
  localhost:9090 \
  TodoApi/CreateTodo
```

Expected gRPC status:

```text
Code: InvalidArgument
Message: Bir veya daha fazla dogrulama hatasi olustu.
Details: google.rpc.BadRequest
```

For decoded `google.rpc.BadRequest` details with `grpcurl`, make sure `google/rpc/error_details.proto` is available in an import path.

## Database

The app uses H2 in-memory database in PostgreSQL compatibility mode.

Liquibase files:

- `src/main/resources/db/changelog/db.changelog-master.xml`
- `src/main/resources/db/changelog/changes/001-create-todos.xml`
- `src/main/resources/db/changelog/changes/002-create-users.xml`

Seed files:

- `src/main/resources/db/data/todos.csv`
- `src/main/resources/db/data/users.csv`
- `src/main/resources/db/data/authorities.csv`
- `src/main/resources/db/data/user-authorities.csv`

Seed data uses:

- Liquibase context `faker` for Todo sample data
- User, authority, and user-authority seed data always run
- CSV separator: `;`

## Internationalization

Message bundles:

- `src/main/resources/i18n/messages.properties`
- `src/main/resources/i18n/messages_tr.properties`

Use `accept-language` gRPC metadata to request localized errors:

```bash
-rpc-header "accept-language: tr"
```

## Build

Generate protobuf sources:

```bash
./mvnw generate-sources
```

Run unit tests:

```bash
./mvnw test
```

Run full verification:

```bash
./mvnw verify
```

Reliable local full check:

```bash
./mvnw test && ./mvnw verify
```

Package:

```bash
./mvnw -DskipTests package
```

Native executable:

```bash
./mvnw -Pnative -DskipTests native:compile
```

Output:

```text
target/spring-grpc-samples
```

JaCoCo report:

```text
target/site/jacoco/jacoco.xml
```

## Code Quality

### Checkstyle

Checkstyle runs automatically in the `validate` phase.

```bash
./mvnw checkstyle:check
```

Config files:

- `checkstyle.xml`
- `checkstyle-suppressions.xml`

### Spotless

Spotless runs `spotless:check` in the `compile` phase.

Check formatting:

```bash
./mvnw spotless:check
```

Apply formatting:

```bash
./mvnw spotless:apply
```

### Sonar

Sonar properties live in:

```text
sonar-project.properties
```

Run analysis:

```bash
./mvnw -Psonar sonar:sonar
```

### Coverage

JaCoCo requires 100% line and instruction coverage for handwritten application code.

Generated code excluded from coverage:

- protobuf generated classes
- MapStruct generated implementation classes

Handwritten classes, including the Spring Boot application class and JPA entities, are covered by unit tests.

## GraalVM Native Image

This project uses Spring Boot AOT and GraalVM Native Build Tools.

Build the native executable:

```bash
./mvnw -Pnative -DskipTests native:compile
```

Run it:

```bash
./target/spring-grpc-samples
```

Then test health:

```bash
grpcurl -plaintext \
  -d '{"service": ""}' \
  localhost:9090 \
  grpc.health.v1.Health/Check
```

## Kubernetes Health Probe

Spring gRPC exposes the standard gRPC Health Checking service.

For Kubernetes, use a gRPC probe against port `9090`:

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

Local equivalent:

```bash
grpcurl -plaintext \
  -d '{"service": ""}' \
  localhost:9090 \
  grpc.health.v1.Health/Check
```
