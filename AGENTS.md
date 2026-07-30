# AI Agent Guidelines

This repo is a Java 25 + Spring Boot 4.1 sample application that exposes a server-side gRPC API for Todo CRUD and JWT login. It uses Spring gRPC, Spring Data JPA, H2, Liquibase XML changelogs, MapStruct, Lombok, Protovalidate, Spring Security OAuth2 Resource Server, i18n messages, Spotless, Checkstyle, Sonar, JaCoCo, and GraalVM Native Image support.

## Table of Contents

1.  [Agent MCP Usage Guidelines](#agent-mcp-usage-guidelines)
2.  [Quick Reference](#quick-reference)
3.  [Prerequisites](#prerequisites)
4.  [Project Structure](#project-structure)
5.  [Code Style and Quality Gates](#code-style-and-quality-gates)
6.  [Testing Guidelines](#testing-guidelines)
7.  [Native Image & AOT Guidance](#native-image--aot-guidance)
8.  [Authentication](#authentication)
9.  [Development Guidelines](#development-guidelines)
10. [Pull Request and Commit Guidelines](#pull-request-and-commit-guidelines)
11. [Review Process & What Reviewers Look For](#review-process--what-reviewers-look-for)
12. [Common Mistakes to Avoid](#common-mistakes-to-avoid)

## Agent MCP Usage Guidelines

- Use Context7 when library/API documentation is needed for Spring Boot, Spring gRPC, Spring Security, Spring Data JPA, Hibernate, Liquibase, MapStruct, Protovalidate, Protobuf, gRPC, Maven plugins, or related setup/configuration details.
- Prefer official documentation or primary sources for framework behavior.

## Quick Reference

| Action | Command |
| --- | --- |
| Run dev server | `./mvnw spring-boot:run` |
| Run prod server | `./mvnw -Pprod spring-boot:run` |
| Run prod + docker-compose | `./mvnw -Pprod,docker-compose spring-boot:run` |
| Generate protobuf sources | `./mvnw generate-sources` |
| Unit tests | `./mvnw test` |
| Full verify | `./mvnw verify` |
| Reliable full check | `./mvnw test && ./mvnw verify` |
| Format check | `./mvnw spotless:check` |
| Format apply | `./mvnw spotless:apply` |
| Checkstyle | `./mvnw checkstyle:check` |
| Package | `./mvnw -DskipTests package` |
| Native executable | `./mvnw -Pprod,native -DskipTests native:compile` |
| Sonar scan | `./mvnw -Psonar sonar:sonar` |

## Prerequisites

- Java: `25+`
- Maven: use the wrapper (`./mvnw`)
- Optional gRPC testing tools:
  - `grpcurl`
  - Insomnia or another gRPC-capable API client
- Optional native build tooling:
  - GraalVM Native Image

## Project Structure

- Application root: `src/main/java/io/github/susimsek/springgrpcsamples`
  - `config`: Spring configuration
    - `aot`: GraalVM Native Image runtime hints (`NativeRuntimeHints`)
    - `i18n`: gRPC locale resolution from metadata such as `accept-language`
    - `security`: JWT, password encoder, authentication manager, and gRPC security interceptor
    - `validation`: Protovalidate validator/interceptor configuration
  - `domain`: JPA entities (`Todo`, `User`, `Authority`) and auditing base class
  - `exception`: gRPC exception model and `@GrpcAdvice` exception mapping
  - `mapper`: MapStruct mappers and protobuf mapping helpers
  - `repository`: Spring Data JPA repositories
  - `security`: authority constants, JWT service, user-details service, security utilities
  - `service`: gRPC service implementations
- Protobuf contract: `src/main/proto/todo.proto`
- Application config: `src/main/resources/config/application.yml`
- Liquibase:
  - Master: `src/main/resources/db/changelog/db.changelog-master.xml`
  - Changelogs: `src/main/resources/db/changelog/changes`
  - CSV seed data: `src/main/resources/db/data`
- i18n messages:
  - Default English: `src/main/resources/i18n/messages.properties`
  - Turkish: `src/main/resources/i18n/messages_tr.properties`
- Docker compose: `src/main/docker/*.yml`
- Helm chart: `helm/spring-grpc-samples`
- Tests: `src/test/java`



## Code Style and Quality Gates

- Formatting: Spotless with `google-java-format` AOSP.
- Checkstyle runs in the `validate` phase.
- Follow `.editorconfig`:
  - LF line endings
  - final newline
  - no trailing whitespace
  - Java indent size 4
  - YAML indent size 2
- Avoid global coverage excludes for handwritten code.
- JaCoCo excludes should stay limited to generated code:
  - protobuf generated classes
  - MapStruct generated implementation classes
- Do not edit generated sources under `target/`.
- When you change code: apply formatting and ensure tests pass (`./mvnw spotless:apply` and `./mvnw test`).

## Testing Guidelines

- Unit tests live under `src/test/java` and use singular class names ending with `Test`.
- Keep test class names class-based, for example:
  - `TodoTest`
  - `UserTest`
  - `AuthorityTest`
  - `TodoMapperTest`
  - `TodoGrpcServiceTest`
- Maintain 100% JaCoCo line and instruction coverage for handwritten application code.
- The current verification command is:
  - `./mvnw test && ./mvnw verify`
- Some tests intentionally exercise unhandled exception logging; a `boom` stack trace in test output can be expected if the build exits successfully.

### gRPC Testing

- Server port: `9090`.
- Reflection and health are enabled by Spring gRPC.
- Health check:

```bash
grpcurl -plaintext -d '{"service": ""}' localhost:9090 grpc.health.v1.Health/Check
```

- List services:

```bash
grpcurl -plaintext localhost:9090 list
```

- Login:

```bash
TOKEN=$(grpcurl -plaintext \
  -d '{"username":"admin","password":"admin"}' \
  localhost:9090 \
  AuthApi/Login | jq -r '.access_token')
```

- Call a secured Todo API:

```bash
grpcurl -plaintext \
  -rpc-header "authorization: Bearer ${TOKEN}" \
  -rpc-header "accept-language: tr" \
  -d '{"page":0,"size":5}' \
  localhost:9090 \
  TodoApi/ListTodos
```

## Native Image & AOT Guidance

- Native builds use Spring Boot AOT and GraalVM Native Build Tools.
- Build with `./mvnw -Pprod,native -DskipTests native:compile`.
- Runtime hints live in `config/aot/NativeRuntimeHints`; update hints there when adding:
  - New Liquibase XML/CSV resources
  - New i18n message bundles
  - New protobuf descriptor files
  - Types requiring reflection (e.g. custom validators, serializers)
- If native runtime fails because resources are missing, add focused `RuntimeHints` instead of broad classpath inclusion.
- Pay attention to Liquibase XML/CSV resources, i18n bundles, protobuf descriptors, H2, Hibernate, and Protovalidate when changing native-sensitive code.

## Authentication

- `AuthApi/Login` is public and returns a JWT access token.
- `TodoApi/*` requires `ROLE_ADMIN`.
- `grpc.*/*` infrastructure calls such as reflection and health are public.
- Authentication uses normal JWT bearer tokens, not opaque tokens.
- JWT authorities are stored in the `auth` claim. Use `SecurityUtils.AUTHORITIES_CLAIM`.
- Seeded users for local dev: `admin/admin` (ROLE_ADMIN + ROLE_USER), `user/user` (ROLE_USER only).
- JWT is sent as `Authorization: Bearer <token>` gRPC metadata.
- `prod` requires `APP_SECURITY_JWT_SECRET` (at least 256-bit, e.g. `openssl rand -hex 32`).
- The checked-in JWT secret is for local sample use only; never commit real secrets.
- Todo APIs should stay secured, but should not require admin globally for every possible service. Current rule: `TodoApi/*` requires `ROLE_ADMIN`; all other non-public calls require authentication.
- Keep constants in `AuthoritiesConstants`.
- Keep user loading in `DomainUserDetailsService`.
- Do not store plain text passwords in seed data; use BCrypt hashes.


## Development Guidelines

### Architecture

- Keep the flow: gRPC service implementation → repository. Do not push business logic into interceptors.
- Service implementations live in `service/`; they call repositories and throw `GrpcApiException` subclasses for domain errors.
- Do not return JPA entities directly from service methods; map to proto responses via MapStruct.

### Validation

- Request validation is handled at the gRPC boundary by `GrpcValidationServerInterceptor` and Protovalidate.
- Prefer standard Protovalidate rules for scalar constraints where possible.
- Custom CEL rule ids should start with `grpc.`.
- Non-custom rule ids are treated as Protovalidate standard rule ids and mapped under `grpc.validation.constraints.<rule_id>`.
- Validation failures should return `INVALID_ARGUMENT` with `google.rpc.BadRequest` details.
- i18n validation messages live in `src/main/resources/i18n`.

### Error Handling and i18n

- Centralize gRPC exception handling in `GlobalGrpcExceptionHandler`.
- Use `@GrpcAdvice` and `@GrpcExceptionHandler`; do not manually handle expected domain exceptions inside service methods.
- Use `GrpcApiException` subclasses for application errors such as not found or invalid credentials.
- Use `accept-language` metadata for localized responses.
- Keep default messages in `messages.properties`; add Turkish translations in `messages_tr.properties`.
- Throw `GrpcApiException` subclasses (e.g. `ResourceNotFoundException`, `BadCredentialsException`) from service layer.
- `GlobalGrpcExceptionHandler` maps them to gRPC status codes and `google.rpc` details automatically.
- Do not catch and rethrow `StatusRuntimeException` manually in service methods.

### Transaction Management

- Put `@Transactional` on service methods; use `@Transactional(readOnly = true)` for read-only paths.
- Avoid `@Transactional` in gRPC service implementations; delegate to a transactional service layer if needed.

### Security

- APIs are authenticated by default via `GrpcSecurityInterceptor`; explicitly permit public RPCs in `SecurityConfig`.
- Keep `ROLE_ADMIN` checks in `SecurityConfig` (not scattered across service methods).
- Keep authority constants in `AuthoritiesConstants`.

### Database and Liquibase

- Use XML-based Liquibase changelogs.
- Use lowercase database types in changelog XML (`bigint`, `varchar`, `boolean`, `timestamp`).
- Shared Liquibase properties such as `${now}` belong in `db.changelog-master.xml`.
- Todo sample seed data uses context `faker`.
- User, authority, and user-authority seed data must not be tied to the `faker` context; they should always run.
- CSV seed files use semicolon separators.
- Use sequence names like `todo_seq`, `user_seq`, and `authority_seq`.
- Auditing fields are `created_at` and `updated_at`, mapped as `Instant` in `AuditableEntity`.
- For DB changes: add a new Liquibase XML changelog and include it from `db/changelog/db.changelog-master.xml`.
- Do not modify existing changelogs that have already been applied.

### Docker Compose (Optional)

- Spring Boot Docker Compose integration is enabled only with the Maven profile `-Pdocker-compose`.
- Compose config: `src/main/docker/services.yml` (starts PostgreSQL for `prod` profile).
- `spring.docker.compose.lifecycle-management=start-only` means containers are not stopped automatically.


## Pull Request and Commit Guidelines

- Keep changes focused; avoid drive-by refactors in the same PR.
- Prefer small, logically grouped commits; avoid `WIP`/"fix typo" noise.
- Do not commit local generated output such as `target/`.
- Do not commit secrets. The checked-in JWT secret is local sample configuration only.
- Before opening a PR: apply formatting and run tests (`./mvnw spotless:apply` and `./mvnw test`).
- Use **Conventional Commits**:
  - `feat`: new feature
  - `fix`: bug fix
  - `docs`: documentation only
  - `test`: adding or fixing tests
  - `chore`: build, CI, or tooling changes
  - `perf`: performance improvement
  - `refactor`: code changes without feature or fix
  - `build`: changes that affect the build system
  - `ci`: CI configuration
  - `style`: code style (formatting, missing semicolons, etc.)
  - `revert`: reverts a previous commit
- Commit message format:
  ```
  <type>(<scope>): <short summary>

  Optional longer description.
  ```
- Keep summary under 80 characters. Use imperative present tense.
- PR description should include:
  - What changed and why.
  - How to verify (exact commands and/or steps).
  - Any risks, rollback notes, or follow-ups.
- Call out cross-cutting impacts explicitly when relevant:
  - Liquibase migrations
  - Security rules (`SecurityConfig`)
  - Native Image / AOT hints (`NativeRuntimeHints`)
  - i18n message keys (`messages.properties`, `messages_tr.properties`)

## Review Process & What Reviewers Look For

- ✅ All automated checks pass (build, tests, Spotless, Checkstyle).
- ✅ Changes are focused and minimal; no unrelated refactors or drive-by cleanups.
- ✅ Commit history is clean, logical, and follows Conventional Commits.
- ✅ No secrets or environment-specific values are committed.
- ✅ PR description clearly explains what changed, how to verify, and any risks.
- ✅ Unit tests are added or updated to cover new behavior and edge cases.
- ✅ gRPC service flow is correct: service → repository, errors via `GrpcApiException`.
- ✅ Transaction boundaries are correct (`@Transactional(readOnly = true)` for reads).
- ✅ Cross-cutting impacts are explicitly called out when applicable:
  - Liquibase migrations
  - Security rules (`SecurityConfig`)
  - Native Image / AOT hints (`NativeRuntimeHints`)
  - i18n message keys (`messages.properties`, `messages_tr.properties`)

## Common Mistakes to Avoid

- Adding Spring Web just to get HTTP constants or MVC behavior.
- Reintroducing REST/Web MVC exception models into gRPC exception handling.
- Editing generated proto Java classes manually.
- Forgetting `./mvnw generate-sources` after changing `todo.proto`.
- Forgetting to import `buf/validate/validate.proto` when using external tools such as Insomnia.
- Applying `ROLE_ADMIN` checks globally to every service instead of securing intended RPC methods.
- Adding handwritten classes to JaCoCo excludes instead of testing them.
- Using comma-separated Liquibase seed CSV files; this project uses `separator=";"`.
- Adding new Liquibase resources or i18n bundles without updating `NativeRuntimeHints`.
- Forgetting `./mvnw spotless:apply` before committing; Spotless runs at compile phase and will fail the build.
- In Mockito `verify/when/given`, avoid useless `eq(...)` matchers; pass values directly and use `ArgumentCaptor` when you need to assert arguments.
- Avoid redundant temporary variables like `var result = expr; return result;` — return the expression directly.
- Avoid unused initial assignments just to overwrite them later.
- Don't forget a `default` branch in `switch` statements.
- Using fully qualified names everywhere instead of imports (only use FQNs to resolve ambiguity).
