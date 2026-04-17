# Security & Code Audit Report

> **Date:** April 11, 2026  
> **Project:** Notification Hub (Spring Boot 3.5.7, Java 21)  
> **Validation Status:** 24 findings reviewed (20 confirmed, 1 partially confirmed, 3 invalidated)

---

## Summary

> **Note after validation:** Findings **#11**, **#14**, and **#19** were invalidated after checking the implementation. Finding **#4** remains valid as a logging concern, but its original impact was overstated.

| # | Severity | Status | Category | Description | Location |
|---|----------|--------|----------|-------------|----------|
| 1 | 🔴 Critical | ✅ Confirmed | Security | JWT secret without minimum length validation | `utils/JwtUtils.java:27` |
| 2 | 🔴 Critical | ✅ Confirmed | Security | No token revocation mechanism | `utils/JwtUtils.java:57-70` |
| 18 | 🔴 Critical | ✅ Confirmed | Business Logic | Race condition in `incrementCounter` can cause constraint violations or double increments | `service/impl/RateLimitServiceImpl.java:43-52` |
| 3 | 🟠 High | ✅ Confirmed | Security | Admin password without complexity validation | `service/AdminInitializer.java:38-46` |
| 8 | 🟠 High | ✅ Confirmed | Security | No brute force protection on login | `config/SecurityConfig.java:46-52`, `service/impl/AuthServiceImpl.java:68-89` |
| 9 | 🟠 High | ✅ Confirmed | Performance | N+1 query in `getAllUserMetrics` (2N+1 queries) | `service/impl/MessageServiceImpl.java:130-149` |
| 11 | 🟠 High | ❌ Invalidated | Performance | Cache key with `LocalDate.now()` serves stale data across day boundaries | `service/impl/RateLimitServiceImpl.java:56-64` |
| 19 | 🟠 High | ❌ Invalidated | Business Logic | `checkRateLimit` not protected against race conditions on counter creation | `service/impl/RateLimitServiceImpl.java:27-37` |
| 4 | 🟡 Medium | ⚠️ Partial | Security | Error logs may expose sensitive admin initialization details | `service/AdminInitializer.java:49` |
| 5 | 🟡 Medium | ✅ Confirmed | Security | Swagger UI publicly exposed in production | `config/SecurityConfig.java:47-48` |
| 6 | 🟡 Medium | ✅ Confirmed | Security | Authentication error differentiation enables user enumeration | `security/filter/JwtAuthFilter.java:37-55` |
| 7 | 🟡 Medium | ✅ Confirmed | Security | Username logged in plaintext on failed logins | `service/impl/AuthServiceImpl.java:88` |
| 10 | 🟡 Medium | ✅ Confirmed | Performance | Manual eager loading holds connection pool during N queries | `service/impl/MessageServiceImpl.java:114-117` |
| 12 | 🟡 Medium | ✅ Confirmed | Performance | Connection pool too small (5) for production | `resources/application-prod.yml:10-16` |
| 13 | 🟡 Medium | ✅ Confirmed | Performance | `.block()` on WebClient wastes Tomcat threads | `service/platform/telegram/TelegramService.java:60-65`, `service/platform/discord/DiscordService.java:55-68` |
| 14 | 🟡 Medium | ❌ Invalidated | Entities | `equals/hashCode` fragile with non-persisted entities | `entity/Message.java:52-63`, `entity/User.java:49-60`, `entity/MessageDelivery.java:73-84`, `entity/DailyMessageCount.java:57-68` |
| 16 | 🟡 Medium | ✅ Confirmed | Entities | Missing indexes on foreign key columns | `entity/Message.java:27-29`, `entity/MessageDelivery.java:28-30` |
| 17 | 🟡 Medium | ✅ Confirmed | Entities | Missing indexes on filter columns | `repository/MessageDeliveryRepository.java:17-36` |
| 21 | 🟡 Medium | ✅ Confirmed | Business Logic | No content sanitization or platform-specific validation | `dto/request/MessageRequest.java` |
| 23 | 🟡 Medium | ✅ Confirmed | Business Logic | 500 error when platforms are not configured | `service/platform/PlatformServiceFactory.java:32-38` |
| 24 | 🟡 Medium | ✅ Confirmed | Business Logic | No validation of null destination without defaults | `dto/request/DestinationRequest.java`, `service/platform/telegram/TelegramService.java:44`, `service/platform/discord/DiscordService.java:42` |
| 15 | 🟢 Low | ✅ Confirmed | Entities | `@CreationTimestamp` without explicit fallback | `entity/DailyMessageCount.java:35-40` |
| 20 | 🟢 Low | ✅ Confirmed | Business Logic | No deduplication of destinations | `service/impl/MessageServiceImpl.java:151-159` |
| 22 | 🟢 Low | ✅ Confirmed | Business Logic | `sentAt` not set on exception-caused delivery failures | `service/impl/MessageServiceImpl.java:173-179` |

---

## Recommended Fix Priority (Validated Findings Only)

### Priority 1 — Fix immediately

1. **#1 JWT secret validation** — startup/runtime auth failure and weak operational safety.
2. **#2 Token revocation mechanism** — no targeted logout or forced invalidation for compromised tokens.
3. **#8 Brute-force protection on login** — direct account takeover risk.
4. **#18 Race condition in `incrementCounter`** — quota corruption, constraint violations, and inconsistent rate limiting.

### Priority 2 — Fix next sprint

