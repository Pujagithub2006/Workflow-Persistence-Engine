# Testing Guide

## Test Layout

```text
src/test/java/org/workflow/engine/
├── repository/
│   ├── UserRepositoryTest.java          # CRUD tests for User repository
│   ├── WorkspaceRepositoryTest.java     # CRUD tests for Workspace repository
│   ├── ProjectRepositoryTest.java       # CRUD tests for Project repository
│   └── IssueRepositoryTest.java         # CRUD tests for Issue repository
├── integration/
│   └── WorkflowIntegrationTest.java     # End-to-end persistence workflow
├── testutil/
│   ├── TestCleanup.java                 # Reset database between tests
│   └── TestData.java                    # Reusable persisted test fixtures
├── RelationshipTest.java                # Issue #5
├── LifecycleTest.java                   # Issue #6
├── FetchTest.java                       # Issue #7
├── TransactionTest.java                 # Issue #8
├── ConcurrencyTest.java                 # Issue #8
├── QueryTest.java                       # Issue #9
└── PerformanceTest.java                 # Issue #10
```

---

## Test Categories

| Category | Purpose | Example |
|----------|---------|---------|
| Repository | CRUD operations and repository queries | `UserRepositoryTest` |
| Integration | End-to-end persistence workflows | `WorkflowIntegrationTest` |
| Feature | Individual JPA concepts | `RelationshipTest`, `FetchTest` |

---

## Standard Test Setup

Every test interacting with the database should follow the same lifecycle.

```java
@BeforeEach
void clean() {
    TestCleanup.all();
}

@AfterAll
static void shutdown() {
    JpaUtil.shutdown();
}
```

`TestCleanup.all()` resets the database before every test.

`JpaUtil.shutdown()` should only be called once after the entire test class finishes.

Do **not** call `JpaUtil.shutdown()` inside `@AfterEach`, as it closes the shared `EntityManagerFactory`.

---

## Reusable Test Fixtures

```java
User admin = TestData.user(UserRole.ADMIN);
User developer = TestData.user();

Workspace workspace = TestData.workspace(admin);
Workflow workflow = TestData.workflow();
Project project = TestData.project(workspace, workflow, admin);
Issue issue = TestData.issue(project, developer, workflow);
```

Each helper returns a fresh, persisted entity with unique test data.

---

## Repository Test Checklist

Each repository should verify the following behavior:

- `save()` inserts a new entity
- Generated identifier is assigned
- Initial version is created
- `save()` updates an existing entity
- Version increments after update
- `findById()` returns an existing entity
- `findById()` returns `Optional.empty()` for missing records
- `findAll()` returns expected entities
- `delete()` removes persisted entities
- Duplicate constraints throw exceptions
- Invalid input is rejected

Approximately 8–10 focused tests per repository provide sufficient coverage.

---

## Assertion Style

Use AssertJ for readable assertions.

```java
assertThat(user.getId()).isNotNull();
assertThat(user.getUsername()).isEqualTo("alice");
assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);

assertThat(users)
        .hasSize(3)
        .contains(alice);

assertThat(user.getVersion())
        .isGreaterThan(0L);

assertThatThrownBy(() -> userRepository.save(duplicateUser))
        .isInstanceOf(RuntimeException.class);
```

---

## Test Naming Convention

Follow the pattern:

```text
should<Action><Condition>
```

Examples:

- `shouldInsertNewUserAndGenerateId`
- `shouldReturnEmptyForMissingUser`
- `shouldRejectDuplicateUsername`
- `shouldIncrementVersionOnUpdate`

Test names should describe the expected behavior rather than the implementation.

---

## What Not to Test

Avoid writing tests for:

- Getter and setter methods
- `equals()` and `hashCode()` on simple entities
- Hibernate or JPA framework internals
- Simple delegation methods
- Mocked database interactions

Repository tests should always execute against a real database.

---

## Running Tests

Run the complete suite:

```bash
mvn test
```

Run repository tests only:

```bash
mvn test -Dtest="*RepositoryTest"
```

Run integration tests only:

```bash
mvn test -Dtest="*IntegrationTest"
```

Skip performance tests:

```bash
mvn test -Dtest="!PerformanceTest"
```

Run a single test class:

```bash
mvn test -Dtest=UserRepositoryTest
```

Run a single test method:

```bash
mvn test -Dtest=UserRepositoryTest#shouldRejectDuplicateUsername
```

---

## Why No Mocks?

The persistence layer is tested against a real PostgreSQL database.

This verifies:

- Constraint enforcement
- Entity relationships
- Cascade operations
- Lazy loading behavior
- Optimistic locking
- Transaction management

Although the test suite executes more slowly than mocked tests, it validates actual JPA behavior instead of simulated interactions.

If execution time becomes a concern in the future, Testcontainers can provide isolated databases without changing the testing approach.

---

## Common Failure Modes

| Symptom | Likely Cause | Resolution |
|---------|--------------|------------|
| `EntityNotFoundException` between tests | Database not cleaned | Call `TestCleanup.all()` in `@BeforeEach` |
| `SessionFactory is closed` | `JpaUtil.shutdown()` called too early | Move shutdown to `@AfterAll` |
| `LazyInitializationException` | Lazy association accessed outside a transaction | Use fetch joins or DTO projections |
| Tests fail only when run together | Shared database state | Reset the database before every test |
| Slow query execution | Missing indexes | Apply `indexes.sql` before running tests |

---

## Key Principles

1. Keep tests isolated.
2. Reset the database before every test.
3. Use reusable fixtures to reduce duplication.
4. Test behavior, not implementation details.
5. Prefer integration tests over mocked persistence tests.
6. Write clear, descriptive test names.
7. Keep repository tests focused on CRUD and query behavior.
8. Validate every important JPA feature with executable tests.