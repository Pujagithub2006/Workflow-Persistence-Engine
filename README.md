# Workflow Persistence Engine

A production-inspired workflow persistence engine built in **Java**, **Hibernate ORM**, and **PostgreSQL**.

The project models the persistence layer behind workflow management platforms such as **Jira**, focusing on how users, workspaces, projects, workflows, issues, comments, and audit events are represented, stored, queried, and maintained.

Instead of building a complete web application, this repository focuses on designing a clean domain model and implementing a production-style persistence layer using enterprise JPA and Hibernate patterns with an emphasis on correctness, maintainability, performance, and testing.

---

# Inspiration

Workflow management systems like Jira are much more than issue trackers. Behind every board lies a persistence layer responsible for managing relationships, workflow definitions, transactions, concurrency, efficient querying, audit history, and performance at scale.

This project is my implementation of those persistence concepts in a simplified workflow engine. The objective was **not** to recreate Jira feature-by-feature, but to understand and implement the architectural patterns behind its persistence layer using plain Java, Hibernate ORM, and PostgreSQL.

---

# Features

- Rich workflow domain model
- JPA & Hibernate ORM mapping
- Repository-based persistence layer
- Workflow and issue management
- Entity relationships and lifecycle management
- JPQL and Criteria API querying
- DTO projections
- Pagination, sorting, and dynamic filtering
- Transaction management
- Optimistic locking
- Batch processing
- Bulk update and delete operations
- Database indexing
- Performance measurement using Hibernate Statistics
- Repository and integration test suite

---

# Technology Stack

| Category | Technology |
|-----------|------------|
| Language | Java 25 |
| ORM | Hibernate ORM |
| Persistence | Jakarta Persistence (JPA) |
| Database | PostgreSQL |
| Build Tool | Maven |
| Logging | SLF4J + Logback |
| Testing | JUnit 5, AssertJ |

---

# Project Architecture

```
                 Workflow Persistence Engine

                   Domain Model
                        │
                        ▼
               Repository Layer
                        │
                        ▼
            JPA / Hibernate ORM
                        │
                        ▼
                  PostgreSQL Database
```

The project intentionally focuses on the persistence layer and does not include a REST API or frontend.

---

# Development Roadmap

| Issue | Implementation | Highlights |
|:----:|-----------------|------------|
| **#0** | Project Bootstrap | Maven setup, Java 25, Hibernate configuration, PostgreSQL, logging, project structure, documentation |
| **#1** | Domain Model | Users, Workspaces, Projects, Workflows, States, Transitions, Issues, Comments, Audit Logs, Value Objects, Enums |
| **#2** | JPA Entity Mapping | Entity mapping, table configuration, embedded objects, constraints, optimistic locking, database schema |
| **#3** | Repository Layer | Repository pattern, CRUD operations, EntityManager abstraction, transaction handling |
| **#4** | Data Bootstrapper | Realistic sample data generation, workflow initialization, object graph persistence |
| **#5** | Entity Relationships | One-to-many, many-to-one, cascading, orphan removal, bidirectional associations, relationship testing |
| **#6** | Entity Lifecycle | Persist, merge, remove, detach, refresh, dirty checking, optimistic locking, lifecycle testing |
| **#7** | Fetch Strategies | Lazy loading, eager loading, fetch joins, N+1 query prevention, fetch optimization |
| **#8** | Transactions & Concurrency | Transaction management, commit/rollback handling, optimistic locking, concurrent update testing |
| **#9** | Advanced Querying | JPQL, Criteria API, DTO projections, pagination, sorting, aggregation, grouping, dynamic filtering |
| **#10** | Performance Optimization | JDBC batching, bulk operations, database indexes, Hibernate Statistics, performance benchmarking |
| **#11** | Persistence Testing | Repository CRUD tests, integration tests, reusable test utilities, testing documentation |

---

# Core Concepts Implemented

### Domain Modeling

- Rich domain entities
- Value Objects
- Business rules
- Aggregate relationships

### Persistence

- JPA Entity Mapping
- Hibernate ORM
- Repository Pattern
- Persistence Context
- Entity Lifecycle
- Optimistic Locking

### Relationships

- One-to-One
- One-to-Many
- Many-to-One
- Cascading
- Orphan Removal

### Querying

- JPQL
- Criteria API
- Constructor Projections (DTO)
- Pagination
- Dynamic Queries
- Aggregation
- Group By

### Transactions

- Transaction Management
- Commit & Rollback
- Concurrency
- Version Control

### Performance

- JDBC Batching
- Bulk Updates
- Bulk Deletes
- Database Indexes
- Hibernate Statistics

### Testing

- Repository Testing
- Integration Testing
- Reusable Test Fixtures
- Database Cleanup Utilities
- AssertJ Assertions

---

# Project Structure

```
workflow-persistence-engine
│
├── docs
│   ├── MappingGuide.md
│   ├── RelationshipGuide.md
│   ├── LifecycleGuide.md
│   ├── FetchGuide.md
│   ├── TransactionGuide.md
│   ├── QueryGuide.md
│   ├── PerformanceGuide.md
│   └── TestingGuide.md
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── org.workflow.engine
│   │   │       ├── bootstrap
│   │   │       ├── domain
│   │   │       ├── persistence
│   │   │       └── ...
│   │   │
│   │   └── resources
│   │       ├── META-INF
│   │       └── sql
│   │
│   └── test
│       └── java
│           └── org.workflow.engine
│               ├── repository
│               ├── integration
│               ├── testutil
│               └── ...
│
├── pom.xml
└── README.md
```

---

# Getting Started

## Prerequisites

- Java 25
- Maven 3.9+
- PostgreSQL

## Clone the Repository

```bash
git clone https://github.com/<username>/workflow-persistence-engine.git
cd workflow-persistence-engine
```

## Configure the Database

Update the PostgreSQL connection details inside:

```
src/main/resources/META-INF/persistence.xml
```

## Build the Project

```bash
mvn clean compile
```

## Bootstrap Sample Data

Run the bootstrapper to populate the database.

```bash
mvn exec:java
```

---

# Running the Tests

Run the complete test suite:

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

Run a single test class:

```bash
mvn test -Dtest=UserRepositoryTest
```

Run a specific test method:

```bash
mvn test -Dtest=UserRepositoryTest#saveInsertsNewUser
```

---

# Documentation

Additional implementation notes are available in the `docs/` directory.

- Mapping Guide
- Relationship Guide
- Lifecycle Guide
- Fetch Guide
- Transaction Guide
- Query Guide
- Performance Guide
- Testing Guide

Each document focuses on the design decisions and implementation details behind a specific part of the persistence layer.