5. **#3 Admin password policy** — protects the highest-privilege account.
6. **#5 Swagger exposure in production** — reduces reconnaissance surface.
7. **#6 Authentication error differentiation** — reduces user enumeration signal.
8. **#9 N+1 in admin metrics** — prevents scaling pain and connection pool pressure.
9. **#23 Unhandled platform misconfiguration** — avoids noisy 500 responses for operational errors.
10. **#24 Null destination validation gaps** — fails late today and should fail fast.

### Priority 3 — Hardening and performance cleanup

11. **#7 Username in failed-login logs**
12. **#10 Manual eager loading pattern**
13. **#12 Production pool sizing**
14. **#13 Blocking WebClient usage in synchronous flow**
15. **#16 Foreign-key indexes**
16. **#17 Filter/query indexes**
17. **#21 Platform-specific content validation**

### Priority 4 — Low-risk correctness/maintainability

18. **#15 `@CreationTimestamp` fallback**
19. **#20 Destination deduplication**
20. **#22 `sentAt` on exception-caused failures**

---

## Technical Implementation Plan by Batch

This section turns the validated findings into an execution plan. Each batch has an explicit priority, a technical objective, and a concrete solution for every problem included.

### Batch 1 — Priority: Critical Risk Reduction

**Why this batch first:** These issues combine direct security exposure with correctness failures in core request flow. They should be fixed before performance tuning or secondary hardening.

#### #1 — JWT secret without minimum length validation

**Explicit solution:**
- Add startup validation in `JwtProperties` or a dedicated validator bean.
- Fail fast if the configured secret is less than 32 bytes for HS256.
- Return a clear boot-time error message so misconfiguration is visible immediately.

**Technical tasks:**
- Add `@PostConstruct` validation or equivalent initialization check.
- Validate `secret != null` and `secret.getBytes(UTF_8).length >= 32`.
- Keep token creation logic unchanged; only protect configuration correctness.

#### #2 — No token revocation mechanism

**Explicit solution:**
- Introduce a `jti` claim for every generated token.
- Persist revoked token identifiers in a dedicated table.
- Reject revoked tokens in the authentication filter.
- Revoke active tokens on logout and password change.

**Technical tasks:**
- Extend `JwtUtils.generateToken()` to include UUID-based `jti`.
- Create `RevokedToken` entity/repository with `jti`, `username`, `revokedAt`, and optional `expiresAt`.
- In `JwtAuthFilter`, validate signature/expiration first and then check revocation status.
- Add application service method for logout/token revocation.
- Add cleanup strategy for expired revoked tokens.

#### #8 — No brute-force protection on login

**Explicit solution:**
- Add rate limiting for `/api/auth/login` using username and/or IP.
- Block or slow repeated failed attempts within a time window.
- Keep login error response generic.

**Technical tasks:**
- Add a login-attempt tracking service (in-memory or persistent, depending deployment needs).
- Enforce threshold such as `5 attempts / minute / username or IP`.
- On failure, increment attempt counter; on success, reset it.
- Return 429 or a domain exception mapped to a controlled response.
- Ensure logging does not reveal whether username exists.

#### #18 — Race condition in `incrementCounter`

**Explicit solution:**
- Replace the current create-then-increment split with a single atomic upsert in the database.
- If upsert is not viable immediately, catch duplicate creation race and retry increment once.

**Technical tasks:**
- Add repository method using PostgreSQL `INSERT ... ON CONFLICT ... DO UPDATE`.
- Replace `createNewCounter()` + second update path in `incrementCounter()` with one repository call.
- Keep cache eviction after the atomic increment succeeds.
- Add concurrency-focused tests to prove exactly-one increment per request.

### Batch 2 — Priority: Security Hardening and Fail-Fast Behavior

**Why this batch second:** These issues materially reduce attack surface and operational ambiguity, but they do not break quota accounting as directly as Batch 1.

#### #3 — Admin password without complexity validation

**Explicit solution:**
- Enforce minimum password policy before creating the bootstrap admin.
- Refuse startup or refuse admin creation when the configured password is weak.
- Optionally require password rotation after first login.

**Technical tasks:**
- Add password policy validator with min length and character diversity rules.
- Validate `admin.password` before `passwordEncoder.encode(...)`.
- Log a safe configuration error without printing sensitive values.
- If desired, store a `mustChangePassword` flag for the seeded admin.

#### #5 — Swagger UI publicly exposed in production

**Explicit solution:**
- Disable Swagger endpoints in production by profile, or restrict them to admin-only access.

**Technical tasks:**
- In `application-prod.yml`, disable `springdoc.swagger-ui.enabled` and optionally `springdoc.api-docs.enabled`.
- If docs must stay enabled, change security rules to `hasRole("ADMIN")` for Swagger endpoints.
- Verify no actuator/doc endpoint remains unintentionally public.

#### #6 — Authentication error differentiation enables user enumeration

**Explicit solution:**
- Normalize auth failures so invalid token, unknown user, and generic auth failure do not produce distinguishable signals to clients.

**Technical tasks:**
- Replace differentiated request attributes with one generic authentication failure code for external responses.
- Keep detailed reason only in internal debug logs if absolutely necessary.
- Review `JwtAuthenticationEntryPoint` response body to ensure it stays generic.

#### #9 — N+1 query in `getAllUserMetrics`

**Explicit solution:**
- Replace per-user counting queries with aggregate queries and merge results in memory.

**Technical tasks:**
- Add grouped query in `MessageRepository` for total messages by user.
- Add grouped query in `DailyMessageCountRepository` for today's count by user.
- Load users once, map aggregates by user ID, and build `MetricsResponse` in memory.
- Add test coverage for users with and without daily counters.

#### #23 — 500 error when platforms are not configured

**Explicit solution:**
- Map platform misconfiguration to a controlled business/operational error instead of an unhandled 500.

