# Junie Development Guidelines

This document defines the development standards to follow for this project.

## Project Structure
com.payable.assessment
├── controller/
│   ├── BinMasterController.java
│   └── TransactionController.java
├── service/
│   ├── BinMasterService.java
│   └── TransactionService.java
├── model/
│   ├── TransactionRow.java       ← input CSV row
│   └── OutputRow.java            ← output CSV row
├── dto/
│   └── BinUploadResponse.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── InvalidFileException.java
└── AssessmentApplication.java

## 1. Software Engineering Best Practices

- Write clean, readable, maintainable code.
- Prefer simplicity over unnecessary abstraction.
- Keep classes and methods small and focused.
- Follow separation of concerns.
- Avoid duplication and extract reusable logic when appropriate.
- Make code easy to test, debug, and extend.
- Handle edge cases and invalid input explicitly.
- Keep business logic out of controllers and infrastructure code.
- Prefer explicit behavior over hidden side effects.

## 2. Architectural Style

Use a layered architecture with clear separation of responsibilities:

- **Controller/API layer**: handles HTTP requests and responses.
- **Service layer**: contains business logic.
- **Repository/DAO layer**: handles database operations.
- **Entity/Domain layer**: represents persistent and business data.
- **Configuration layer**: contains app, security, and bean configuration.

### Rules
- Controllers must not contain business logic.
- Services must not directly expose persistence details.
- Repositories must not contain business rules.
- Keep dependencies clean and minimal between layers.
- Prefer loosely coupled and highly cohesive components.

## 3. Naming Conventions

Use consistent, meaningful names.

### Java naming
- **Classes / Interfaces**: `PascalCase`
    - Example: `UserService`, `JwtTokenFilter`
- **Methods / Variables**: `camelCase`
    - Example: `findUserByEmail`, `tokenExpiryTime`
- **Constants**: `UPPER_SNAKE_CASE`
    - Example: `JWT_SECRET_KEY`, `DEFAULT_PAGE_SIZE`
- **Packages**: lowercase and dot-separated
    - Example: `com.example.app.service`

### Boolean names
- Use prefixes such as `is`, `has`, `can`, `should`
    - Example: `isActive`, `hasAccess`, `canEdit`

### General naming rules
- Use descriptive names.
- Avoid vague abbreviations.
- Choose names based on business meaning.
- Use singular/plural properly.

## 4. SOLID Principles

### Single Responsibility Principle
Each class should have one responsibility and one reason to change.

### Open/Closed Principle
Components should be extendable without modifying existing code.

### Liskov Substitution Principle
Subclasses must be usable wherever their base types are expected.

### Interface Segregation Principle
Prefer small, focused interfaces over large, general ones.

### Dependency Inversion Principle
Depend on abstractions, not concrete implementations.

## 5. ACID Principles for Database Management

### Atomicity
A transaction must complete fully or not at all.

### Consistency
Data must remain valid before and after a transaction.

### Isolation
Concurrent transactions must not interfere with each other incorrectly.

### Durability
Committed data must persist even after failures.

### Transaction rules
- Use transactions when multiple database updates must succeed together.
- Roll back when business rules or persistence rules fail.
- Keep transactions as short as possible.

## 6. Database Indexing Handling

- Add indexes on columns frequently used in:
    - `WHERE`
    - `JOIN`
    - `ORDER BY`
    - `GROUP BY`
- Use composite indexes for common multi-column filters.
- Put columns in composite indexes in the order that best matches query patterns.
- Avoid over-indexing because it hurts insert/update/delete performance.
- Index high-selectivity columns when possible.
- Review query plans before adding indexes.
- Remove unused or redundant indexes.
- Do not create indexes without a performance reason.

## 7. Technology Stack

### Core Technologies
- **Java**: Version 17
- **Build Tool**: Maven
- **Framework**: Spring Boot 3.3.x
- **Authentication**: Spring Security
- **Persistence**: Spring Data JPA
- **Database**: MySQL

### Libraries & Tools
- **Lombok**: For reducing boilerplate code.
- **Testing**: JUnit 5, Mockito, Spring Boot Starter Test.
- **DevTools**: Spring Boot DevTools for enhanced development experience.

## 8. Unit Testing with Mockito

- Write unit tests for business logic and important edge cases.
- Use **Mockito** to mock dependencies.
- Keep tests isolated and deterministic.
- Avoid external dependencies in unit tests.
- Test one behavior per test when practical.
- Verify only important interactions.
- Name tests clearly based on behavior.

### Test guidance
- Include success, failure, and boundary scenarios.
- Prefer readable test setup.
- Do not mock the class under test.
- Use assertions that clearly explain expected behavior.

## 9. Logging Guidelines

- Log important application events.
- Use the correct log level:
    - `ERROR` for failures
    - `WARN` for recoverable problems
    - `INFO` for major business events
    - `DEBUG` for troubleshooting
- Keep logs meaningful and concise.
- Do not log secrets, passwords, tokens, or sensitive personal data.
- Include context such as IDs when useful.
- Log exceptions with enough detail for troubleshooting.

