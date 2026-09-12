package org.workflow.engine.integration;

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

class WorkflowIntegrationTest {

    private final IssueRepository issueRepo = new IssueRepository();

    @BeforeEach
    void clean() {
        TestCleanup.all();
    }

    @AfterAll
    static void shutdown() {
        JpaUtil.shutdown();
    }

    @Test
    void completeIssueLifecyclePersistsEverything() {
        // Setup
        User admin = TestData.user(UserRole.ADMIN);
        User lead = TestData.user(UserRole.PROJECT_LEAD);
        User dev = TestData.user(UserRole.DEVELOPER);
        Workspace ws = TestData.workspace(admin);
        Workflow wf = TestData.workflow();
        Project project = TestData.project(ws, wf, lead);

        // Create issue
        Issue issue = TestData.issue(project, dev, wf);
        String key = issue.getIssueKey().getValue();

        // Assign
        issue.assignTo(dev);
        issueRepo.save(issue);

        // Add comments
        issue.addComment("Starting work", dev);
        issue.addComment("Update in progress", dev);
        issueRepo.save(issue);

        // Transition through workflow
        State inProgress = wf.getStateByName("In Progress");
        State done = wf.getStateByName("Done");

        issue.transitionTo(inProgress, dev);
        issueRepo.save(issue);

        issue.transitionTo(done, dev);
        issueRepo.save(issue);

        // Reload and verify everything
        Issue loaded = issueRepo.findByKey(key).orElseThrow();

        assertThat(loaded.getAssignee()).isNotNull();
        assertThat(loaded.getCurrentState().getName()).isEqualTo("Done");
        assertThat(loaded.getComments()).hasSize(2);
        assertThat(loaded.getAuditLogs()).isNotEmpty();
        assertThat(loaded.isResolved()).isTrue();
    }

    @Test
    void issueMovesThroughMultipleProjectsIndependently() {
        User admin = TestData.user(UserRole.ADMIN);
        User lead = TestData.user(UserRole.PROJECT_LEAD);
        User dev = TestData.user(UserRole.DEVELOPER);
        Workspace ws = TestData.workspace(admin);
        Workflow wf = TestData.workflow();

        Project p1 = TestData.project(ws, wf, lead);
        Project p2 = TestData.project(ws, wf, lead);

        Issue i1 = TestData.issue(p1, dev, wf);
        Issue i2 = TestData.issue(p2, dev, wf);

        // Transition only i1
        i1.transitionTo(wf.getStateByName("In Progress"), dev);
        issueRepo.save(i1);

        Issue loadedI1 = issueRepo.findByKey(i1.getIssueKey().getValue()).orElseThrow();
        Issue loadedI2 = issueRepo.findByKey(i2.getIssueKey().getValue()).orElseThrow();

        assertThat(loadedI1.getCurrentState().getName()).isEqualTo("In Progress");
        assertThat(loadedI2.getCurrentState().getName()).isEqualTo("To Do");
    }

    @Test
    void dataSurvivesAcrossEntityManagers() {
        User admin = TestData.user(UserRole.ADMIN);
        User lead = TestData.user(UserRole.PROJECT_LEAD);
        User dev = TestData.user(UserRole.DEVELOPER);
        Workspace ws = TestData.workspace(admin);
        Workflow wf = TestData.workflow();
        Project project = TestData.project(ws, wf, lead);
        Issue issue = TestData.issue(project, dev, wf);
        String key = issue.getIssueKey().getValue();

        // Each findById opens and closes its own EntityManager
        assertThat(issueRepo.findByKey(key)).isPresent();
        assertThat(issueRepo.findByKey(key)).isPresent();
        assertThat(issueRepo.findByKey(key)).isPresent();
    }
}