**Technical tasks:**
- Introduce a domain exception such as `PlatformNotConfiguredException`.
- Throw it from `PlatformServiceFactory` instead of generic `IllegalStateException`.
- Handle it in the global exception handler with stable response payload.
- Decide whether response should be 400, 422, or 503 based on API contract.

#### #24 — No validation of null destination without defaults

**Explicit solution:**
- Validate destination presence before platform call resolution.
- If destination is optional because a platform default exists, validate the fallback explicitly.

**Technical tasks:**
- Add Bean Validation and/or service-level validation for `DestinationRequest`.
- In each platform service, resolve the final destination first and reject null/blank final values.
- Return a clear validation error instead of letting downstream HTTP client fail.

### Batch 3 — Priority: Operational Hardening and Performance Stability

**Why this batch third:** These fixes improve resilience, observability, and scalability, but they are less urgent than credential, token, and quota integrity issues.

#### #7 — Username logged in plaintext on failed logins

**Explicit solution:**
- Stop logging raw usernames on failed authentication attempts.
- Replace with masked identifiers or request correlation IDs.

**Technical tasks:**
- Change failed-login warning log format.
- Optionally hash or partially mask the username.
- Keep enough metadata for incident analysis without exposing user identifiers.

#### #10 — Manual eager loading holds connection pool during N queries

**Explicit solution:**
- Replace manual `getDeliveries().size()` initialization with a repository fetch strategy.

**Technical tasks:**
- Introduce `@EntityGraph` or a dedicated fetch join query for the page use case.
- Avoid lazy initialization loops in service code.
- Measure query count before/after change.

#### #12 — Connection pool too small for production

**Explicit solution:**
- Tune Hikari pool settings for expected concurrency and deployment size.

**Technical tasks:**
- Increase `maximum-pool-size` and `minimum-idle` conservatively.
- Adjust `connection-timeout` and `idle-timeout` to deployment reality.
- Validate with load testing before increasing further.

#### #13 — `.block()` on WebClient wastes Tomcat threads

**Explicit solution:**
- Use a synchronous client for synchronous transactional flows, or fully embrace async boundaries.

**Technical tasks:**
- Prefer `RestClient` for Telegram/Discord services if the application remains servlet/JPA based.
- If keeping `WebClient`, document the blocking trade-off and tune thread pools explicitly.
- Ensure exception handling semantics remain identical after the client swap.

#### #16 — Missing indexes on foreign key columns

**Explicit solution:**
- Create explicit database indexes for key relationship columns used in joins and filters.

**Technical tasks:**
- Add migration scripts for `messages.user_id` and `message_deliveries.message_id`.
- Verify query plans after migration.
- Prefer migrations over ORM-only annotations for production reliability.

#### #17 — Missing indexes on filter columns

**Explicit solution:**
- Add indexes matching actual repository filtering patterns.

**Technical tasks:**
- Review `MessageDeliveryRepository` predicates.
- Create composite indexes aligned with status/platform/date filters used together.
- Validate that new indexes help real queries and do not over-index the table.

#### #21 — No content sanitization or platform-specific validation

**Explicit solution:**
- Validate outbound message content against each platform's constraints before dispatch.

**Technical tasks:**
- Add size and shape constraints to request DTO/service validation.
- Centralize platform-specific rules (e.g., Discord max length, Telegram destination format).
- Reject invalid content before persistence/external calls.

### Batch 4 — Priority: Low-Risk Correctness and Maintainability

**Why this batch last:** These are worthwhile fixes, but they do not materially change the threat model or the main scaling bottlenecks.

#### #15 — `@CreationTimestamp` without explicit fallback

**Explicit solution:**
- Add `@PrePersist` fallback for defensive consistency.

**Technical tasks:**
- Mirror the `Message` entity pattern in `DailyMessageCount`.
- Keep `@CreationTimestamp` if desired, but do not rely on it as the only guard.

#### #20 — No deduplication of destinations

**Explicit solution:**
- Reject or deduplicate repeated destinations before sending.

**Technical tasks:**
- Define destination equality semantics (`platform + destination`).
- Either normalize with `.distinct()` or fail validation with an explicit message.
- Ensure rate-limit consumption matches deduplicated deliveries.

#### #22 — `sentAt` not set on exception-caused delivery failures

**Explicit solution:**
- Set failure timestamp consistently even when the delivery attempt ends via exception path.

**Technical tasks:**
- Add `sentAt(LocalDateTime.now())` when building failed `MessageDelivery` in catch blocks.
- Keep failure metadata aligned with success-path persistence model.

### Cross-Batch Delivery Notes

- Prefer **small PRs per batch** instead of one giant security/performance refactor.
- Add **tests with every batch**, especially for authentication and rate-limiting behavior.
- Re-run the audit after Batches 1 and 2 before starting performance cleanup.
- Do **not** spend time implementing fixes for invalidated findings **#11**, **#14**, and **#19**.

---

## 🔴 Critical Findings

### #1 — JWT Secret Without Minimum Length Validation

**File:** `src/main/java/com/notificationhub/utils/JwtUtils.java` — Line 27

```java
private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
}
```

**Problem:** `Keys.hmacShaKeyFor()` requires a minimum of 256 bits (32 bytes) for HS256. If `JWT_SECRET` in `.env` is shorter, it throws `WeakKeyException` at runtime when generating or validating a token. There is no validation in `JwtProperties` or any `@PostConstruct` method to verify this at startup.

**Related file:** `src/main/java/com/notificationhub/config/JwtProperties.java` — only performs `@Value` injection without validation.

