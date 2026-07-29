# AI Agent Guidelines

This repo is a Java 25 + Spring Boot 4.1 sample application that exposes a server-side gRPC API for Todo CRUD and JWT login. It uses Spring gRPC, Spring Data JPA, H2, Liquibase XML changelogs, MapStruct, Lombok, Protovalidate, Spring Security OAuth2 Resource Server, i18n messages, Spotless, Checkstyle, Sonar, JaCoCo, and GraalVM Native Image support.

## Agent MCP Usage Guidelines

- Use Context7 when library/API documentation is needed for Spring Boot, Spring gRPC, Spring Security, Spring Data JPA, Hibernate, Liquibase, MapStruct, Protovalidate, Protobuf, gRPC, Maven plugins, or related setup/configuration details.
- Prefer official documentation or primary sources for framework behavior.

## Quick Reference

| Action | Command |
| --- | --- |
| Run dev server | `./mvnw spring-boot:run` |
| Generate protobuf sources | `./mvnw generate-sources` |
| Unit tests | `./mvnw test` |
| Full verify | `./mvnw verify` |
| Reliable full check | `./mvnw test && ./mvnw verify` |
| Format check | `./mvnw spotless:check` |
| Format apply | `./mvnw spotless:apply` |
| Checkstyle | `./mvnw checkstyle:check` |
| Package | `./mvnw -DskipTests package` |
| Native executable | `./mvnw -Pnative -DskipTests native:compile` |
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
- Application config: `src/main/resources/application.yml`
- Liquibase:
  - Master: `src/main/resources/db/changelog/db.changelog-master.xml`
  - Changelogs: `src/main/resources/db/changelog/changes`
  - CSV seed data: `src/main/resources/db/data`
- i18n messages:
  - Default English: `src/main/resources/i18n/messages.properties`
  - Turkish: `src/main/resources/i18n/messages_tr.properties`
- Tests: `src/test/java`

## Architecture Rules

- This is a server-only gRPC sample. Do not add a gRPC client module unless explicitly requested.
- Keep the gRPC contract in `src/main/proto/todo.proto`; generated Java classes belong under Maven generated sources and must not be edited manually.
- Keep persistence in JPA entities and repositories.
- Prefer direct proto request/response mapping for this sample. Do not reintroduce Todo DTO classes unless there is a real cross-boundary need.
- MapStruct should map:
  - `CreateTodoRequest` to `Todo`
  - `UpdateTodoRequest` into an existing `Todo` via `@MappingTarget`
  - `PatchTodoRequest` into an existing `Todo` via partial update
  - `Todo` to `TodoResponse`
- Keep protobuf builder helper ignores centralized with `@ProtobufMapping`; keep response-specific ignores next to the concrete mapping method.
- Keep timestamp conversions in `ProtobufMapper` as reusable protobuf mapping helpers.

## gRPC API

- `AuthApi/Login` is public and returns a JWT access token.
- `TodoApi/*` requires `ROLE_ADMIN`.
- `grpc.*/*` infrastructure calls such as reflection and health are public.
- Todo RPCs:
  - `CreateTodo`
  - `GetTodo`
  - `ListTodos`
  - `UpdateTodo`
  - `PatchTodo`
  - `DeleteTodo`

## Security

- Authentication uses normal JWT bearer tokens, not opaque tokens.
- JWT authorities are stored in the `auth` claim. Use `SecurityUtils.AUTHORITIES_CLAIM`.
- `admin/admin` is seeded with `ROLE_ADMIN` and `ROLE_USER`.
- `user/user` is seeded with `ROLE_USER` only.
- Todo APIs should stay secured, but should not require admin globally for every possible service. Current rule: `TodoApi/*` requires `ROLE_ADMIN`; all other non-public calls require authentication.
- Keep constants in `AuthoritiesConstants`.
- Keep user loading in `DomainUserDetailsService`.
- Do not store plain text passwords in seed data; use BCrypt hashes.

## Validation

- Request validation is handled at the gRPC boundary by `GrpcValidationServerInterceptor` and Protovalidate.
- Prefer standard Protovalidate rules for scalar constraints where possible.
- Custom CEL rule ids should start with `grpc.`.
- Non-custom rule ids are treated as Protovalidate standard rule ids and mapped under `grpc.validation.constraints.<rule_id>`.
- Validation failures should return `INVALID_ARGUMENT` with `google.rpc.BadRequest` details.
- i18n validation messages live in `src/main/resources/i18n`.

## Error Handling and i18n

- Centralize gRPC exception handling in `GlobalGrpcExceptionHandler`.
- Use `@GrpcAdvice` and `@GrpcExceptionHandler`; do not manually handle expected domain exceptions inside service methods.
- Use `GrpcApiException` subclasses for application errors such as not found or invalid credentials.
- Use `accept-language` metadata for localized responses.
- Keep default messages in `messages.properties`; add Turkish translations in `messages_tr.properties`.

## Database and Liquibase

- Use XML-based Liquibase changelogs.
- Use lowercase database types in changelog XML (`bigint`, `varchar`, `boolean`, `timestamp`).
- Shared Liquibase properties such as `${now}` belong in `db.changelog-master.xml`.
- Todo sample seed data uses context `faker`.
- User, authority, and user-authority seed data must not be tied to the `faker` context; they should always run.
- CSV seed files use semicolon separators.
- Use sequence names like `todo_seq`, `user_seq`, and `authority_seq`.
- Auditing fields are `created_at` and `updated_at`, mapped as `Instant` in `AuditableEntity`.

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

## Native Image Guidance

- Native builds use Spring Boot AOT and GraalVM Native Build Tools.
- Build with `./mvnw -Pnative -DskipTests native:compile`.
- If native runtime fails because resources are missing, add focused Spring `RuntimeHints` instead of broad classpath inclusion.
- Pay attention to Liquibase XML/CSV resources, i18n bundles, protobuf descriptors, H2, Hibernate, and Protovalidate when changing native-sensitive code.

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

## gRPC Testing Notes

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

## Pull Request and Commit Guidelines

- Keep changes focused.
- Avoid unrelated refactors.
- Do not commit local generated output such as `target/`.
- Do not commit secrets. The checked-in JWT secret is local sample configuration only.
- Prefer Conventional Commits:
  - `feat(grpc): add todo rpc`
  - `fix(security): handle invalid bearer token`
  - `test(domain): cover entity equality`
  - `docs(readme): update grpc usage`

## Common Mistakes to Avoid

- Adding Spring Web just to get HTTP constants or MVC behavior.
- Reintroducing REST/Web MVC exception models into gRPC exception handling.
- Editing generated proto Java classes manually.
- Forgetting `./mvnw generate-sources` after changing `todo.proto`.
- Forgetting to import `buf/validate/validate.proto` when using external tools such as Insomnia.
- Applying `ROLE_ADMIN` checks globally to every service instead of securing intended RPC methods.
- Adding handwritten classes to JaCoCo excludes instead of testing them.
- Using comma-separated Liquibase seed CSV files; this project uses `separator=";"`.
