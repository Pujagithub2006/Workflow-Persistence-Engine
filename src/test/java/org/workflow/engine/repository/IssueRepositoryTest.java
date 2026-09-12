package org.workflow.engine.repository;

import org.workflow.engine.domain.enums.*;
import org.workflow.engine.domain.model.*;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.repositories.IssueRepository;
import org.workflow.engine.testutil.TestCleanup;
import org.workflow.engine.testutil.TestData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class IssueRepositoryTest {

    private final IssueRepository issueRepo = new IssueRepository();

    private Project project;
    private Workflow workflow;
    private User reporter;

    @BeforeEach
    void setup() {
        TestCleanup.all();
        User admin = TestData.user(UserRole.ADMIN);
        User lead = TestData.user(UserRole.PROJECT_LEAD);
        reporter = TestData.user(UserRole.DEVELOPER);
        Workspace ws = TestData.workspace(admin);
        workflow = TestData.workflow();
        project = TestData.project(ws, workflow, lead);
    }

    @AfterAll
    static void shutdown() {
        JpaUtil.shutdown();
    }

    @Test
    void saveInsertsIssue() {
        Issue saved = TestData.issue(project, reporter, workflow);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getIssueKey()).isNotNull();
    }

    @Test
    void findByKeyReturnsIssue() {
        Issue saved = TestData.issue(project, reporter, workflow);

        assertThat(issueRepo.findByKey(saved.getIssueKey().getValue())).isPresent();
    }

    @Test
    void findByProjectReturnsIssues() {
        TestData.issue(project, reporter, workflow);
        TestData.issue(project, reporter, workflow);
        TestData.issue(project, reporter, workflow);

        assertThat(issueRepo.findByProject(project.getId())).hasSize(3);
    }

    @Test
    void reporterAndStateArePersisted() {
        Issue saved = TestData.issue(project, reporter, workflow);
        Issue loaded = issueRepo.findByKey(saved.getIssueKey().getValue()).orElseThrow();

        assertThat(loaded.getReporter().getUserId()).isEqualTo(reporter.getUserId());
        assertThat(loaded.getCurrentState()).isNotNull();
    }

    @Test
    void assigneeIsPersisted() {
        Issue issue = TestData.issue(project, reporter, workflow);
        issue.assignTo(reporter);
        issueRepo.save(issue);

        Issue loaded = issueRepo.findByKey(issue.getIssueKey().getValue()).orElseThrow();
        assertThat(loaded.getAssignee()).isNotNull();
        assertThat(loaded.getAssignee().getUserId()).isEqualTo(reporter.getUserId());
    }

    @Test
    void commentsAreCascadePersisted() {
        Issue issue = TestData.issue(project, reporter, workflow);
        issue.addComment("First comment", reporter);
        issue.addComment("Second comment", reporter);
        issueRepo.save(issue);

        Issue loaded = issueRepo.findByKey(issue.getIssueKey().getValue()).orElseThrow();
        assertThat(loaded.getComments()).hasSize(2);
    }

    @Test
    void deletingIssueRemovesComments() {
        Issue issue = TestData.issue(project, reporter, workflow);
        issue.addComment("Will be deleted", reporter);
        issueRepo.save(issue);
        String key = issue.getIssueKey().getValue();

        issueRepo.delete(issue.getId());

        assertThat(issueRepo.findByKey(key)).isEmpty();
    }

    @Test
    void issueKeyIncrements() {
        Issue i1 = TestData.issue(project, reporter, workflow);
        Project reloaded = new org.workflow.engine.persistence.repositories.ProjectRepository()
                .findByKey(project.getKey()).orElseThrow();
        Issue i2 = TestData.issue(reloaded, reporter, workflow);

        assertThat(i1.getIssueKey().getValue()).isNotEqualTo(i2.getIssueKey().getValue());
    }

    @Test
    void duplicateKeyFails() {
        Issue saved = TestData.issue(project, reporter, workflow);

        State state = workflow.getInitialState();
        Issue duplicate = new Issue(saved.getIssueKey(),
                "Dup", "Test", reporter, IssueType.TASK, IssuePriority.MAJOR, state);
        duplicate.setProject(project);

        assertThatThrownBy(() -> issueRepo.save(duplicate))
                .isInstanceOf(RuntimeException.class);
    }
}