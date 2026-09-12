# Performance Guide

## Overview

Performance optimization should be driven by measurement rather than assumptions. In most JPA applications, three optimizations consistently provide the largest improvements:

1. Batch inserts
2. Bulk update and delete operations
3. Proper database indexing

Everything else should be considered only after these fundamentals are in place.

---

## 1. Batch Inserts

### Without Batching

Every `persist()` operation results in an individual SQL statement.

```java
for (User user : users) {
    em.persist(user);
}

tx.commit();
```

Result:

- One database round trip per insert
- High network overhead
- Poor scalability for large datasets

---

### With Batching

```java
for (int i = 0; i < users.size(); i++) {
    em.persist(users.get(i));

    if ((i + 1) % 50 == 0) {
        em.flush();
        em.clear();
    }
}

tx.commit();
```

Result:

- Inserts are grouped into JDBC batches
- Significantly fewer database round trips
- Lower memory usage

---

### Why `flush()` and `clear()`?

`flush()` synchronizes the persistence context with the database by sending pending SQL statements.

`clear()` removes all managed entities from the persistence context.

Without `clear()`, every persisted entity remains managed until the transaction commits.

For very large batches, this causes:

- Increasing memory usage
- Slower dirty checking
- Potential `OutOfMemoryError`

Clearing the persistence context after each batch keeps memory usage stable.

---

### Hibernate Configuration

```xml
<property name="hibernate.jdbc.batch_size" value="50"/>
<property name="hibernate.order_inserts" value="true"/>
```

Recommended batch sizes typically range from **25–100**, with **50** being a good default.

---

## 2. Bulk Updates

### Entity-Based Updates

```java
List<Issue> issues = em.createQuery(
        "SELECT i FROM Issue i WHERE i.priority = :priority",
        Issue.class)
    .setParameter("priority", IssuePriority.MAJOR)
    .getResultList();

for (Issue issue : issues) {
    issue.setPriority(IssuePriority.CRITICAL);
}
```

This approach:

- Loads every entity into memory
- Performs dirty checking
- Executes multiple update statements

---

### Bulk JPQL Update

```java
em.createQuery(
        "UPDATE Issue i " +
        "SET i.priority = :to " +
        "WHERE i.priority = :from")
    .setParameter("to", IssuePriority.CRITICAL)
    .setParameter("from", IssuePriority.MAJOR)
    .executeUpdate();
```

Benefits:

- Executes a single SQL `UPDATE`
- No entity loading
- Much lower memory usage
- Faster for large datasets

---

### Important Caveat

Bulk updates bypass the persistence context.

Any entities already managed by the current `EntityManager` become stale.

If bulk operations are mixed with managed entities in the same transaction, clear the persistence context afterwards.

```java
em.clear();
```

---

## 3. Bulk Deletes

Instead of loading entities before removing them:

```java
for (Issue issue : issues) {
    em.remove(issue);
}
```

use a JPQL bulk delete:

```java
em.createQuery(
        "DELETE FROM Issue i WHERE i.project.id = :projectId")
    .setParameter("projectId", projectId)
    .executeUpdate();
```

Benefits:

- One SQL statement
- No entity loading
- Much faster for large deletions

---

## 4. Database Indexes

Indexes improve query performance by reducing full table scans.

Typical indexing strategy:

| Query Pattern | Recommended Index |
|--------------|-------------------|
| Foreign keys | B-tree |
| Frequently filtered columns | B-tree |
| Composite filters | Composite index |
| Common sorting columns | Ordered index |
| Partial filtering | Partial index |

Example:

```sql
CREATE INDEX idx_issues_project
ON issues(project_id);

CREATE INDEX idx_issues_project_state
ON issues(project_id, state_id);

CREATE INDEX idx_issues_active
ON issues(project_id, updated_at DESC)
WHERE resolved_at IS NULL;
```

---

### When to Add Indexes

Indexes are most useful for columns that are:

- Frequently searched
- Frequently joined
- Frequently sorted
- Frequently grouped

Avoid indexing every column.

Each additional index:

- Consumes storage
- Increases insert cost
- Increases update cost
- Increases delete cost

Only create indexes that support actual query patterns.

---

## Measuring Performance

Enable Hibernate Statistics:

```xml
<property name="hibernate.generate_statistics" value="true"/>
```

Retrieve statistics:

```java
Statistics statistics =
        entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();

statistics.clear();

// Execute work

long statements =
        statistics.getPrepareStatementCount();

long entityLoads =
        statistics.getEntityLoadCount();

long queryExecutions =
        statistics.getQueryExecutionCount();

long transactions =
        statistics.getTransactionCount();
```

Useful metrics include:

- Prepared statement count
- Query execution count
- Entity load count
- Transaction count
- Maximum query execution time

Always measure before attempting further optimization.

---

## Common Anti-Patterns

Avoid the following:

- Persisting thousands of entities without batching
- Forgetting `flush()` and `clear()` during batch operations
- Loading entities solely to update one field
- Loading entities solely to delete them
- Missing indexes on frequently queried foreign keys
- Creating unnecessary indexes
- Optimizing without measuring

---

## Optimization Priority

Apply optimizations in this order:

1. Batch inserts
2. Bulk updates
3. Bulk deletes
4. Proper indexing
5. Measure using Hibernate Statistics

Only consider more advanced optimizations after these fundamentals are implemented and measured.

---

## Advanced Optimizations (Later)

Only investigate these when performance measurements indicate they are necessary:

1. Second-level cache
2. Query cache
3. Fetch size tuning
4. Connection pool tuning
5. JDBC driver configuration
6. Query execution plan analysis

For this project, these optimizations are intentionally out of scope.

---

## Key Takeaways

- Batch inserts reduce database round trips.
- `flush()` writes pending SQL, while `clear()` releases managed entities.
- Bulk JPQL operations avoid loading entities into memory.
- Bulk operations bypass the persistence context.
- Indexes should support real query patterns, not every column.
- Hibernate Statistics should be used to measure performance improvements.
- Measure first, optimize second.