## 10. Comments for Understanding

- Add comments only when they improve understanding.
- Use comments to explain **why**, not what is already obvious.
- Document complex business rules or non-obvious logic.
- Avoid redundant or misleading comments.
- Remove outdated comments immediately.

## 11. JWT Authentication with Spring Security

### JWT rules
- Use JWT for **stateless** authentication.
- Validate token integrity, expiration, and claims on every request.
- Reject invalid, expired, tampered, or malformed tokens.
- Use Spring Security filters to process JWTs.
- Keep token payload minimal and non-sensitive.
- Do not store server-side session state for JWT authentication.

### Security rules
- Never hardcode secrets.
- Store signing keys securely.
- Use strong signing algorithms.
- Set appropriate expiration times.
- Protect login and token refresh flows carefully.
- Verify token claims such as issuer, subject, and expiration when needed.

## 12. Constant Variable Usage

- Use constants for values that do not change.
- Avoid magic numbers and magic strings.
- Use `static final` for Java constants.
- Keep constants in a logical shared location.
- Do not turn changing values into constants.
- Name constants clearly and consistently.

## 13. Exception Handling

- Handle exceptions at the correct layer.
- Do not swallow exceptions silently.
- Use meaningful error messages.
- Preserve root causes when rethrowing exceptions.
- Prefer custom exceptions for domain-specific failures.
- Use global exception handling for API responses when appropriate.
- Do not expose stack traces or internal details to clients.

### API error handling
- Return consistent error structures.
- Differentiate validation, authorization, authentication, and business errors.
- Keep responses clear and safe.

## 14. JPQL Query Guidelines

- Prefer JPQL for entity-based queries when it improves readability.
- Use parameter binding instead of string concatenation.
- Avoid SQL injection risks.
- Keep queries readable and maintainable.
- Use joins only when necessary.
- Review generated SQL for performance-sensitive queries.
- Use native SQL only when JPQL is insufficient.

## 15. Concurrency and Caching

### Concurrency
- Avoid shared mutable state when possible.
- Design code to be safe under concurrent access.
- Use transactions and locking carefully when consistency matters.
- Prevent race conditions, duplicate processing, and lost updates.

### Caching
- Use caching only when it adds clear value.
- Define cache keys carefully.
- Set expiration and invalidation rules properly.
- Do not cache sensitive data without strong justification.
- Ensure caching does not break correctness.

## 16. Code Quality

- Keep code consistent and maintainable.
- Remove dead code and unused imports.
- Avoid deeply nested logic where possible.
- Prefer clear abstractions over premature optimization.
- Follow project formatting and style rules.
- Refactor regularly to reduce complexity.
- Keep dependencies justified and minimal.

### Quality expectations
- Code should compile and pass tests.
- Use static analysis if available.
- Review null handling, edge cases, and error paths.

## 17. Performance

- Avoid unnecessary database calls.
- Prevent N+1 query problems.
- Use pagination for large result sets.
- Fetch only needed data.
- Use indexes only where they help.
- Cache expensive repeated operations when appropriate.
- Measure before optimizing.
- Focus on real bottlenecks, not assumptions.

## 18. Final Rules for Implementation

When making changes:
- follow the existing architecture
- keep changes small and focused
- preserve backward compatibility when possible
- update or add tests for behavior changes
- ensure logging, security, and exception handling are considered
- keep the code readable for future maintainers

1.Create junie/guidelines.md concisely describing style guidelines for Java Spring Boot.
Mention preferring constructor injection over field injection, using Records for DTOs,
following RESTful naming conventions, and keeping controllers thin by delegating to services.

2.Analyze the docs/requirements.md and create a detailed plan for the improvements of this Spring Boot project.
Include package structure, new classes/interfaces, and database migration steps. Write the plan to docs/plan.md.

3.Create a detailed enumerated task list according to the suggested enhancements plan in docs/plan.md.
Task items should have a placeholder [ ] for marking as done [x] upon task completion.
Include tasks for: entity/repository creation, service layer, controller, DTO records, exception handling, and tests.
Write the task list to docs/tasks.md.

4.Start implementing the Spring Boot application according to the tasks listed in docs/tasks.md.
Follow the guidelines in junie/guidelines.md. Mark tasks as done [x] upon completion.
Ensure all layers (Controller → Service → Repository) are wired correctly and tests are green.


1. Code Design & Architecture
   SOLID Principles

Single Responsibility — one class, one job
Open/Closed — open for extension, closed for modification
Liskov Substitution — subclasses must be replaceable for their parent
Interface Segregation — don't force classes to implement unused methods
Dependency Inversion — depend on abstractions, not concrete classes

Design Patterns to know and use

Creational: Factory, Builder, Singleton (thread-safe)
Structural: Adapter, Decorator, Facade
Behavioural: Strategy, Observer, Command, Template Method


2. Clean Code Practices

