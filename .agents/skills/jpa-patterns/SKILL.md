---
name: jpa-patterns
description: JPA/Hibernate patterns and pitfalls for Spring Boot — entity design, auditing, N+1 prevention, fetch strategies, transactions, projections, bulk ops, optimistic locking, connection pooling, migrations, and testing. Use when user has JPA performance issues, LazyInitializationException, asks about entity relationships, fetch strategies, or is designing the data layer from scratch.
---

# JPA/Hibernate Patterns

## When to Activate

- Designing entities, relationships, or table mappings
- N+1 problem, `LazyInitializationException`, or EAGER/LAZY questions
- Transaction management, propagation, or self-invocation issues
- Query optimization: projections, bulk ops, cursor pagination
- Setting up HikariCP, Flyway/Liquibase, or second-level cache
- Writing `@DataJpaTest` with Testcontainers

---

## Quick Reference

| Problem                     | Symptom                   | Solution                                       |
| --------------------------- | ------------------------- | ---------------------------------------------- |
| N+1 queries                 | Many SELECTs in logs      | `JOIN FETCH`, `@EntityGraph`, `@BatchSize`     |
| LazyInitializationException | Error outside transaction | `JOIN FETCH`, `@Transactional`, DTO projection |
| Dirty checking overhead     | Slow reads                | `readOnly = true`, DTOs                        |
| Lost updates                | Concurrent modifications  | `@Version`                                     |
| `@Transactional` ignored    | No rollback, no tx        | Self-invocation bug — inject self              |

---

## Entity Design

```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_email", columnList = "customerEmail")
})
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version private Long version;           // optimistic locking
    @Enumerated(EnumType.STRING) private OrderStatus status;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // Always maintain both sides of bidirectional relationships
    public void addItem(OrderItem item) { items.add(item); item.setOrder(this); }
    public void removeItem(OrderItem item) { items.remove(item); item.setOrder(null); }

    @Override public String toString() {
        return "Order{id=" + id + "}"; // NEVER include lazy fields here
    }
}
```

Enable auditing: `@Configuration @EnableJpaAuditing class JpaConfig {}`

**equals/hashCode** — use business key, not ID:

```java
@NaturalId @Column(unique = true, nullable = false) private String code;

@Override public boolean equals(Object o) {
    if (!(o instanceof Order order)) return false;
    return code != null && code.equals(order.code);
}
@Override public int hashCode() { return Objects.hash(code); }
```

---

## N+1 Prevention

> Always verify with SQL logs before shipping.

**JOIN FETCH** — single query:

```java
@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Optional<Order> findByIdWithItems(@Param("id") Long id);
```

**@EntityGraph** — declarative, works with derived queries:

```java
@EntityGraph(attributePaths = {"items", "items.product"})
List<Order> findByStatus(OrderStatus status);
```

**@BatchSize** — best when JOIN FETCH causes cartesian explosion:

```java
@OneToMany(mappedBy = "order")
@BatchSize(size = 25)
private List<OrderItem> items;
// or globally: spring.jpa.properties.hibernate.default_batch_fetch_size=25
```

**Detect N+1:**

```yaml
logging.level.org.hibernate.SQL: DEBUG
logging.level.org.hibernate.orm.jdbc.bind: TRACE
```

---

## Lazy Loading

Default to `LAZY` everywhere — `@ManyToOne` defaults to EAGER, override it:

```java
@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id")
private Order order;
```

Set `spring.jpa.open-in-view=false` — it masks N+1 problems.

**LazyInitializationException** — three options in order of preference:

```java
// 1. DTO projection (best — fetch only what you need)
@Query("SELECT o.id as id, SIZE(o.items) as itemCount FROM Order o WHERE o.id = :id")
Optional<OrderSummary> findSummary(@Param("id") Long id);

// 2. JOIN FETCH in specific query
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Optional<Order> findWithItems(@Param("id") Long id);

// 3. @Transactional on service method (access within tx boundary)
@Transactional(readOnly = true)
public OrderDTO getOrder(Long id) {
    Order order = repo.findById(id).orElseThrow();
    return OrderDTO.from(order);
}
```

---

## Transactions

```java
@Transactional(readOnly = true)           // disables dirty checking
public Order findById(Long id) { ... }

@Transactional                            // full tx
public Order create(CreateOrderRequest r) { ... }

@Transactional(rollbackFor = Exception.class)  // also rolls back checked exceptions
public void processPayment(Long id) throws PaymentException { ... }
```

**Propagation:**

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)  // independent tx
public void chargeCard(Order o) { ... }

