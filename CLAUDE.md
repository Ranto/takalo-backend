# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

Spring Boot 4.0.5 on Java 21, built with Maven. Persistence is PostgreSQL via Spring Data JPA, schema managed by Flyway. Lombok is enabled as an annotation processor. The DB runs locally via `docker-compose.yml` (image `postgres:16`, db `takalo_db`, user `admin` / `admin123`, port 5432) — `application.properties` is hard-wired to those credentials.

## Commands

```bash
# Start the database (required before running the app — ddl-auto=validate)
docker compose up -d

# Run the app (devtools is on the runtime classpath, so hot reload works)
./mvnw spring-boot:run

# Build / package
./mvnw clean package

# Run all tests
./mvnw test

# Run a single test class or method
./mvnw test -Dtest=TakaloApplicationTests
./mvnw test -Dtest=ProductCategoryServiceTest#shouldCreate
```

There is no Checkstyle/Spotless config — formatting follows IntelliJ defaults.

## Architecture

The codebase follows **hexagonal (ports-and-adapters) architecture**, organized by bounded context. Each context (`category`, `product`, `purchase`) replicates the same package skeleton:

```
<context>/
  application/
    port/in/      <Name>ServicePort        — inbound use-case interface
    port/out/     <Name>Repository         — outbound repository port (interface)
    service/     <Name>Service             — implements the port, @Transactional
  domain/
    model/       <Name>                    — immutable Java record (the domain model)
  infrastructure/
    persistence/
      Jpa<Name>Repository                  — Spring Data JPA interface (entity-typed)
      <Name>PersistenceAdapter             — implements the outbound repository port
      <Name>Mapper                         — domain ↔ JPA entity
      entities/<Name>Entity                — JPA @Entity (DB-shaped, mutable)
    rest/
      <Name>Controller                     — @RestController under /api/v1/<resource>
      dto/<Name>Request|Response           — wire-format records
      mapper/<Name>WebMapper               — domain ↔ DTO (when non-trivial)
```

**Key invariant: the domain layer never imports JPA, Spring Data, or web types.** Controllers and persistence adapters sit at the edges and translate to/from domain records via mappers. Services depend only on the outbound repository *port* (`application/port/out/`); the JPA-backed adapter is wired in by Spring at runtime. Outbound ports live in `application/port/out/` (canonical hexagonal placement) — do not put repository interfaces under `domain/`.

**Cross-context calls go through inbound ports, not repositories.** Example: `PurchaseService` injects `ProductServicePort` (not `ProductRepository`) to enrich purchase items with product names — see `purchase/application/service/PurchaseService.java`. When adding a new cross-context dependency, add a method to the existing port rather than reaching into another context's persistence layer.

## Shared module

`shared/` holds cross-cutting pieces:

- `shared/domain/exception/` — `ResourceNotFoundException` (→ 404), `AlreadyExistsException` (→ 409). Throw these from services; do **not** return `Optional` to controllers for "missing" cases — services should resolve and throw.
- `shared/infrastructure/rest/GlobalExceptionHandler` — `@RestControllerAdvice` that maps the above plus `DataIntegrityViolationException` (→ 400) and `MethodArgumentNotValidException` (→ 400 with field-error map). New domain exceptions belong here, not in per-controller try/catch.
- `shared/domain/utility/PagedResponse<T>` — the project's pagination envelope (record with `content`, `pageNumber`, `pageSize`, `totalElements`, `totalPages`, `isLast`, plus `.map()`). Use this in service/repository signatures instead of Spring's `Page<T>`; convert at the persistence boundary via `shared/infrastructure/utility/PaginationMapper.toPagedResponse(...)`.

## Database

- Migrations live in `src/main/resources/db/migration/` as `V<n>__<name>.sql`. Always add a new file — never edit an applied migration. `spring.jpa.hibernate.ddl-auto=validate`, so the JPA entities and the SQL schema must stay in sync or the app will fail to start.
- `spring.flyway.baseline-on-migrate=true` is set, which is convenient for local dev but worth flagging before any prod deployment.

## Conventions worth knowing

- **Domain models are `record`s.** "Updates" produce a new record (see `ProductService.update` constructing a fresh `Product` with the existing `createdAt`); don't add setters or convert them to classes.
- **Constructor injection via Lombok `@RequiredArgsConstructor`** on `final` fields. Don't introduce field injection or `@Autowired`.
- **REST base path is `/api/v1/<resource>`** and controllers are plural-noun routed (`/categories`, `/products`, `/purchases`).
- **User-facing error messages are in French** (e.g. `"Un produit avec ce nom existe déjà"`, `"Erreur d'intégrité des données…"`). Keep new messages in French to match.
- Tests are scaffolded but largely empty (`src/test/.../category/application/` exists with no files yet); `TakaloApplicationTests` is the only real test. Spring Boot's `*-test` starters (jpa-test, webmvc-test, flyway-test, actuator-test) are already on the test classpath when you add new ones.