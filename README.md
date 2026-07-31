# Spring gRPC Samples

[![Build Status](https://circleci.com/gh/susimsek/spring-grpc-samples/tree/main.svg?style=shield)](https://circleci.com/gh/susimsek/spring-grpc-samples/tree/main)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=spring-grpc-samples&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=spring-grpc-samples)
[![Vulnerabilities](https://snyk.io/test/github/susimsek/spring-grpc-samples/badge.svg)](https://snyk.io/test/github/susimsek/spring-grpc-samples)
[![Docker Image Size](https://img.shields.io/docker/image-size/suayb/spring-grpc-samples/latest-native?label=Image%20Size)](https://hub.docker.com/r/suayb/spring-grpc-samples)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot/)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?&logo=springsecurity&logoColor=white)](https://docs.spring.io/spring-security/)
[![GraalVM](https://img.shields.io/badge/GraalVM-25%2B-FF6600?logo=graalvm)](https://www.graalvm.org/)
[![gRPC](https://img.shields.io/badge/gRPC-High%20Performance-4285F4?logo=google&logoColor=white)](https://grpc.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Docker Compose](https://img.shields.io/badge/Docker_Compose-Orchestration-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Orchestration-326CE5?logo=kubernetes&logoColor=white)](https://kubernetes.io/)
[![Helm](https://img.shields.io/badge/Helm-Charts-0F1689?logo=helm&logoColor=white)](https://helm.sh/)
[![Codex](https://custom-icon-badges.demolab.com/badge/Codex-181717?&logo=openai&logoColor=white)](https://openai.com/codex/)

This repository is a server-side Todo sample application built with Spring Boot 4.1 + Spring gRPC + Spring Data JPA + Liquibase on Java 25. It exposes gRPC APIs for authentication and Todo CRUD, stores data in an H2 in-memory database, validates protobuf requests with Protovalidate, returns localized gRPC errors, and can be compiled as a GraalVM native executable.

There is no gRPC client module in this project.

## Table of Contents

1. [Features](#features)
2. [Requirements](#requirements)
3. [Project Layout](#project-layout)
4. [Configuration](#configuration)
5. [Configuration and Profiles](#configuration-and-profiles)
6. [Run Locally](#run-locally)
7. [API Quick Overview](#api-quick-overview)
8. [gRPC Contracts](#grpc-contracts)
9. [Try with grpcurl](#try-with-grpcurl)
10. [Validation and Error Details](#validation-and-error-details)
11. [Database](#database)
12. [Internationalization](#internationalization)
13. [Build](#build)
14. [Code Quality](#code-quality)
15. [GraalVM Native Image](#graalvm-native-image)
16. [Docker Image](#docker-image)
17. [Kubernetes Health Probe](#kubernetes-health-probe)
18. [Docker Compose Support](#docker-compose-support)
19. [Helm](#helm)
20. [Continuous Integration](#continuous-integration)

## Features

- Todo CRUD over gRPC
- Pageable `ListTodos`
- JWT login with Spring Security OAuth2 Resource Server
- `ROLE_ADMIN` authorization for `TodoService/*`
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

- Java `25`
- Maven Wrapper (`./mvnw`)
- Kubernetes `1.24+`
- Helm `3.8.0+`
- Docker or Podman *(optional, for Jib, Docker Compose, and Helm deployments)*
- GraalVM Native Image `25+` *(optional, for native builds)*
- `grpcurl` *(optional, for gRPC testing)*
- `jq` *(optional, for JSON processing)*

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
- Protobuf contract: `src/main/proto`
- Configuration: `src/main/resources/application.yml`
- Liquibase: `src/main/resources/db/changelog`
- Seed data: `src/main/resources/db/data`
- i18n messages: `src/main/resources/i18n`
- Docker compose files: `src/main/docker`
- Helm chart: `helm/spring-grpc-samples`
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
- JWT issuer: `https://spring-grpc-samples.local`

The checked-in JWT secret is for local sample use only. Replace it for any real deployment.

## Configuration and Profiles

Configuration lives under `src/main/resources/config`:

- `application.yml` (shared)
- `application-dev.yml` (H2, debug logs)
- `application-prod.yml` (PostgreSQL, cache headers)

Maven profiles:

- `dev` (default) — H2 in-memory database, devtools
- `prod` — PostgreSQL
- `native` — GraalVM native build + Jib native-image extension
- `docker-compose` — Spring Boot Docker Compose integration

`spring.profiles.active` in `application.yml` is filled via Maven resource filtering.

## Run Locally

### Dev (H2)

Start the gRPC server:

```bash
./mvnw spring-boot:run
```

The server listens on:

```text
localhost:9090
```

### Prod (PostgreSQL)

Start PostgreSQL first:

```bash
docker compose -f src/main/docker/postgresql.yml up -d
```

Then run the app with the `prod` profile:

```bash
export SECURITY_JWT_SECRET="$(openssl rand -hex 32)"
export SPRING_DATASOURCE_USERNAME=appuser
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/grpcsamples
./mvnw -Pprod spring-boot:run
```

The server listens on:

```text
localhost:9090
```

## API Quick Overview

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

- `grpc.health.v1.Health`
- `grpc.reflection.v1.ServerReflection`

Security:

- `AuthService/Login` is public.
- `grpc.*/*` infrastructure calls are public.
- `TodoService/*` requires `ROLE_ADMIN`.
- Other non-public calls require authentication.

## gRPC Contracts

The protobuf contracts are located in:

```text
src/main/proto
```

Generated Java classes use:

```text
io.github.susimsek.springgrpcsamples.proto
```

After changing any `.proto` file, regenerate the sources:

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
AuthService
TodoService
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
  AuthService/Login | jq -r '.access_token')

echo "$TOKEN"
```

List todos:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"page":0,"size":5}' \
  localhost:9090 \
  TodoService/ListTodos
```

Create todo:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"title":"Write README"}' \
  localhost:9090 \
  TodoService/CreateTodo
```

Get todo:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"id":1}' \
  localhost:9090 \
  TodoService/GetTodo
```

Update todo:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"id":1,"title":"Update README","completed":true}' \
  localhost:9090 \
  TodoService/UpdateTodo
```

Patch todo:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"id":1,"completed":false}' \
  localhost:9090 \
  TodoService/PatchTodo
```

Delete todo:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -d '{"id":1}' \
  localhost:9090 \
  TodoService/DeleteTodo
```

Invalid token example:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer invalid-token" \
  -rpc-header "accept-language: tr" \
  -d '{"page":0,"size":5}' \
  localhost:9090 \
  TodoService/ListTodos
```

Access denied example with `ROLE_USER`:

```bash
USER_TOKEN=$(grpcurl -plaintext \
  -d '{"username":"user","password":"user"}' \
  localhost:9090 \
  AuthService/Login | jq -r '.access_token')

grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${USER_TOKEN}" \
  -rpc-header "accept-language: tr" \
  -d '{"page":0,"size":5}' \
  localhost:9090 \
  TodoService/ListTodos
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
  TodoService/CreateTodo
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
target/native-executable
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

If you use SonarCloud or SonarQube in your pipeline, you can run analysis locally as well.

Sonar properties live in:

```text
sonar-project.properties
```

Run analysis:

```bash
export SONAR_TOKEN=...
./mvnw -B -ntp -Psonar verify sonar:sonar \
  -Dsonar.token="$SONAR_TOKEN"
```

### Coverage

JaCoCo requires 100% line and instruction coverage for handwritten application code.

Generated code excluded from coverage:

- protobuf generated classes
- MapStruct generated implementation classes

Handwritten classes, including the Spring Boot application class and JPA entities, are covered by unit tests.

## GraalVM Native Image

Native executable:

```bash
./mvnw -Pnative -DskipTests native:compile
```

Output: `target/native-executable`

Native-image build arguments:

```bash
./mvnw -ntp -Pnative -DskipTests \
  -DbuildArgs="--no-fallback,-Os,--static,--libc=musl,--verbose,-J-Xmx6g" \
  native:compile
```

`buildArgs` meaning:

- `--no-fallback`: fail the build instead of producing a fallback JVM image
- `-Os`: optimize for size
- `--static`: build a statically linked binary
- `--libc=musl`: link against musl (Linux/musl environments)
- `--verbose`: print detailed native-image output, useful for debugging
- `-J-Xmx6g`: give the native-image process up to about 6 GB heap

UPX compression (optional):

```bash
upx --lzma --best target/native-executable
```

- `--best`: maximum compression
- `--lzma`: use LZMA for better compression, slower but smaller

Run it:

```bash
./target/native-executable
```

Then test health:

```bash
grpcurl -plaintext \
  -d '{"service": ""}' \
  localhost:9090 \
  grpc.health.v1.Health/Check
```

## Docker Image

Build a JVM container image without a Dockerfile:

```bash
./mvnw -DskipTests jib:dockerBuild
```

Push to a registry:

```bash
./mvnw -DskipTests jib:build -Djib.to.image=YOUR_IMAGE
```

Defaults from `pom.xml`:

- Base image: `eclipse-temurin:25-jre-alpine`
- Platform: `linux/arm64`, override with `-Djib-maven-plugin.architecture=amd64` if needed

Native Docker image (GraalVM Native Image + Jib):

```bash
./mvnw -Pnative -DskipTests jib:dockerBuild \
  -Djib.to.image=spring-grpc-samples:latest-native
```

Defaults from `pom.xml`, `native` profile:

- Base image: `scratch`, contains only the native binary and no JVM
- Working directory: `/tmp`
- Platform: `linux/arm64`, override with `-Djib-maven-plugin.architecture=amd64` if needed

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

## Docker Compose Support

Files under `src/main/docker/*.yml` are marked as "dev purpose only".

- PostgreSQL: `docker compose -f src/main/docker/postgresql.yml up -d`
- App with prebuilt native image: `docker compose -f src/main/docker/app.yml up -d`

Spring Boot Docker Compose integration (optional, starts PostgreSQL automatically):

```bash
./mvnw -Pprod,docker-compose spring-boot:run
```

## Helm

- Chart: `helm/spring-grpc-samples`

Common commands:
Lint the chart:

```bash
helm lint helm/spring-grpc-samples
```

Render manifests locally:

```bash
helm template spring-grpc-samples helm/spring-grpc-samples
```

Create namespace (idempotent):

```bash
kubectl create namespace spring-grpc-samples --dry-run=client -o yaml | kubectl apply -f -
```

Install/upgrade release:

```bash
helm upgrade --install spring-grpc-samples helm/spring-grpc-samples -n spring-grpc-samples
```

Install/upgrade with values override:

```bash
helm upgrade --install spring-grpc-samples helm/spring-grpc-samples -n spring-grpc-samples -f helm/spring-grpc-samples/values.yaml
```

Uninstall release:

```bash
helm uninstall spring-grpc-samples -n spring-grpc-samples
```

## Continuous Integration

Pipeline: `.circleci/config.yml`

- `./mvnw verify` for backend tests + quality gates
- `./mvnw -Pprod,native -DskipTests native:compile` for a musl static native build
- Compress `target/native-executable` with UPX
- Push the native Docker image to Docker Hub on the `main` branch (via Jib)

Environment variables:

- SonarCloud: `SONAR_TOKEN` (optional)
- Snyk: `SNYK_TOKEN` (optional)
- Docker Hub push: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` (only on `main`)