**Impact:** The application starts successfully but crashes silently when attempting to use JWT. In production, this causes unhandled exceptions during authentication.

**Recommendation:** Validate the secret length at startup:

```java
@PostConstruct
public void validate() {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
        throw new IllegalStateException("JWT secret must be at least 32 bytes (256 bits)");
    }
}
```

---

### #2 — No Token Revocation Mechanism

**File:** `src/main/java/com/notificationhub/utils/JwtUtils.java` — Lines 57-70

```java
public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    // ... generates token with username + roles
    return createToken(claims, userDetails.getUsername());
}

private String createToken(Map<String, Object> claims, String subject) {
    return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
}
```

**Problem:** The token is generated with `issuedAt`, `expiration`, `subject`, and `roles`, but does not include a `jti` (JWT ID) claim. There is no denylist/allowlist table or mechanism to invalidate tokens before their 24-hour expiration.

**Impact:** If a token is compromised (leaked logs, network interception, XSS), it remains valid for the full 24 hours. There is no way to force logout or revoke access for a specific user without changing the JWT secret (which invalidates ALL users).

**Recommendation:**
- Add a `jti` (UUID) claim to generated tokens
- Create a `revoked_tokens` table with `(jti, username, revoked_at)`
- Check the denylist in `JwtAuthFilter.doFilterInternal()` after validation
- Invalidate tokens on logout and password change

---

### #18 — Race Condition in `incrementCounter`

**File:** `src/main/java/com/notificationhub/service/impl/RateLimitServiceImpl.java` — Lines 43-52

```java
@CacheEvict(value = "rateLimits", key = "#user.id + '_' + T(java.time.LocalDate).now()")
public void incrementCounter(User user) {
    LocalDate today = LocalDate.now();

    int rowsUpdated = dailyMessageCountRepository.incrementCountAtomic(user, today);

    if (rowsUpdated == 0) {
        createNewCounter(user, today);                           // Thread A creates
        dailyMessageCountRepository.incrementCountAtomic(user, today);  // Thread B also creates → constraint violation
    }
}
```

**Problem:** Two concurrent requests can both receive `rowsUpdated == 0` simultaneously. Both enter the `if` block and both call `createNewCounter()` — the second fails with `DataIntegrityViolationException` due to the `unique(user_id, date)` constraint. Or worse: both execute `incrementCountAtomic` after creation — the counter gets incremented twice instead of once.

**Impact:**
- **Constraint violation:** Unhandled exception causes 500 error and message not counted
- **Double increment:** User's daily count increases by 2 for a single message, prematurely exhausting their quota
- **Lost increment:** If one thread's create fails silently, the other's increment may apply to the wrong counter state

**Recommendation:** Use an upsert query or handle the race condition:

```java
if (rowsUpdated == 0) {
    try {
        createNewCounter(user, today);
    } catch (DataIntegrityViolationException e) {
        // Another thread created it, safe to proceed
    }
    dailyMessageCountRepository.incrementCountAtomic(user, today);
}
```

Or better, a single PostgreSQL upsert:

```java
@Modifying
@Query(value = "INSERT INTO daily_message_counts (user_id, date, count, created_at, updated_at) " +
               "VALUES (:userId, :date, 1, NOW(), NOW()) " +
               "ON CONFLICT (user_id, date) DO UPDATE SET count = count + 1, updated_at = NOW()",
       nativeQuery = true)
void upsertAndIncrement(@Param("userId") Long userId, @Param("date") LocalDate date);
```

---

## 🟠 High Findings

### #3 — Admin Password Without Complexity Validation

**File:** `src/main/java/com/notificationhub/service/AdminInitializer.java` — Lines 38-46

```java
private void initializeAdminUser() {
    try {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = User.builder()
                    .username(adminUsername)
                    .passwordHash(passwordEncoder.encode(adminPassword))  // ← encodes whatever is provided
                    .role(Role.ADMIN)
                    .dailyMessageLimit(1000)
                    .build();

            userRepository.save(admin);
            log.info("Usuario admin creado exitosamente");
        }
    } catch (Exception e) {
        log.error("Error creando usuario admin: {}", e.getMessage());
    }
}
```

**Problem:** If `ADMIN_PASSWORD=admin` in `.env`, it gets created as-is. There is no validation of minimum length, complexity requirements, or forced change on first login.

**Impact:** The most privileged account in the system can be secured with a trivially guessable password, granting full admin access to all messages, user metrics, and system data.

**Recommendation:**
- Enforce minimum password complexity (length >= 12, mix of character types)
- Force password change on first admin login
- Log a warning if the configured password is weak (fail-safe)

---

### #8 — No Brute Force Protection on Login