Meaningful names — getUserById() not getData()
Methods should do ONE thing only
Keep methods short — ideally under 20 lines
Avoid magic numbers — use named constants
No deep nesting — max 2-3 levels, extract to methods
Fail fast — validate inputs at the top of methods
Prefer composition over inheritance
Remove dead code — don't comment it out, delete it


3. Java-Specific Best Practices
   Core Language

Use Optional<T> instead of returning null
Prefer immutable objects — final fields, no setters
Use var (Java 10+) wisely for local variables
Prefer interfaces over concrete types in declarations (List not ArrayList)
Override equals(), hashCode(), and toString() properly
Use records (Java 16+) for simple data carriers
Use sealed classes (Java 17+) for controlled hierarchies

Collections & Streams

Use Streams for transformations, not for everything
Prefer Map.getOrDefault() over null checks
Use Collections.unmodifiableList() to protect returned lists
Use EnumMap and EnumSet when keys are enums

Concurrency

Prefer java.util.concurrent over raw synchronized
Use ExecutorService instead of creating raw threads
Use ConcurrentHashMap, AtomicInteger etc. for thread safety
Understand volatile and when to use it
Avoid shared mutable state wherever possible

Exception Handling

Never swallow exceptions with empty catch blocks
Catch specific exceptions, not just Exception
Use custom exceptions for business logic errors
Always clean up resources with try-with-resources
Log exceptions with full context, not just the message


4. Testing Best Practices

Write tests FIRST when fixing bugs (regression tests)
Follow AAA pattern — Arrange, Act, Assert
One assertion concept per test
Use descriptive test names — shouldReturnEmptyWhenUserNotFound()
Mock only external dependencies (DB, APIs) — not your own classes
Aim for 80%+ unit test coverage on business logic
Write integration tests for repository and API layers
Use @ParameterizedTest for multiple input scenarios
Never use Thread.sleep() in tests — use Awaitility

Tools: JUnit 5, Mockito, AssertJ, Testcontainers

5. Spring Boot Best Practices (if applicable)

Use constructor injection, NOT field injection (@Autowired on fields)
Keep controllers thin — business logic goes in services
Use @Transactional at service layer, not controller
Validate inputs with @Valid and Bean Validation annotations
Use DTOs to separate API contracts from domain entities
Never expose JPA entities directly in REST responses
Use application.yml over application.properties for readability
Externalise all config — no hardcoded URLs or credentials
Use profiles (dev, staging, prod) for environment config


6. API Design

Follow RESTful conventions — proper HTTP verbs and status codes
Use 200, 201, 400, 401, 403, 404, 500 correctly
Version your APIs — /api/v1/...
Return consistent error response structure
Paginate large collections — never return unbounded lists
Document with OpenAPI / Swagger annotations
Validate all incoming request bodies


7. Database & JPA Best Practices

Avoid N+1 query problem — use JOIN FETCH or @EntityGraph
Use pagination with Pageable — never findAll() on large tables
Write meaningful index strategies
Use database migrations — Flyway or Liquibase
Keep transactions short and focused
Prefer @Query with JPQL over complex method name queries


8. Security Practices

Never log sensitive data (passwords, tokens, PII)
Always validate and sanitise user inputs
Use parameterised queries — never string-concatenate SQL
Store passwords with BCrypt, never plain text or MD5
Use HTTPS everywhere, no exceptions
Apply principle of least privilege for roles and DB users
Keep dependencies updated — scan with OWASP Dependency Check


9. Performance Practices

Cache expensive operations — use Spring Cache with Redis or Caffeine
Use lazy loading wisely in JPA — understand the difference from eager
Profile before optimising — don't guess bottlenecks
Use connection pooling — HikariCP (Spring Boot default)
Avoid creating objects in tight loops
Use StringBuilder instead of + string concatenation in loops


10. Code Quality & Tooling

Use static analysis — SonarQube, SpotBugs, Checkstyle, PMD
Enforce code style with .editorconfig and formatting rules
Use Lombok wisely — @Builder, @Slf4j, @RequiredArgsConstructor
Write meaningful commit messages — feat:, fix:, refactor: prefixes
Do proper code reviews — check logic, not just style
Use feature branches and pull requests — never commit directly to main
Add CI pipeline checks — build, test, lint must pass before merge


11. Logging Best Practices

Use SLF4J with Logback or Log4j2 — never System.out.println()
Use proper log levels — DEBUG for dev, INFO for business events, ERROR for failures
Include correlation/trace IDs for distributed tracing
Log the WHAT and WHY, not just the HOW
Use structured logging (JSON format) in production
Never log inside loops — it destroys performance


Senior-Level Mindset (Most Important)

Think about maintainability — will the next developer understand this in 6 months?
Think about failure modes — what happens when the DB is down? The API times out?
Think about scalability — will this work with 10x the data?
Raise concerns early — don't just build what's asked if you see a problem
Document decisions — use ADRs (Architecture Decision Records)
Review AI output critically — never blindly trust generated code

Summarize everything we have discussed so far



