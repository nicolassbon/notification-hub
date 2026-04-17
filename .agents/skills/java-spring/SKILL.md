---
name: java-spring
description: Coding (unit/integration), Testing and Architecture rules for Spring Boot applications, and Maven project structure. Use when creating, generating, scaffolding, writing, or reviewing code.
---

## Code Fromatting
- Blank Lines: Use to separate logical blocks of code.
- Line Length: Maximum 120 characters.
- Default Code Style: IntelliJ IDEA Java code style.

## Simplicity
- Keep the design simple
- Always implement the simplest possible solution
- Write simple code first; ask before implementing enhancements or optional features
- Never over-engineer; ask about adding optional features or extension points
- Create new components with minimal business logic and essential fields only

## Clean Java Code
- always remove unused imports
- prefer multiple simpler lines to one more complex line
- prefer multiline Strings (text blocks) over String concatenations
- prefer imports over fully qualified class names
- never use "this" to reference instance fields
- if the project use Java 25, use it with modern syntax (var, pattern matching, records, text blocks)
- use Java SE APIs over writing custom code
- consider using Java records instead of classes with final fields
- prefer factory methods in records over passing null in constructors
- Avoid comments - prefer extracting well-named constants, methods or classes.
- Comments should be applied for: cron expressions, and Regex patterns.
- Prefer imports over fully qualified class names.
- Use constants to explain magic numbers and strings.
- Use explanatory variables for complex boolean conditions.
- Use descriptive names for classes, methods, and variables.
- Avoid meaningless suffixes: *Impl, *Manager, *Creator, *Factory
- Keep methods small (ideally <=20 lines) and flat (code indentation <= 3 TABs)
- Use Cuard Clauses to return or throw early from a function, to reduce the code indentation.
- Prefer explicit types over `var` unless the variable name captures all the semantics of it.
- Watch out for NullPointerException when the source of data can't be verified (such as coming from a REST payload).
- Avoid checked exceptions. If thrown by library code, rethrow them wrapped in a runtime exception.
- Do not create interfaces implemented by a single class; use classes directly
- Create multiple classes only if it decreases complexity and increases readability
- Avoid "getter" methods starting with "get"; prefer record convention (e.g., name() not getName())
- Create well-named methods for coarse-grained, cohesive, self-contained logic
- If a lambda requires multiple statements or braces {}, extract it into a well-named helper method
- Do not create multiline lambda expressions; prefer method references instead
- Extract repeated logic into helper methods (DRY principle)
- Avoid creating empty delegate methods which just call methods without added value
- extract variables to eliminate duplication
- prefer enums over plain Strings for finite, well-defined values
- reuse enum constants as values if possible; enum constants do not have to follow naming conventions
- prefer try-with-resources over explicitly closing resources

## Stream & Collections
- Prefer toList() to .collect(Collectors.toList()), given no later mutation of the returned list.
- Prefer List.of over new ArrayList<>()
- Prefer variable declaration over lengthy method chaining (for example after 3-4 stream operators)

## Lombok (if present in classpath)
- Always @RequiredArgsConstructor to generate constructor for injecting dependencies.
- Copy any annotations on fields in the generated constructor using lombok.copyableAnnotations+= .
- Always use @Slf4j Lombok for logging.
- Use @Builder for creating immutable objects with >= 5 fields.
- On a JPA @Entity, never use @Data; prefer @Getter and @Setter for granular control.
- Don't use any experimental Lombok feature.

## Logging
- Use placeholders (`{}`) in log messages instead of string concatenation.
- Avoid logging sensitive information.

## Spring Framework
- Use constructor injection for production code and @Autowired fiels only for tests.
- Bind 2+ related properties to an object using @ConfigurationProperties.
- Keep @Transactional methods fast: for example, they shouldn't do API calls.
- @Transactional should be used only when strictly necessary, to make atomic 2 repo.save, or 1 repo.save + one update.
- Use @Secured or @PreAuthorize: at the controller layer when using Spring Security to enforce method-level security.
- Avoid `@Order` annotation for dependency resolution.

## REST API
- The REST APIs called by a SPA should be developed code-first, rather than generating Java code from a swagger.
- Avoid returning ResponseEntity<> from @RestController methods.
- Fields of Request Dtos should generally be constrained with annotations like @NotNull, @NotBlank, ...
- All @RequestBody parameters should be @Validated.
- Use a global @RestControllerAdvice + @ExceptionHandler to handle common exceptions.
- Report to client the cause of an application error using an ErrorCode enum, wrapped inside a custom runtime exception.
- Map exceptions to appropriate HTTP status codes in REST controllers, for example: MethodArgumentNotValidException->400, NoSuchElementException->404.
- Define a consistent error response structure.

## Testing
- use JUnit 5 for unit and integration testing
- do not use private visibility in tests
- use AssertJ library instead of JUnit assertions
- use Mockito for mocking dependencies in unit tests
- separate the given/when/then sections of a @Test with an empty line, or explicit "//when" if test is large
- test method names should never start with 'test' and must be snake_case or camelCase
- test names should follow the pattern <then><when> (eg 'throwsForMissingName')
- test methods must NOT start with "test" or "should"
- never use reflection in tests
- keep tests simple and explicit
- avoid repetitive or trivial unit tests; keep only essential tests verifying core functionality
- use constants to explain magic values
- use @WebMvcTest(ControllerClass.class) for testing Spring MVC controllers
- use @SpringBootTest for integration tests traversing the entire application
- integration tests end with IT suffix and are executed by the failsafe maven plugin.
- use popular, also funny, technical terms from Java ecosystem and IT in general as examples in unit tests.


## Domain Model
- Their methods should implement simple domain rules operating only on their own field.

## JavaDoc
- do not write obvious JavaDoc comments that rephrase code
- document the intentions and the "why", not implementation details
- either describe the "why" or do not comment at all
- follow links in JavaDoc to external specifications and use them for code generation

## README Guidelines
- write brief, 'to the point' README.md files for advanced developers
- use precise and concise language; avoid generic adjectives like "simple", "lightweight"
- do not include detailed project structure (file/folder listings); high-level module descriptions are acceptable
- never list REST resources in READMEs
- if modules are listed, provide links

## System Tests (ST)
- system tests are created in a dedicated Maven module ending with "-st"
- use microprofile-rest-client for testing JAX-RS resources
- REST client interfaces: src/main/java of the -st module
- test classes: src/test/java of the -st module
- name client interfaces after the resource with "Client" suffix (e.g., GreetingsResource -> GreetingsResourceClient)
- RegisterRestClient configKey: "service_uri"
- STs end with "IT" suffix
- do not use RestAssured. Write e2e test in the -st module

## Project Management
- always ask before changing pom.xml
- create metrics and observability features with OTEL / opentelemetry