**File:** `src/main/java/com/notificationhub/config/SecurityConfig.java` — Lines 46-52

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        // ...
)
```

**File:** `src/main/java/com/notificationhub/service/impl/AuthServiceImpl.java` — Lines 68-89

```java
public AuthResponse login(LoginRequest request) {
    try {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        // ...
    } catch (Exception e) {
        throw new InvalidCredentialsException("Invalid username or password");
    }
}
```

**Problem:** `/api/auth/login` is `permitAll` with no rate limiting, CAPTCHA, account lockout, or IP-based throttling. An attacker can send thousands of credential attempts per second.

**Impact:** Successful brute force or credential stuffing attacks against user accounts. Given that JWT tokens last 24 hours, a single compromised account provides extended access.

**Recommendation:**
- Implement per-IP or per-username rate limiting on the login endpoint (e.g., 5 attempts/minute)
- Use Spring Security's `AccountLockoutManager` or a custom counter
- Consider exponential backoff after N failed attempts

---

### #9 — N+1 Query in `getAllUserMetrics`

**File:** `src/main/java/com/notificationhub/service/impl/MessageServiceImpl.java` — Lines 130-149

```java
public List<MetricsResponse> getAllUserMetrics() {
    if (!securityUtils.isAdmin()) {
        throw new IllegalStateException("Only admins can view metrics");
    }

    List<User> users = userRepository.findAll();  // ← 1 query
    LocalDate today = LocalDate.now();

    return users.stream()
            .map(user -> {
                long totalMessages = messageRepository.countByUser(user);       // ← 1 query per user
                DailyMessageCount todayCount = dailyMessageCountRepository
                        .findByUserAndDate(user, today);                         // ← 1 query per user

                int messagesSentToday = todayCount != null ? todayCount.getCount() : 0;
                // ...
            })
            .collect(Collectors.toList());
}
```

**Problem:** For N users, this executes **2N + 1 queries** sequentially. With 100 users = 201 database round-trips.

**Impact:** The admin metrics endpoint becomes progressively slower as the user base grows. Under concurrent admin requests, this can saturate the connection pool.

**Recommendation:** Use aggregate queries:

```java
// In MessageRepository
@Query("SELECT m.user.id, COUNT(m) FROM Message m GROUP BY m.user.id")
Map<Long, Long> countAllByUserId();

// In DailyMessageCountRepository
@Query("SELECT d.user.id, d.count FROM DailyMessageCount d WHERE d.date = :date")
Map<Long, Integer> findCountsByDate(@Param("date") LocalDate date);

// Then merge in memory — 3 queries total regardless of user count
```

---

### #11 — Cache Key with `LocalDate.now()` Serves Stale Data *(Invalidated after validation)*

**File:** `src/main/java/com/notificationhub/service/impl/RateLimitServiceImpl.java` — Lines 56-64

```java
@Cacheable(value = "rateLimits", key = "#user.id + '_' + T(java.time.LocalDate).now()")
public int getRemainingMessages(User user) {
    LocalDate today = LocalDate.now();

    DailyMessageCount count = dailyMessageCountRepository
            .findByUserAndDate(user, today)
            .orElseGet(() -> createNewCounter(user, today));

    return count.getRemainingMessages(user.getDailyMessageLimit());
}
```

**Validation Result:** This finding is **not correct**. In Spring Cache, the SpEL expression used in the `key` attribute is evaluated **for each method invocation**. After midnight, `T(java.time.LocalDate).now()` produces the new date, causing a cache miss for the previous day's key and forcing a fresh lookup.

**Actual Impact:** No stale-data issue was confirmed from the key expression itself.

**Recommendation:** No fix is required for this specific claim. Cache configuration can still be revisited separately for TTL/observability reasons, but not because the date-based key is stale.

---

### #19 — `checkRateLimit` Not Protected Against Race Conditions *(Invalidated after validation)*

**File:** `src/main/java/com/notificationhub/service/impl/RateLimitServiceImpl.java` — Lines 27-37

```java
public void checkRateLimit(User user) {
    LocalDate today = LocalDate.now();

    DailyMessageCount count = dailyMessageCountRepository
            .findByUserAndDate(user, today)               // Has @Lock(PESSIMISTIC_WRITE) ✓
            .orElseGet(() -> createNewCounter(user, today));  // ← No lock when creating ✗

    if (count.hasReachedLimit(user.getDailyMessageLimit())) {
        throw new RateLimitExceededException(...);
    }
}
```

**Validation Result:** This finding is **not correct for the existing-row case**, which is the dangerous rate-limit boundary scenario. `findByUserAndDate(...)` uses `@Lock(PESSIMISTIC_WRITE)`, and `RateLimitServiceImpl` is transactional at class level. When a row already exists, concurrent requests serialize on the locked row, so the second request observes the updated count after the first transaction completes.

**Actual Impact:** The report's described "99 -> 101" scenario was not reproducible from the current implementation. The real concurrency bug is already captured in **finding #18 (`incrementCounter`)**.

**Recommendation:** No separate fix is required for this finding. If the rate-limit flow is refactored, align counter creation/increment with the same atomic strategy recommended in **#18**.

---

## 🟡 Medium Findings

### #4 — Admin Credentials May Leak in Error Logs

**File:** `src/main/java/com/notificationhub/service/AdminInitializer.java` — Line 49

```java
} catch (Exception e) {
    log.error("Error creando usuario admin: {}", e.getMessage());
}
```

**Problem:** Logging `e.getMessage()` during admin initialization can expose internal validation or persistence details. The most realistic leak here is the **username** or low-level persistence details, not necessarily the raw password or the stored hash.

**Impact:** If logs are accessible through CloudWatch, files, or a log aggregator, they may reveal sensitive operational details that help troubleshooting but also aid reconnaissance.

**Recommendation:**
```java
} catch (Exception e) {
    log.error("Error creating admin user. Verify configuration.");
}
```

---

### #5 — Swagger UI Publicly Exposed in Production

**File:** `src/main/java/com/notificationhub/config/SecurityConfig.java` — Lines 47-48

```java
.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
```

**Problem:** Anyone can access `/swagger-ui/index.html` in production and view the complete API documentation: all endpoints, request/response schemas, JWT token structure, example payloads, error formats, etc. This facilitates reconnaissance for attackers.

**Impact:** An attacker can discover:
- All available endpoints and their parameters
- Expected request/response formats
- Authentication mechanisms
- Error response structures (useful for crafting attacks)

**Recommendation:**
- Disable Swagger in production via profile: `springdoc.swagger-ui.enabled=false` in `application-prod.yml`
- Or restrict to admin role only:
  ```java
  .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").hasRole("ADMIN")
  ```

---

### #6 — Authentication Error Differentiation Enables User Enumeration

**File:** `src/main/java/com/notificationhub/security/filter/JwtAuthFilter.java` — Lines 37-55

```java
if (!jwtUtils.validateToken(jwt)) {
    request.setAttribute(AUTH_ERROR_ATTR, "INVALID_TOKEN");
} else {
    String username = jwtUtils.extractUsername(jwt);
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    // ...
} catch (UsernameNotFoundException e) {
    request.setAttribute(AUTH_ERROR_ATTR, "USER_NOT_FOUND");
} catch (Exception e) {
    request.setAttribute(AUTH_ERROR_ATTR, "AUTH_FAILED");
}
```

**Problem:** Three distinct error attributes are set: `INVALID_TOKEN`, `USER_NOT_FOUND`, `AUTH_FAILED`. If `JwtAuthenticationEntryPoint` exposes these values in the HTTP response, an attacker can confirm whether a username exists (`USER_NOT_FOUND` means the token is valid but the user was deleted).

**Recommendation:** Unify all auth errors to a single generic message. Check the entry point handler to ensure no differentiation is exposed.

---

### #7 — Username Logged in Plaintext on Failed Logins

**File:** `src/main/java/com/notificationhub/service/impl/AuthServiceImpl.java` — Line 88

```java
} catch (Exception e) {
    log.warn("Login failed for user: {} - {}", request.getUsername(), e.getMessage());
    throw new InvalidCredentialsException("Invalid username or password");
}
```

**Problem:** The username in plaintext is logged for every failed login attempt. If logs are accessible, an attacker (or insider) can reconstruct which usernames exist in the system and how many attempts were made against each.

**Impact:** Facilitates user enumeration and brute force detection evasion (attacker knows when they've hit a valid username).

**Recommendation:**
```java
log.warn("Login failed for user");  // No username, no exception details
```
Or hash the username:
```java
log.warn("Login failed for user hash: {}", DigestUtils.sha256Hex(request.getUsername()));
```

---

### #10 — Manual Eager Loading Holds Connection Pool

**File:** `src/main/java/com/notificationhub/service/impl/MessageServiceImpl.java` — Lines 114-117

```java
Page<Message> messages = messageDeliveryRepository.findMessagesByFilters(criteria, pageable);

