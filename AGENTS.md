# AGENTS.md - Coding Guidelines for Notification Hub

## Build/Test Commands
- **Run all tests**: `./mvnw test`
- **Run single test**: `./mvnw test -Dtest=ClassName` (e.g., `./mvnw test -Dtest=AuthControllerTest`)
- **Build JAR**: `./mvnw clean package -DskipTests`
- **Run application**: `./mvnw spring-boot:run`
- **Run with coverage**: `./mvnw verify`

## Code Style Guidelines

### Imports & Organization
- Organize imports alphabetically
- Use fully qualified class names for DTOs and entities
- Separate static imports with blank line

### Dependencies & Injection
- Use constructor injection exclusively
- Avoid field injection (@Autowired)
- Order constructor parameters alphabetically

### Annotations & Lombok
- Use Lombok annotations: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, `@Slf4j`
- Apply `@Builder.Default` for collections in entities
- Use `@ToString.Exclude` for lazy-loaded relationships

### Entities & JPA
- Use `@Entity` with proper table naming (snake_case)
- Implement custom `equals()` and `hashCode()` using ID field
- Use `@PrePersist` for audit fields (createdAt)
- Define relationships with proper cascade and fetch types

### Services & Business Logic
- Use `@Service` and `@Transactional` at class level
- Implement interfaces for all services
- Use meaningful logging with parameterized messages
- Handle exceptions with custom exception classes

### Controllers & REST
- Use `@RestController` with proper request mapping
- Include comprehensive Swagger documentation
- Return `ResponseEntity<DTO>` for responses
- Use `@Valid` for request validation

### DTOs & Mapping
- Use MapStruct for entity-DTO conversion
- Separate request/response DTOs
- Include validation annotations (@NotBlank, @Size, etc.)

### Testing
- Use JUnit 5 with Mockito extension
- Follow naming: `*Test` for test classes
- Use `@DisplayName` for test descriptions
- Mock all external dependencies

### Error Handling
- Use custom exception classes in `exception.custom` package
- Implement global exception handler with consistent error responses
- Return structured error DTOs with status, message, and details

### Security & Validation
- Use Spring Security with JWT authentication
- Validate all inputs with Bean Validation
- Implement role-based access control (USER, ADMIN)
- Use enum-based status management

### Naming Conventions
- Classes: PascalCase (MessageService, UserRepository)
- Methods: camelCase (sendMessage, getUserById)
- Variables: camelCase (messageRepository, currentUser)
- Constants: UPPER_SNAKE_CASE
- Database: snake_case (messages, user_id, created_at)</content>
<parameter name="filePath">C:\Users\Nico\Desktop\notification-hub\AGENTS.md