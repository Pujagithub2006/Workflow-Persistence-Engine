# Fetch Strategies

## The N+1 Problem

The N+1 problem occurs when you load a list of entities and then lazily load a related entity for each item in the list.

Instead of executing a single query, JPA executes one query for the list plus one additional query for every entity.

Example:

```java
// One query
List<Issue> issues = issueRepo.findByProject(projectId);

for (Issue issue : issues) {
    issue.getReporter().getUsername();   // One extra query per issue
}
```

If there are 10 issues, Hibernate executes:

- 1 query to load the issues
- 10 additional queries to load each reporter

Total: **11 queries**

### Diagnosis

Enable Hibernate statistics and inspect the number of prepared statements.

---

## The Fix: Fetch Join

Tell JPA which relationships should be loaded as part of the original query.

```jpql
SELECT i
FROM Issue i
JOIN FETCH i.reporter
LEFT JOIN FETCH i.assignee
WHERE i.project.id = :pid
```

- `JOIN FETCH` loads required relationships in the same SQL query.
- `LEFT JOIN FETCH` is used when the relationship is optional (nullable).

Result:

- One SQL query
- No additional lazy-loading queries

---

## When to Use Each Strategy

| Situation | Recommended Approach |
|-----------|----------------------|
| Relationship will be accessed for every entity | Fetch Join |
| Relationship may never be accessed | Keep LAZY loading |
| Loading many parent entities with small related collections | `hibernate.default_batch_fetch_size` |
| Very large collections | Load separately and paginate |

---

## General Rules

1. Keep **LAZY** as the default fetch strategy.
2. `@ManyToOne` and `@OneToOne` may be eager if they are always required, but in this project they remain **LAZY**.
3. `@OneToMany` and `@ManyToMany` should remain **LAZY**.
4. Use `LEFT JOIN FETCH` when the relationship is optional.
5. Use `JOIN FETCH` when the relationship is mandatory.
6. `DISTINCT` is unnecessary for `@ManyToOne` fetch joins because no duplicate parent rows are produced.

---

## Batch Fetching

Configure Hibernate:

```xml
<property name="hibernate.default_batch_fetch_size" value="10"/>
```

Without batch fetching:

```sql
SELECT * FROM state WHERE id = 1;
SELECT * FROM state WHERE id = 2;
SELECT * FROM state WHERE id = 3;
```

With batch fetching:

```sql
SELECT *
FROM state
WHERE id IN (1,2,3,...,10);
```

Instead of executing many individual queries, Hibernate groups them into a single query.

Use batch fetching when:

- You cannot modify the original JPQL query.
- Relationships are loaded lazily in different parts of the application.
- Multiple entities are expected to access the same relationship.

---

## Common Anti-Patterns

Avoid the following:

- Using `EAGER` on `@OneToMany`.
- Using fetch joins with paginated collection queries.
- Loading an entire entity when only one or two fields are required.
- Triggering lazy loading repeatedly inside loops.

---

## Measuring Query Performance

Enable Hibernate statistics:

```java
Statistics stats = entityManagerFactory
        .unwrap(SessionFactory.class)
        .getStatistics();

stats.setStatisticsEnabled(true);

stats.clear();

// Execute application code

long queries = stats.getPrepareStatementCount();
```

Compare the number of prepared statements before and after optimization.

A lower query count generally indicates that unnecessary database round-trips have been eliminated.