messages.getContent().forEach(message -> {
    message.getDeliveries().size();  // ← Triggers N additional SELECTs
});
```

**Problem:** For each message in the page (e.g., 20), a `SELECT * FROM message_deliveries WHERE message_id = ?` is executed. This happens inside the `@Transactional` method, holding a single Hibernate connection for the duration of all N queries. If page size is 100, that's 100 sequential SELECTs blocking one connection.

**Impact:** Under load, this pattern can exhaust the connection pool (already small at 5 connections), causing other requests to timeout waiting for an available connection.

**Recommendation:** Use `@BatchSize` on the relationship:
```java
@OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
@BatchSize(size = 20)
private List<MessageDelivery> deliveries = new ArrayList<>();
```

Or use a `@EntityGraph` with a separate non-paginated query for the current page's IDs.

---

### #12 — Connection Pool Too Small for Production

**File:** `src/main/resources/application-prod.yml` — Lines 10-16

```yaml
hikari:
  maximum-pool-size: 5
  minimum-idle: 2
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
  leak-detection-threshold: 60000
```

**Problem:** With only 5 maximum connections, if 6 concurrent requests hit `/api/messages/send` (each consuming ~3-4 connections for rate limit check + save + increment + cache evict), the 7th request will wait 30 seconds (`connection-timeout`) and then fail.

**Impact:** Under moderate concurrent load, requests fail with connection timeout errors even though the database can handle more connections.

**Recommendation:** For Railway (typically 1-2 instances):
```yaml
hikari:
  maximum-pool-size: 10
  minimum-idle: 5
  connection-timeout: 20000
  idle-timeout: 300000
  max-lifetime: 1200000
  leak-detection-threshold: 60000
```

---

### #13 — `.block()` on WebClient Wastes Tomcat Threads

**File:** `src/main/java/com/notificationhub/service/platform/telegram/TelegramService.java` — Lines 60-65

```java
var response = webClient.post()
        .uri("/sendMessage")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(Map.class)
        .onErrorResume(e -> { ... })
        .block();  // ← Blocks the Tomcat thread
```

**File:** `src/main/java/com/notificationhub/service/platform/discord/DiscordService.java` — Lines 55-68

```java
webClient.post()
        .uri(webhookUrl)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestBody)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> { ... })
        .toBodilessEntity()
        .doOnSuccess(response -> { ... })
        .block();  // ← Blocks the Tomcat thread
