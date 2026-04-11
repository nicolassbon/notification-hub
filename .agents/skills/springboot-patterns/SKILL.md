---
name: springboot-patterns
description: >
  Spring Boot architecture and production patterns for controllers, services,
  repositories, DTOs, validation, exception handling, testing, caching, async,
  filters, observability, and configuration. Use for Java Spring Boot backend work.
---

# Spring Boot Patterns

Use this skill for Spring Boot backend work. It mixes the clean CRUD structure of classic Spring projects with the production concerns that usually get forgotten.

## When to use

- Creating or reviewing controllers, services, repositories, DTOs, configs, or tests
- Designing REST APIs with Spring MVC or WebFlux-style boundaries
- Adding validation, transactions, caching, async flows, or filters
- Structuring a new Spring Boot codebase or refactoring an existing one
- Setting up profiles, observability, retries, and safer production defaults

## Core principles

- Keep controllers thin: HTTP only
- Put business rules in services
- Keep repositories focused on persistence
- Never expose entities directly from APIs
- Use DTOs and mappers to control boundaries
- Centralize exception handling
- Default reads to `@Transactional(readOnly = true)`
- Optimize for maintainability, testability, observability

## Recommended structure
```text
src/main/java/com/example/myapp/
├── MyAppApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── WebConfig.java
│   ├── CacheConfig.java
│   └── AsyncConfig.java
├── controller/
├── service/
│   └── impl/
├── repository/
├── model/
├── dto/
│   ├── request/
│   └── response/
├── mapper/
├── exception/
├── filter/
└── util/
```

For larger systems, prefer package-by-feature first, then keep controller/service/repository inside each feature.

## Controller pattern
```java
@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
```

### Controller rules

- Use plural nouns and versioned routes like `/api/v1/users`
- Validate input with `@Valid`
- Return `201 Created` plus `Location` on creates
- Return DTOs, never entities
- Do not call repositories from controllers
- Do not put business logic in controllers

## Service pattern
```java
public interface UserService {
    Page<UserResponse> findAll(Pageable pageable);
    UserResponse findById(Long id);
    UserResponse create(CreateUserRequest request);
    void delete(Long id);
}

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
            .map(userMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        User saved = userRepository.save(userMapper.toEntity(request));
        return userMapper.toResponse(saved);
    }
}
```

### Service rules

- Business rules and orchestration live here
- Use read-only transactions by default
- Add write transactions only where needed
- Throw domain-specific exceptions
- Keep methods cohesive and intention-revealing

## Repository pattern
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("select u from User u where u.active = true order by u.createdAt desc")
    List<User> findActiveUsers(Pageable pageable);
}
```

### Repository rules

- Prefer derived queries first
- Use `Optional` for single-result queries
- Use `existsBy...` for existence checks
- Avoid native SQL unless necessary
- Use `@EntityGraph` or fetch tuning to avoid N+1 issues

## DTOs, validation, and mapping
```java
public record CreateUserRequest(
    @NotBlank @Size(min = 2, max = 100) String name,
    @NotBlank @Email String email,
    @NotNull @Min(18) Integer age
) {}

public record UserResponse(Long id, String name, String email, LocalDateTime createdAt) {}

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(CreateUserRequest request);
}
```

### DTO rules

- Request DTOs validate input
- Response DTOs shape output intentionally
- Use MapStruct or explicit mappers when needed
- Never leak lazy relations or persistence details

## Exception handling
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", message));
    }
}

public record ErrorResponse(String code, String message) {}
```

### Error rules

- Centralize exception-to-response mapping
- Log unexpected failures with stack traces
- Return stable error codes and clean messages
- Prefer RFC 7807 Problem Details in Spring Boot 3+ when possible

## Caching and async
```java
@Service
public class UserCacheService {

    @Cacheable(value = "user", key = "#id")
    public UserResponse getById(Long id) {
        return repository.findById(id)
            .map(mapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @CacheEvict(value = "user", key = "#id")
    public void evict(Long id) {}
}

@Service
public class NotificationService {
    @Async
    public CompletableFuture<Void> sendWelcomeEmail(String email) {
        return CompletableFuture.completedFuture(null);
    }
}
```

- Enable caching and async explicitly in config
- Cache stable reads, evict on writes
- Keep async handlers idempotent and observable

## Filters, retries, and background jobs
```java
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("req method={} uri={} status={} durationMs={}",
                request.getMethod(), request.getRequestURI(), response.getStatus(),
                System.currentTimeMillis() - start);
        }
    }
}
```

- Use filters for logging, correlation IDs, and other cross-cutting concerns
- Prefer Spring Retry or Resilience4j over handwritten retry loops
- Use `@Scheduled` only for simple jobs; use queues for scalable workloads
- Keep handlers idempotent and measurable

## Rate limiting and proxy safety

- If rate limiting by client IP, never trust `X-Forwarded-For` directly
- Use `request.getRemoteAddr()` unless forwarded headers are correctly configured
- If behind a trusted proxy, configure forwarded-header handling explicitly
- Ensure your proxy overwrites forwarded headers instead of appending blindly

## Configuration and profiles
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000
```

```java
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {
    @NotBlank
    private String secret;

    @Min(60000)
    private long expiration;
}
```

- Use `application.yml` for shared config and `application-{profile}.yml` per environment
- Never hardcode secrets
- Never use `ddl-auto=create` in production
- Validate custom properties with `@ConfigurationProperties`

## Observability and production defaults

- Structured logs with request IDs and business context
- Metrics via Micrometer with Prometheus or OpenTelemetry
- Tracing via Micrometer Tracing with OpenTelemetry or Brave
- Tune HikariCP pool size and timeouts for real workload
- Prefer explicit null-safety and optionals at boundaries

## Testing patterns
```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;
}

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock UserRepository userRepository;
    @InjectMocks UserServiceImpl userService;
}

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserIntegrationTest {}
```

### Testing rules

- Controller tests: `@WebMvcTest` for HTTP contract and validation
- Service tests: Mockito for business logic and branching
- Integration tests: `@SpringBootTest` plus Testcontainers for real wiring
- Test mappers, exception contracts, and persistence edge cases

## Anti-patterns
- Controllers with business logic or repository access
- Returning entities from APIs
- Fat services mixing unrelated use cases
- Native queries as the default solution
- Hidden transactions and silent lazy-loading problems
- Hardcoded secrets or unstructured logs

## Quick checklist
- Thin controller?
- Service owns business rules?
- DTO boundary enforced?
- Exceptions centralized?
- Validation present?
- Logs, metrics, and config production-safe?

Remember: clean CRUD structure gets you started; production quality comes from boundaries, observability, safe config, and disciplined layering.