@Transactional(propagation = Propagation.MANDATORY)     // throws if no tx exists
public void updateLedger(Order o) { ... }
```

**Self-invocation anti-pattern** — `@Transactional` is silently ignored:

```java
// ❌ Spring proxy bypassed — no transaction
public void process(Long id) { updateStatus(id); }
@Transactional public void updateStatus(Long id) { ... }

// ✅ Inject self or extract to a separate service
@Autowired private OrderService self;
public void process(Long id) { self.updateStatus(id); }
```

---

## Query Patterns

**DTO Projections:**

```java
public interface OrderSummary { Long getId(); String getCustomerEmail(); }
Page<OrderSummary> findAllBy(Pageable pageable);

public record OrderDTO(Long id, String email) {}
@Query("SELECT new com.example.OrderDTO(o.id, o.customerEmail) FROM Order o WHERE o.status = :s")
List<OrderDTO> findDTOs(@Param("s") OrderStatus s);
```

**Pagination:**

```java
Page<OrderSummary> result = repo.findAllBy(PageRequest.of(page, 20, Sort.by("createdAt").descending()));

// Cursor-based (better at large offsets)
@Query("SELECT o FROM Order o WHERE o.id > :lastId ORDER BY o.id ASC")
List<Order> findNextPage(@Param("lastId") Long lastId, Pageable p);
```

**Bulk operations** — never load entities just to update/delete:

```java
@Modifying
@Query("UPDATE Order o SET o.status = :s WHERE o.createdAt < :date")
int archiveOld(@Param("s") OrderStatus s, @Param("date") Instant date);

// Bulk inserts
repo.saveAll(items);
// spring.jpa.properties.hibernate.jdbc.batch_size=50
// spring.jpa.properties.hibernate.order_inserts=true
```

---

## Optimistic Locking

```java
// @Version already on entity (see Entity Design section)
// Thread 1: load (v=1) → save → v=2
// Thread 2: load (v=1) → save → OptimisticLockException!

@Retryable(value = OptimisticLockException.class, maxAttempts = 3)
@Transactional
public Order updateStatus(Long id, OrderStatus s) {
    Order o = repo.findById(id).orElseThrow();
    o.setStatus(s);
    return repo.save(o);
}
```

---

## ManyToMany

Use `Set` (not `List`) to avoid duplicates in the join table:

```java
@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
@JoinTable(name = "student_course",
    joinColumns = @JoinColumn(name = "student_id"),
    inverseJoinColumns = @JoinColumn(name = "course_id"))
private Set<Course> courses = new HashSet<>();

public void enroll(Course c) { courses.add(c); c.getStudents().add(this); }
```

---

## Common Mistakes

```java
// ❌ CascadeType.ALL on @ManyToOne — deleting a child can delete the parent
@ManyToOne(cascade = CascadeType.ALL) private Author author;
// ✅ Cascade only parent → child
@OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Book> books;
```

---

## Infrastructure

**HikariCP:**

```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true  # PostgreSQL
```

**Cache:** First-level cache is per EntityManager — never share entities across transactions. Second-level cache (Ehcache/Redis) only for read-heavy, rarely-updated entities; define eviction strategy upfront.

**Migrations:**

```properties
spring.jpa.hibernate.ddl-auto=validate  # never update/create in production
```

Use Flyway or Liquibase. Keep scripts additive and idempotent — never drop columns without a migration plan.

---

## Testing

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class OrderRepositoryTest {

    @Container
    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", db::getJdbcUrl);
        r.add("spring.datasource.username", db::getUsername);
        r.add("spring.datasource.password", db::getPassword);
    }

    @Autowired OrderRepository repo;
}
```

---

## Performance Checklist

- [ ] No N+1 — verificar con SQL logs
- [ ] `FetchType.LAZY` en todas las asociaciones (override EAGER default de `@ManyToOne`)
- [ ] `@Transactional(readOnly = true)` en todos los métodos de lectura
- [ ] Paginación en queries de colecciones
- [ ] DTO projections para read paths
- [ ] `@Modifying` para bulk updates/deletes
- [ ] `@Version` en entidades con acceso concurrente
- [ ] Índices en columnas de filtro frecuente y foreign keys
- [ ] `toString()` sin campos lazy
- [ ] `CascadeType.ALL` solo padre → hijo
- [ ] `spring.jpa.open-in-view=false`
- [ ] Flyway/Liquibase — nunca `ddl-auto=update` en producción
- [ ] `@DataJpaTest` + Testcontainers