```

**Problem:** `WebClient` (reactive) is injected but `.block()` converts the async call into a synchronous blocking call. Each message send holds a Tomcat worker thread idle while waiting for the external API response (Telegram/Discord latency ~100ms-2s).

**Impact:** Wastes thread pool resources. With 200 default Tomcat threads and 2 platform calls per message, concurrent users can exhaust threads waiting for external API responses.

**Recommendation:** Since JPA transactions are inherently synchronous, either:
- Use `RestClient` (Spring Boot 3.2+) for synchronous calls — cleaner API, no reactive overhead
- Or keep WebClient but document the trade-off and tune Tomcat's `max-threads` accordingly

---

### #14 — `equals/hashCode` Fragile with Non-Persisted Entities *(Invalidated after validation)*

**File:** `src/main/java/com/notificationhub/entity/Message.java` — Lines 52-63

```java
@Override
public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
    Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) return false;
    Message message = (Message) o;
    return getId() != null && Objects.equals(getId(), message.getId());  // ← If id is null, always returns false
}
```

**Validation Result:** This finding is **not correct**. The method starts with `if (this == o) return true;`, so identity comparison for the same in-memory object is already handled correctly before any ID-based comparison occurs.

**Same issue in:** `User.java:49-60`, `MessageDelivery.java:73-84`, `DailyMessageCount.java:57-68`.

**Actual Impact:** No concrete bug was confirmed from the current implementation. The pattern is a common Hibernate-safe equality strategy for entities identified by database ID.

**Recommendation:** No fix is required for this claim.

---

### #16 — Missing Indexes on Foreign Key Columns

**File:** `src/main/java/com/notificationhub/entity/Message.java` — Lines 27-29

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
@ToString.Exclude
private User user;
```

**File:** `src/main/java/com/notificationhub/entity/MessageDelivery.java` — Lines 28-30

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "message_id", nullable = false)
@ToString.Exclude
private Message message;
```

**Problem:** JPA/Hibernate does **not automatically create indexes** on foreign key columns in all databases. PostgreSQL creates them when defining the FK constraint, but this is not guaranteed across all database engines or migration tools. Without indexes, queries filtering by `user_id` or `message_id` perform full table scans.

**Impact:** As the tables grow, queries like "find all messages by user" or "find all deliveries for a message" become increasingly slow.

**Recommendation:** Use database migrations (Flyway/Liquibase) to explicitly create indexes:
```sql
CREATE INDEX idx_messages_user_id ON messages(user_id);
CREATE INDEX idx_message_deliveries_message_id ON message_deliveries(message_id);
```

Or add Hibernate annotations:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_message_user"))
@org.hibernate.annotations.Index(name = "idx_message_user_id")
private User user;
```

---

### #17 — Missing Indexes on Filter Columns

**File:** `src/main/java/com/notificationhub/repository/MessageDeliveryRepository.java` — Lines 17-36

```java
@Query("""
        SELECT DISTINCT m FROM Message m
        JOIN m.deliveries md
        WHERE (:#{#criteria.user()} IS NULL OR m.user = :#{#criteria.user()})
        AND (:#{#criteria.from()} IS NULL OR m.createdAt >= :#{#criteria.from()})
        AND (:#{#criteria.to()} IS NULL OR m.createdAt <= :#{#criteria.to()})
        AND (:#{#criteria.platform()} IS NULL OR md.platformType = :#{#criteria.platform()})
        AND (:#{#criteria.status()} IS NULL OR md.status = :#{#criteria.status()})
        ORDER BY m.createdAt DESC
        """)
Page<Message> findMessagesByFilters(MessageFilterCriteria criteria, Pageable pageable);
```

**Problem:** This query filters by `md.platformType`, `md.status`, `m.createdAt`, and orders by `m.createdAt DESC`. Without composite indexes on these columns, paginated queries with filters will perform full table scans and filesort operations.

**Impact:** As `message_deliveries` and `messages` tables grow to millions of rows, the filtered/paginated endpoint becomes increasingly slow.

**Recommendation:**
```sql
CREATE INDEX idx_md_platform_status ON message_deliveries(platform_type, status);
CREATE INDEX idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX idx_md_message_id ON message_deliveries(message_id);
```

---

### #21 — No Content Sanitization or Platform-Specific Validation

**File:** `src/main/java/com/notificationhub/dto/request/MessageRequest.java`

```java
@NotBlank
@Size(max = 4000)
private String content;
```

**Problem:** Only validates non-empty and maximum 4000 characters. Does not validate:
- Content is not just whitespace or special characters (partially covered by `@NotBlank`)
- Does not contain malicious URLs (SSRF via content that platforms may interpret as commands)
- Platform-specific limits: Discord has a 2000 character limit, Telegram 4096 — the 4000 limit exceeds Discord's maximum

**Impact:** Messages may fail on Discord with content between 2001-4000 characters, causing unnecessary delivery failures that still consume the rate limit (if at least one other platform succeeds).

**Recommendation:**
- Add platform-specific content validation before sending
- Consider sanitizing or limiting special character sequences
- Document per-platform limits in Swagger

---

### #23 — 500 Error When Platforms Are Not Configured

**File:** `src/main/java/com/notificationhub/service/platform/PlatformServiceFactory.java` — Lines 32-38

```java
public PlatformService getService(PlatformType platformType) {
    PlatformService service = services.get(platformType);

    if (service == null) {
        throw new IllegalArgumentException("Platform not supported: " + platformType);
    }

    if (!service.isConfigured()) {
        throw new IllegalStateException("Platform not configured: " + platformType);  // ← Not handled by GlobalExceptionHandler
    }

    return service;
}
```

**Problem:** `IllegalStateException` is not handled in `GlobalExceptionHandler`. It falls through to the generic `Exception.class` handler which returns 500 "An unexpected error occurred" — misleading, as this is a configuration issue that should return 400.

**Impact:** Clients receive a 500 Internal Server Error when they should receive a 400 Bad Request with a clear "Platform X is not configured, please set Y environment variable" message.

**Recommendation:** Add to `GlobalExceptionHandler.java`:
```java
@ExceptionHandler(IllegalStateException.class)
public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    BAD_REQUEST_ERROR,
                    ex.getMessage(),
                    null));
}
```

---

### #24 — No Validation of Null Destination Without Defaults

**File:** `src/main/java/com/notificationhub/dto/request/DestinationRequest.java`

```java
private String destination;  // ← No @NotBlank, no validation
```

