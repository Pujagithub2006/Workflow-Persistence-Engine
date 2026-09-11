package org.workflow.engine;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.workflow.engine.domain.enums.IssuePriority;
import org.workflow.engine.domain.enums.IssueStatus;
import org.workflow.engine.domain.enums.IssueType;
import org.workflow.engine.domain.enums.UserRole;
import org.workflow.engine.domain.model.*;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.TransactionManager;
import org.workflow.engine.persistence.repositories.IssueRepository;
import org.workflow.engine.persistence.repositories.ProjectRepository;
import org.workflow.engine.persistence.repositories.UserRepository;
import org.workflow.engine.persistence.repositories.WorkspaceRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class FetchTest {
    private final UserRepository userRepo = new UserRepository();
    private final WorkspaceRepository workspaceRepo = new WorkspaceRepository();
    private final ProjectRepository projectRepo = new ProjectRepository();
    private final IssueRepository issueRepo = new IssueRepository();

    private Long projectId;
    private Statistics stats;

    @BeforeEach
    void setup() {
        cleanDatabase();
        buildTestData();

        stats = JpaUtil.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
    }

    @AfterAll
    static void shutdown() {
        JpaUtil.shutdown();
    }

    // N+1 problem
    @Test
    void lazyLoadingCausesNPlusOneQueries() {
        stats.clear();

        // 1 Query for the list
        EntityManager em = JpaUtil.getEntityManager();
        List<Issue> issues = em.createQuery(
                        "SELECT i FROM Issue i WHERE i.project.id = :pid", Issue.class)
                .setParameter("pid", projectId)
                .getResultList();

        // + N queries: one per issue, to load the reporter
        for(Issue issue : issues) {
            issue.getReporter().getUsername(); // triggers a query
        }

        em.close();

        long statements = stats.getPrepareStatementCount();

        // 1 (list) + 5 (reporters) = 6 minimum
        assertThat(statements).isGreaterThanOrEqualTo(issues.size()+1);

        System.out.println("LAZY: " + issues.size() + " issues -> " + statements + " queries");
    }

    @Test
    void fetchJoinSolvesNPlusOne() {
        stats.clear();

        // 1 query for everything
        List<Issue> issues = issueRepo.findByProjectWithDetails(projectId);

        // Access reporter + state + assignee — no more queries
        for (Issue issue : issues) {
            issue.getReporter().getUsername();
            issue.getCurrentState().getName();
            if (issue.getAssignee() != null) {
                issue.getAssignee().getUsername();
            }
        }

        long statements = stats.getPrepareStatementCount();

        // Exactly 1
        assertThat(statements).isEqualTo(1);
        assertThat(issues).isNotEmpty();

        System.out.println("FETCH JOIN: " + issues.size() + " issues -> " + statements + " query");
    }

    // Correctness of Fetch-Join methods
    @Test
    void findByKeyWithUsersLoadsReporterAndAssignee() {
        EntityManager em = JpaUtil.getEntityManager();
        String key;

        try {
            key = em.createQuery("SELECT i.issueKey FROM Issue i", String.class)
                    .setMaxResults(1).getSingleResult();

        } finally {
            em.close();
        }

        Issue issue = issueRepo.findByKeyWithUsers(key).orElseThrow();
        assertThat(issue.getReporter()).isNotNull();
        assertThat(issue.getReporter().getUsername()).isNotBlank();
    }

    @Test
    void findByKeyWithDetailsLoadsProjectLeadAndWorkspace() {
        Project project = projectRepo.findByKeyWithDetails("F1").orElseThrow();
        assertThat(project.getProjectLead()).isNotNull();
        assertThat(project.getProjectLead().getUsername()).isNotBlank();
        assertThat(project.getWorkspace()).isNotNull();
        assertThat(project.getWorkspace().getName()).isNotBlank();
    }

    // Batch Fetching
    @Test
    void batchFetchingGroupsLazyLoads() {
        stats.clear();

        // Load issues WITHOUT fetch join
        EntityManager em = JpaUtil.getEntityManager();
        List<Issue> issues = em.createQuery(
                        "SELECT i FROM Issue i WHERE i.project.id = :pid", Issue.class)
                .setParameter("pid", projectId)
                .getResultList();

        // Access state for each — batched because default_batch_fetch_size=10
        for (Issue issue : issues) {
            issue.getCurrentState().getName();
        }
        em.close();

        long statements = stats.getPrepareStatementCount();

        // 1 (list) + 1 (batched state load for all 5 issues) = 2
        assertThat(statements).isLessThanOrEqualTo(3);

        System.out.println("BATCH FETCH: " + issues.size() + " issues -> " + statements + " queries");
    }

    // Helper data
    private void cleanDatabase() {
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

    private void buildTestData() {
        User admin = userRepo.save(new User("fa", new Email("fa@test.com"), "Admin", UserRole.ADMIN));
        User lead = userRepo.save(new User("fl", new Email("fl@test.com"), "Lead", UserRole.PROJECT_LEAD));
        User dev = userRepo.save(new User("fd", new Email("fd@test.com"), "Dev", UserRole.DEVELOPER));

        Workflow wf = new Workflow("FW", "Test");
        State todo = new State("To Do", "Init", IssueStatus.TO_DO);
        wf.addState(todo);
        wf.setInitialState(todo);

        TransactionManager.inTransaction(em -> {
            em.persist(wf);
            return wf;
        });

        Workspace ws = new Workspace("FWS", "Test", admin);
        Project project = new Project("F1", "Fetch Project", "Test", lead);
        project.setWorkflow(wf);
        ws.addProject(project);
        workspaceRepo.save(ws);

        Project saved = projectRepo.findByKey("F1").orElseThrow();
        projectId = saved.getId();

        for (int i = 1; i <= 5; i++) {
            Issue issue = new Issue(saved.generateIssueKey(),
                    "Issue " + i, "Desc", dev,
                    IssueType.TASK, IssuePriority.MAJOR, todo);
            issue.setProject(saved);
            issue.assignTo(dev);
            issueRepo.save(issue);
        }
    }
}
