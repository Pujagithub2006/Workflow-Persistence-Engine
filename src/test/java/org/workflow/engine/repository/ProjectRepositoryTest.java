package org.workflow.engine.repository;

import org.workflow.engine.domain.model.*;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.repositories.ProjectRepository;
import org.workflow.engine.testutil.TestCleanup;
import org.workflow.engine.testutil.TestData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProjectRepositoryTest {

    private final ProjectRepository projectRepo = new ProjectRepository();

    private Workspace workspace;
    private Workflow workflow;
    private User lead;

    @BeforeEach
    void setup() {
        TestCleanup.all();
        User admin = TestData.user(org.workflow.engine.domain.enums.UserRole.ADMIN);
        lead = TestData.user(org.workflow.engine.domain.enums.UserRole.PROJECT_LEAD);
        workspace = TestData.workspace(admin);
        workflow = TestData.workflow();
    }

    @AfterAll
    static void shutdown() {
        JpaUtil.shutdown();
    }

    @Test
    void saveInsertsProject() {
        Project saved = TestData.project(workspace, workflow, lead);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getKey()).startsWith("P");
    }

    @Test
    void findByKeyReturnsProject() {
        Project saved = TestData.project(workspace, workflow, lead);

        assertThat(projectRepo.findByKey(saved.getKey())).isPresent();
    }

    @Test
    void findByKeyReturnsEmptyForMissing() {
        assertThat(projectRepo.findByKey("MISSING")).isEmpty();
    }

    @Test
    void findByWorkspaceReturnsProjects() {
        TestData.project(workspace, workflow, lead);
        TestData.project(workspace, workflow, lead);

        assertThat(projectRepo.findByWorkspace(workspace.getId())).hasSize(2);
    }

    @Test
    void workflowIsPersisted() {
        Project saved = TestData.project(workspace, workflow, lead);
        Project loaded = projectRepo.findByKey(saved.getKey()).orElseThrow();

        assertThat(loaded.getWorkflow()).isNotNull();
        assertThat(loaded.getWorkflow().getId()).isEqualTo(workflow.getId());
    }

    @Test
    void leadIsAutoAddedAsTeamMember() {
        Project saved = TestData.project(workspace, workflow, lead);
        Project loaded = projectRepo.findByKey(saved.getKey()).orElseThrow();

        assertThat(loaded.getTeamMembers()).contains(lead);
    }

    @Test
    void duplicateKeyFails() {
        Project saved = TestData.project(workspace, workflow, lead);

        Project duplicate = new Project(saved.getKey(), "Dup", "Test", lead);
        duplicate.setWorkflow(workflow);
        workspace.addProject(duplicate);

        assertThatThrownBy(() -> projectRepo.save(duplicate))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteRemovesProject() {
        Project saved = TestData.project(workspace, workflow, lead);

        projectRepo.delete(saved.getId());

        assertThat(projectRepo.findByKey(saved.getKey())).isEmpty();
    }
}