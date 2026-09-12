package org.workflow.engine;

import org.workflow.engine.domain.dto.IssueSummary;
import org.workflow.engine.domain.enums.*;
import org.workflow.engine.domain.model.*;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.TransactionManager;
import org.workflow.engine.persistence.repositories.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class QueryTest {

    private final UserRepository userRepo = new UserRepository();
    private final WorkspaceRepository workspaceRepo = new WorkspaceRepository();
    private final ProjectRepository projectRepo = new ProjectRepository();
    private final IssueRepository issueRepo = new IssueRepository();
    private final IssueQueryRepository queryRepo = new IssueQueryRepository();

    private Long projectId;
    private Long devId;
    private Long unassignedId;

    @BeforeEach
    void setup() {
        clean();

        User admin = userRepo.save(new User("qa", new Email("qa@test.com"), "Admin", UserRole.ADMIN));
        User lead = userRepo.save(new User("ql", new Email("ql@test.com"), "Lead", UserRole.PROJECT_LEAD));
        User dev = userRepo.save(new User("qd", new Email("qd@test.com"), "Dev", UserRole.DEVELOPER));
        devId = dev.getUserId();

        Workflow wf = new Workflow("QW", "Test");
        State todo = new State("To Do", "Init", IssueStatus.TO_DO);
        State inProgress = new State("In Progress", "Working", IssueStatus.IN_PROGRESS);
        State done = new State("Done", "Complete", IssueStatus.DONE);
        wf.addState(todo);
        wf.addState(inProgress);
        wf.addState(done);
        wf.setInitialState(todo);

        TransactionManager.inTransaction(em -> {
            em.persist(wf);
            return wf;
        });

        Workspace ws = new Workspace("QWS", "Test", admin);
        Project project = new Project("QP", "Query Project", "Test", lead);
        project.setWorkflow(wf);
        ws.addProject(project);
        workspaceRepo.save(ws);

        Project saved = projectRepo.findByKey("QP").orElseThrow();
        projectId = saved.getId();

        // Test data — 6 issues with distinct attributes
        createIssue(saved, "Critical bug", IssuePriority.CRITICAL, IssueType.BUG, todo, dev);
        createIssue(saved, "Minor task", IssuePriority.MINOR, IssueType.TASK, todo, null);
        createIssue(saved, "Major feature", IssuePriority.MAJOR, IssueType.STORY, inProgress, dev);
        createIssue(saved, "Another critical", IssuePriority.CRITICAL, IssueType.BUG, inProgress, null);
        createIssue(saved, "Completed", IssuePriority.MAJOR, IssueType.TASK, done, dev);
        createIssue(saved, "Old task", IssuePriority.MINOR, IssueType.TASK, done, null);

        // Get the ID of the "Minor task" (unassigned) for assertions
        unassignedId = issueRepo.findByProject(projectId).stream()
                .filter(i -> i.getAssignee() == null)
                .findFirst()
                .map(Issue::getId)
                .orElseThrow();
    }

    @AfterAll
    static void shutdown() {
        JpaUtil.shutdown();
    }

    // ---------- JPQL: simple filter ----------

    @Test
    void findByPriorityReturnsMatchingIssues() {
        List<Issue> critical = queryRepo.findByPriority(IssuePriority.CRITICAL);
        assertThat(critical).hasSize(2);
        assertThat(critical).allMatch(i -> i.getPriority() == IssuePriority.CRITICAL);
    }

    // ---------- JPQL: filter by status ----------

    @Test
    void findByProjectAndStatusReturnsTodoIssues() {
        List<Issue> todo = queryRepo.findByProjectAndStatus(projectId, IssueStatus.TO_DO);
        assertThat(todo).hasSize(2);
    }

    // ---------- JPQL: aggregate count ----------

    @Test
    void countByProjectAndPriorityReturnsCorrectCount() {
        long count = queryRepo.countByProjectAndPriority(projectId, IssuePriority.CRITICAL);
        assertThat(count).isEqualTo(2);
    }

    // ---------- JPQL: GROUP BY ----------

    @Test
    void countByStatusInProjectGroupsCorrectly() {
        List<Object[]> rows = queryRepo.countByStatusInProject(projectId);
        assertThat(rows).hasSize(3);

        // Log for inspection
        for (Object[] row : rows) {
            System.out.println("Status: " + row[0] + ", Count: " + row[1]);
        }
    }

    // ---------- Projection: only needed fields ----------

    @Test
    void findSummariesByProjectReturnsProjections() {
        List<IssueSummary> summaries = queryRepo.findSummariesByProject(projectId);

        assertThat(summaries).hasSize(6);
        assertThat(summaries.get(0).issueKey()).isNotBlank();
        assertThat(summaries).anyMatch(s -> s.assigneeName().equals("Unassigned"));
        assertThat(summaries).anyMatch(s -> s.assigneeName().equals("qd"));
    }

    // ---------- Pagination ----------

    @Test
    void paginationReturnsCorrectSlices() {
        List<Issue> page1 = queryRepo.findByProjectPaginated(projectId, 0, 3, "id", true);
        List<Issue> page2 = queryRepo.findByProjectPaginated(projectId, 1, 3, "id", true);

        assertThat(page1).hasSize(3);
        assertThat(page2).hasSize(3);

        // No overlap
        assertThat(page1.get(0).getId()).isLessThan(page2.get(0).getId());
    }

    @Test
    void paginationWithDifferentSortOrders() {
        List<Issue> asc = queryRepo.findByProjectPaginated(projectId, 0, 10, "priority", true);
        List<Issue> desc = queryRepo.findByProjectPaginated(projectId, 0, 10, "priority", false);

        assertThat(asc).hasSize(6);
        assertThat(desc).hasSize(6);
        // Reversed
        assertThat(asc.get(0).getId()).isNotEqualTo(desc.get(0).getId());
    }

    @Test
    void whitelistRejectsInvalidSortField() {
        // "maliciousField" isn't whitelisted, falls back to id
        List<Issue> issues = queryRepo.findByProjectPaginated(projectId, 0, 10, "maliciousField", true);
        assertThat(issues).hasSize(6);
    }

    @Test
    void countByProjectReturnsTotal() {
        assertThat(queryRepo.countByProject(projectId)).isEqualTo(6);
    }

    // ---------- Criteria: dynamic filtering ----------

    @Test
    void searchWithNoOptionalFilters() {
        List<Issue> all = queryRepo.search(projectId, null, null, null);
        assertThat(all).hasSize(6);
    }

    @Test
    void searchByPriorityOnly() {
        List<Issue> result = queryRepo.search(projectId, IssuePriority.CRITICAL, null, null);
        assertThat(result).hasSize(2);
    }

    @Test
    void searchByStatusOnly() {
        List<Issue> result = queryRepo.search(projectId, null, IssueStatus.DONE, null);
        assertThat(result).hasSize(2);
    }

    @Test
    void searchByAssigneeOnly() {
        List<Issue> result = queryRepo.search(projectId, null, null, devId);
        assertThat(result).hasSize(3);
    }

    @Test
    void searchWithAllFilters() {
        // Critical + To Do + unassigned
        List<Issue> result = queryRepo.search(projectId, IssuePriority.MINOR, IssueStatus.TO_DO, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSummary()).isEqualTo("Minor task");
    }

    // ---------- Helpers ----------

    private void createIssue(Project project, String summary, IssuePriority priority,
                             IssueType type, State state, User assignee) {
        Issue issue = new Issue(project.generateIssueKey(), summary,
                "Description", project.getProjectLead(), type, priority, state);
        issue.setProject(project);
        if (assignee != null) {
            issue.assignTo(assignee);
        }
        issueRepo.save(issue);
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