**File:** `src/main/java/com/notificationhub/service/platform/telegram/TelegramService.java` — Line 44

```java
String chatId = (destination != null && !destination.isEmpty()) ? destination : defaultChatId;
```

**File:** `src/main/java/com/notificationhub/service/platform/discord/DiscordService.java` — Line 42

```java
String finalDestination = (destination != null && !destination.isEmpty()) ? destination : webhookUrl;
```

**Problem:** If `destination` is `null` and the defaults (`defaultChatId` or `webhookUrl`) are not configured (empty in `.env`), the API call to the external platform fails with a confusing error instead of validating upfront.

**Impact:** Users receive obscure API errors (e.g., Telegram 400 Bad Request) instead of a clear "No destination configured, please provide one or configure defaults."

**Recommendation:** Validate at the service level before making the API call:
```java
String chatId = (destination != null && !destination.isEmpty()) ? destination : defaultChatId;
if (chatId == null || chatId.isEmpty()) {
    throw new IllegalArgumentException("No destination provided and no default chat ID configured for Telegram");
}
```

---

## 🟢 Low Findings

### #15 — `@CreationTimestamp` Without Explicit Fallback

**File:** `src/main/java/com/notificationhub/entity/DailyMessageCount.java` — Lines 35-40

```java
@CreationTimestamp
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
```

**Problem:** `@CreationTimestamp` works in tests with H2, but it depends on Hibernate's interceptor configuration. Unlike `Message.java` which has an explicit `@PrePersist` fallback (lines 44-48), `DailyMessageCount` relies solely on the Hibernate annotation.

**Impact:** In edge cases (manual entity construction in tests, specific Hibernate configurations), `createdAt` may remain `null` and fail the `nullable = false` constraint.

**Recommendation:** Add a `@PrePersist` method as a safety net, similar to `Message`:
```java
@PrePersist
protected void onCreate() {
    if (createdAt == null) {
        createdAt = LocalDateTime.now();
    }
}
```

---

### #20 — No Deduplication of Destinations

**File:** `src/main/java/com/notificationhub/service/impl/MessageServiceImpl.java` — Lines 151-159

```java
private List<MessageDelivery> processMessageDeliveries(MessageRequest request, Message message) {
    List<MessageDelivery> deliveries = new ArrayList<>();

    for (DestinationRequest destination : request.getDestinations()) {
        MessageDelivery delivery = processSingleDelivery(destination, message);
        deliveries.add(delivery);
    }

    return deliveries;
}
```

**Problem:** If a user sends `destinations: [{platform: TELEGRAM, destination: "123"}, {platform: TELEGRAM, destination: "123"}]`, the same message is sent twice to the same chat without any validation or warning.

**Impact:** Accidental spam/duplicate messages. Each duplicate also consumes the rate limit.

**Recommendation:** Deduplicate before processing:
```java
List<DestinationRequest> uniqueDestinations = request.getDestinations().stream()
        .distinct()
        .toList();
```

Or validate uniqueness and reject with a clear error.

---

### #22 — `sentAt` Not Set on Exception-Caused Delivery Failures

**File:** `src/main/java/com/notificationhub/service/impl/MessageServiceImpl.java` — Lines 173-179

```java
} catch (Exception e) {
    log.error("Failed to send message to {}: {}", destination.getPlatform(), e.getMessage());
    return MessageDelivery.builder()
            .platformType(destination.getPlatform())
            .destination(destination.getDestination())
            .status(DeliveryStatus.FAILED)
            .errorMessage("Exception: " + e.getMessage())
            .build();
    // ← sentAt is not set
}
```

**Problem:** Unlike `markAsFailed()` in `MessageDelivery.java` (line 69) which does set `sentAt = LocalDateTime.now()`, this builder path does not include `sentAt`. Failed deliveries caused by exceptions have no timestamp indicating when the failure occurred.

**Impact:** Inconsistent data — some failed deliveries have `sentAt`, others don't. This complicates auditing and debugging delivery timelines.

**Recommendation:** Add `.sentAt(LocalDateTime.now())` to the builder:
```java
return MessageDelivery.builder()
        .platformType(destination.getPlatform())
        .destination(destination.getDestination())
        .status(DeliveryStatus.FAILED)
        .errorMessage("Exception: " + e.getMessage())
        .sentAt(LocalDateTime.now())
        .build();
```

---

## Appendix: File Reference Map

| File | Finding #s |
|------|-----------|
| `utils/JwtUtils.java` | 1, 2 |
| `config/JwtProperties.java` | 1 |
| `config/SecurityConfig.java` | 5, 8 |
| `service/AdminInitializer.java` | 3, 4 |
| `service/impl/AuthServiceImpl.java` | 7, 8 |
| `service/impl/MessageServiceImpl.java` | 9, 10, 20, 22 |
| `service/impl/RateLimitServiceImpl.java` | 11, 18, 19 |
| `security/filter/JwtAuthFilter.java` | 6 |
| `service/platform/PlatformServiceFactory.java` | 23 |
| `service/platform/telegram/TelegramService.java` | 13, 24 |
| `service/platform/discord/DiscordService.java` | 13, 24 |
| `repository/MessageDeliveryRepository.java` | 17 |
| `repository/DailyMessageCountRepository.java` | 19 |
| `entity/Message.java` | 14, 16 |
| `entity/MessageDelivery.java` | 14 |
| `entity/User.java` | 14 |
| `entity/DailyMessageCount.java` | 14, 15 |
| `dto/request/MessageRequest.java` | 21 |
| `dto/request/DestinationRequest.java` | 24 |
| `resources/application-prod.yml` | 12 |
