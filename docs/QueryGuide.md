# Query Guide

## When to Use What

| Situation | Use |
|-----------|-----|
| Fixed query with parameters | JPQL |
| Optional dynamic filters | Criteria API |
| Aggregations (`COUNT`, `GROUP BY`) | JPQL |
| Complex dynamic joins | Criteria API |
| Reusable named queries | Not needed for this project |

**Rule:** Default to JPQL. Reach for Criteria API only when the query is genuinely dynamic.

---

## JPQL Basics

```java
em.createQuery(
        "SELECT i FROM Issue i WHERE i.priority = :p",
        Issue.class)
    .setParameter("p", IssuePriority.CRITICAL)
    .getResultList();
```

### Key Points

- Uses **entity names**, not table names (`Issue`, not `issues`)
- Uses **entity field names**, not database column names (`priority`, not `priority_col`)
- Always uses **named parameters**
- Avoid string concatenation

---

## Aggregate Queries

```java
em.createQuery(
        "SELECT i.currentState.status, COUNT(i) " +
        "FROM Issue i " +
        "WHERE i.project.id = :pid " +
        "GROUP BY i.currentState.status",
        Object[].class)
    .setParameter("pid", projectId)
    .getResultList();
```

### Notes

- Returns `List<Object[]>`
- Each row contains:
    - `row[0]` → Status
    - `row[1]` → Count
- Use a DTO when stronger typing is preferred.

---

## DTO Projections

Instead of loading the full entity, fetch only the required fields.

```java
SELECT new org.workflow.engine.domain.dto.IssueSummary(
    i.issueKey,
    i.summary,
    i.priority,
    s.status,
    COALESCE(a.username, 'Unassigned')
)
FROM Issue i
JOIN i.currentState s
LEFT JOIN i.assignee a
WHERE i.project.id = :pid
```

### Benefits

- Retrieves only required columns
- Reduces database traffic
- No managed entity or dirty checking
- DTO records are immutable and lightweight

---

## Pagination

```java
em.createQuery(
        "SELECT i FROM Issue i WHERE i.project.id = :pid ORDER BY i.id",
        Issue.class)
    .setParameter("pid", projectId)
    .setFirstResult(page * size)
    .setMaxResults(size)
    .getResultList();
```

Retrieve the total record count separately.

```java
em.createQuery(
        "SELECT COUNT(i) FROM Issue i WHERE i.project.id = :pid",
        Long.class)
    .setParameter("pid", projectId)
    .getSingleResult();
```

### Why?

- Supports paginated UI
- Allows calculation of total pages
- Prevents loading unnecessary rows

---

## Dynamic Sorting

Never concatenate raw user input into JPQL.

```java
private String whitelistSortField(String field) {
    return switch (field) {
        case "issueKey", "summary", "priority", "createdAt" -> field;
        default -> "id";
    };
}
```

```java
String safeField = whitelistSortField(sortField);

String jpql =
    "SELECT i FROM Issue i " +
    "ORDER BY i." + safeField + " ASC";
```

### Why?

Dynamic field names cannot be parameterized.

Whitelist allowed values to prevent JPQL/SQL injection.

---

## Criteria API

Use Criteria API only when filters are optional.

```java
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Issue> cq = cb.createQuery(Issue.class);
Root<Issue> root = cq.from(Issue.class);

List<Predicate> predicates = new ArrayList<>();

predicates.add(
    cb.equal(root.get("project").get("id"), projectId)
);

if (priority != null) {
    predicates.add(
        cb.equal(root.get("priority"), priority)
    );
}

if (status != null) {
    predicates.add(
        cb.equal(root.get("currentState").get("status"), status)
    );
}

cq.where(predicates.toArray(new Predicate[0]));
cq.orderBy(cb.desc(root.get("id")));

return em.createQuery(cq).getResultList();
```

### Why Criteria?

- Optional search filters
- Dynamic query construction
- Type-safe query building
- Eliminates complex string concatenation

---

## JPQL vs Criteria API

| Feature | JPQL | Criteria API |
|---------|------|--------------|
| Simple queries | Excellent | Verbose |
| Readability | High | Low |
| Dynamic filters | Limited | Excellent |
| Aggregations | Excellent | Good |
| Constructor projections | Excellent | Possible but verbose |
| Learning curve | Easy | Higher |

**Recommendation:** Use JPQL unless dynamic query construction is required.

---

## Common Patterns in This Project

| Repository Method | Pattern |
|-------------------|---------|
| `findByPriority()` | JPQL filter |
| `findByProjectAndStatus()` | JPQL multiple parameters |
| `countByProjectAndPriority()` | JPQL aggregate |
| `countByStatusInProject()` | JPQL `GROUP BY` |
| `findSummariesByProject()` | DTO projection |
| `findByProjectPaginated()` | Pagination + sorting |
| `countByProject()` | Count query |
| `search()` | Criteria API |

---

## Anti-Patterns

- String concatenation inside JPQL
- Using Criteria API for simple queries
- Loading entire entities when only a few fields are required
- Forgetting the count query when implementing pagination
- Trusting user input in `ORDER BY`
- Building different JPQL strings for every filter combination

---

## Best Practices

1. Use JPQL by default.
2. Use named parameters for every value.
3. Whitelist sortable fields.
4. Use DTO projections for read-only list screens.
5. Pair pagination with a count query.
6. Use Criteria API only for optional filters.
7. Keep repository methods focused on a single query responsibility.