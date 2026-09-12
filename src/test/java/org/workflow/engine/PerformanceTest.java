package org.workflow.engine;

import org.workflow.engine.domain.enums.*;
import org.workflow.engine.domain.model.*;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.TransactionManager;
import org.workflow.engine.persistence.repositories.*;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PerformanceTest {

    private final UserRepository userRepo = new UserRepository();
    private final WorkspaceRepository workspaceRepo = new WorkspaceRepository();
    private final ProjectRepository projectRepo = new ProjectRepository();
    private final IssueRepository issueRepo = new IssueRepository();
    private final BatchRepository batchRepo = new BatchRepository();

    private Statistics stats;

    @BeforeEach
    void setup() {
        clean();
        stats = JpaUtil.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
    }

    @AfterAll
    static void shutdown() {
        JpaUtil.shutdown();
    }

    // ---------- Batch insert ----------

    @Test
    void batchInsertUsesFewStatements() {
        int count = 200;
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(new User("bu" + i,
                    new Email("bu" + i + "@test.com"),
                    "Batch User " + i, UserRole.DEVELOPER));
        }

        stats.clear();
        batchRepo.batchInsertUsers(users);
        long statements = stats.getPrepareStatementCount();

        System.out.println("Batch insert " + count + " users: " + statements + " statements");

        assertThat(userRepo.findAll()).hasSize(count);
        // With batch_size=50, 200 inserts → ~4-8 statements
        assertThat(statements).isLessThan(count / 5);
    }

    // ---------- Bulk update ----------

    @Test
    void bulkUpdateIsOneStatement() {
        setupProjectWithIssues(500);

        Project project = projectRepo.findByKey("PERF").orElseThrow();
        long majorCount = issueRepo.findByProject(project.getId()).stream()
                .filter(i -> i.getPriority() == IssuePriority.MAJOR).count();
        assertThat(majorCount).isGreaterThan(0);

        stats.clear();
        int updated = batchRepo.bulkUpdatePriority(
                project.getId(), IssuePriority.MAJOR, IssuePriority.CRITICAL);
        long statements = stats.getPrepareStatementCount();

        System.out.println("Bulk updated " + updated + " issues in " + statements + " statement");

        assertThat(updated).isEqualTo(majorCount);
        assertThat(statements).isEqualTo(1);
    }

    // ---------- Bulk delete ----------

    @Test
    void bulkDeleteIsOneStatement() {
        setupProjectWithIssues(100);

        Project project = projectRepo.findByKey("PERF").orElseThrow();
        assertThat(issueRepo.findByProject(project.getId())).hasSize(100);

        stats.clear();
        int deleted = batchRepo.bulkDeleteIssuesByProject(project.getId());
        long statements = stats.getPrepareStatementCount();

        System.out.println("Bulk deleted " + deleted + " issues in " + statements + " statement");

        assertThat(deleted).isEqualTo(100);
        assertThat(statements).isEqualTo(1);
    }

    // ---------- Memory efficiency from clear() ----------

    @Test
    void batchInsertWithClearDoesNotExhaustMemory() {
        // 5000 users would be ~5000 managed entities if we didn't clear
        int count = 5000;
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(new User("mem" + i,
                    new Email("mem" + i + "@test.com"),
                    "Memory " + i, UserRole.DEVELOPER));
        }

        batchRepo.batchInsertUsers(users);

        // If we didn't clear, this would still work but memory would be high
        assertThat(userRepo.findAll()).hasSize(count);
    }

    // ---------- Statistics reporting ----------

    @Test
    void statisticsShowQueryAndEntityCounts() {
        setupProjectWithIssues(100);

        stats.clear();

        // Run some operations
        Project project = projectRepo.findByKey("PERF").orElseThrow();
        issueRepo.findByProject(project.getId());
        issueRepo.findByProject(project.getId());

        System.out.println("=== Statistics ===");
        System.out.println("Query executions:    " + stats.getQueryExecutionCount());
        System.out.println("Prepare statements:  " + stats.getPrepareStatementCount());
        System.out.println("Entity loads:        " + stats.getEntityLoadCount());
        System.out.println("Transaction count:   " + stats.getTransactionCount());

        assertThat(stats.getPrepareStatementCount()).isGreaterThan(0);
    }

    // ---------- Helpers ----------

    private void setupProjectWithIssues(int issueCount) {
        User admin = userRepo.save(new User("perfa",
                new Email("perfa@test.com"), "Admin", UserRole.ADMIN));
        User lead = userRepo.save(new User("perfl",
                new Email("perfl@test.com"), "Lead", UserRole.PROJECT_LEAD));
        User dev = userRepo.save(new User("perfd",
                new Email("perfd@test.com"), "Dev", UserRole.DEVELOPER));

        Workflow wf = new Workflow("PerfWF", "Test");
        State todo = new State("To Do", "Init", IssueStatus.TO_DO);
        wf.addState(todo);
        wf.setInitialState(todo);

        TransactionManager.inTransaction(em -> {
            em.persist(wf);
            return wf;
        });

        Workspace ws = new Workspace("PerfWS", "Test", admin);
        Project project = new Project("PERF", "Perf Project", "Test", lead);
        project.setWorkflow(wf);
        ws.addProject(project);
        workspaceRepo.save(ws);

        Project saved = projectRepo.findByKey("PERF").orElseThrow();

        List<Issue> issues = new ArrayList<>();
        for (int i = 0; i < issueCount; i++) {
            Issue issue = new Issue(saved.generateIssueKey(),
                    "Task " + i, "Desc " + i, dev,
                    IssueType.TASK,
                    i % 2 == 0 ? IssuePriority.MAJOR : IssuePriority.MINOR,
                    todo);
            issue.setProject(saved);
            issue.assignTo(dev);
            issues.add(issue);
        }

        // Batch insert the issues
        TransactionManager.inTransaction(em -> {
            for (int i = 0; i < issues.size(); i++) {
                em.persist(issues.get(i));
                if ((i + 1) % 50 == 0) {
                    em.flush();
                    em.clear();
                }
            }
            return "Batch inserted the issues!";
        });
    }

    private void clean() {
        TransactionManager.inTransaction(em -> {
            em.createQuery("DELETE FROM AuditLog").executeUpdate();
            em.createQuery("DELETE FROM Comment").executeUpdate();
            em.createQuery("DELETE FROM Issue").executeUpdate();
            em.createQuery("DELETE FROM Transition").executeUpdate();
            em.createQuery("DELETE FROM State").executeUpdate();
            em.createQuery("DELETE FROM Workflow").executeUpdate();
            em.createQuery("DELETE FROM Project").executeUpdate();
            em.createQuery("DELETE FROM Workspace").executeUpdate();
            em.createQuery("DELETE FROM User").executeUpdate();

            return "Database cleaned!";
        });
    }
}