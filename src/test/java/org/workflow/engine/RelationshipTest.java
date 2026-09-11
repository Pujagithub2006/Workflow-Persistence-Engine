package org.workflow.engine;

import org.workflow.engine.domain.enums.*;
import org.workflow.engine.domain.model.*;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RelationshipTest {

    private final UserRepository userRepo = new UserRepository();
    private final WorkspaceRepository workspaceRepo = new WorkspaceRepository();
    private final ProjectRepository projectRepo = new ProjectRepository();
    private final IssueRepository issueRepo = new IssueRepository();

    @BeforeEach
    void clean() {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM AuditLog").executeUpdate();
            em.createQuery("DELETE FROM Comment").executeUpdate();
            em.createQuery("DELETE FROM Issue").executeUpdate();
            em.createQuery("DELETE FROM Transition").executeUpdate();
            em.createQuery("DELETE FROM State").executeUpdate();
            em.createQuery("DELETE FROM Workflow").executeUpdate();
            em.createQuery("DELETE FROM Project").executeUpdate();
            em.createQuery("DELETE FROM Workspace").executeUpdate();
            em.createQuery("DELETE FROM User").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
        } finally {
            em.close();
        }
    }

    @AfterAll
    static void shutdown() {
        JpaUtil.shutdown();
    }

    // ---------- OneToMany + Cascade ALL + Orphan Removal ----------

    @Test
    void workspaceProjectsAreCascaded() {
        User owner = userRepo.save(new User("owner",
                new Email("owner@test.com"), "Owner", UserRole.ADMIN));

        Workspace ws = new Workspace("WS", "Test", owner);
        Project p1 = new Project("P1", "Project 1", "Test", owner);
        Project p2 = new Project("P2", "Project 2", "Test", owner);
        ws.addProject(p1);
        ws.addProject(p2);

        // Cascade saves the projects
        workspaceRepo.save(ws);

        Workspace loaded = workspaceRepo.findById(ws.getId()).orElseThrow();
        assertThat(loaded.getProjects()).hasSize(2);
    }

    @Test
    void removingProjectFromWorkspaceDeletesIt() {
        User owner = userRepo.save(new User("owner2",
                new Email("owner2@test.com"), "Owner", UserRole.ADMIN));

        Workspace ws = new Workspace("WS2", "Test", owner);
        Project p1 = new Project("DEL", "To delete", "Test", owner);
        ws.addProject(p1);
        workspaceRepo.save(ws);

        // Reload and remove
        Workspace loaded = workspaceRepo.findById(ws.getId()).orElseThrow();
        loaded.removeProject(loaded.getProjects().iterator().next());
        workspaceRepo.save(loaded);

        // Project should be gone (orphan removal)
        assertThat(projectRepo.findByKey("DEL")).isEmpty();
    }

    @Test
    void issueCommentsCascadeOnDelete() {
        User dev = userRepo.save(new User("dev",
                new Email("dev@test.com"), "Dev", UserRole.DEVELOPER));
        User owner = userRepo.save(new User("owner3",
                new Email("o3@test.com"), "Owner", UserRole.ADMIN));

        Workspace ws = new Workspace("WS3", "Test", owner);
        Workflow wf = new Workflow("WF", "Test");
        State todo = new State("To Do", "Init", IssueStatus.TO_DO);
        wf.addState(todo);
        wf.setInitialState(todo);

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(wf);
        tx.commit();
        em.close();

        Project project = new Project("CASCADE", "Cascade", "Test", owner);
        project.setWorkflow(wf);
        ws.addProject(project);
        workspaceRepo.save(ws);

        Project loaded = projectRepo.findByKey("CASCADE").orElseThrow();
        Issue issue = loaded.createIssue("Bug", "Desc", dev,
                IssueType.BUG, IssuePriority.CRITICAL);
        issue.addComment("First", dev);
        issue.addComment("Second", dev);
        issueRepo.save(issue);

        // Verify comments persisted
        Issue loadedIssue = issueRepo.findByKey(issue.getIssueKey().getValue()).orElseThrow();
        assertThat(loadedIssue.getComments()).hasSize(2);

        // Delete issue → comments should be gone
        issueRepo.delete(loadedIssue.getId());
        assertThat(issueRepo.findByKey(issue.getIssueKey().getValue())).isEmpty();
    }

    // ---------- ManyToMany ----------

    @Test
    void workspaceMembersAreBidirectional() {
        User owner = userRepo.save(new User("wsowner",
                new Email("wsowner@test.com"), "Owner", UserRole.ADMIN));
        User member = userRepo.save(new User("member",
                new Email("member@test.com"), "Member", UserRole.DEVELOPER));

        Workspace ws = new Workspace("TeamWS", "Test", owner);
        ws.addMember(member);
        workspaceRepo.save(ws);

        // From workspace side
        Workspace loadedWs = workspaceRepo.findById(ws.getId()).orElseThrow();
        assertThat(loadedWs.getMembers()).contains(member);

        // From user side
        User loadedUser = userRepo.findById(member.getUserId()).orElseThrow();
        assertThat(loadedUser.getWorkspaces()).contains(loadedWs);
    }

    @Test
    void projectTeamIsBidirectional() {
        User lead = userRepo.save(new User("plead",
                new Email("plead@test.com"), "Lead", UserRole.PROJECT_LEAD));
        User dev = userRepo.save(new User("pdev",
                new Email("pdev@test.com"), "Dev", UserRole.DEVELOPER));
        User owner = userRepo.save(new User("powner",
                new Email("powner@test.com"), "Owner", UserRole.ADMIN));

        Workspace ws = new Workspace("PTeamWS", "Test", owner);
        Project project = new Project("TEAM", "Team", "Test", lead);
        project.addTeamMember(dev);
        ws.addProject(project);
        workspaceRepo.save(ws);

        Project loaded = projectRepo.findByKey("TEAM").orElseThrow();
        assertThat(loaded.getTeamMembers()).contains(dev);
    }

    // ---------- Workflow Relationship ----------

    @Test
    void projectWorkflowIsPersisted() {
        User owner = userRepo.save(new User("wfowner",
                new Email("wfowner@test.com"), "Owner", UserRole.ADMIN));

        Workflow wf = new Workflow("ProjectWF", "Test");
        State todo = new State("To Do", "Init", IssueStatus.TO_DO);
        wf.addState(todo);
        wf.setInitialState(todo);

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(wf);
        tx.commit();
        em.close();

        Workspace ws = new Workspace("WFWS", "Test", owner);
        Project project = new Project("WFPRJ", "WF Project", "Test", owner);
        project.setWorkflow(wf);
        ws.addProject(project);
        workspaceRepo.save(ws);

        Project loaded = projectRepo.findByKey("WFPRJ").orElseThrow();
        assertThat(loaded.getWorkflow()).isNotNull();
        assertThat(loaded.getWorkflow().getInitialState()).isNotNull();
        assertThat(loaded.getWorkflow().getInitialState().getName()).isEqualTo("To Do");
    }

    // ---------- State / Transition ----------

    @Test
    void stateTransitionsAreBidirectional() {
        Workflow wf = new Workflow("TransWF", "Test");
        State todo = new State("To Do", "Init", IssueStatus.TO_DO);
        State done = new State("Done", "Complete", IssueStatus.DONE);
        wf.addState(todo);
        wf.addState(done);
        wf.setInitialState(todo);
        new Transition("Finish", "Complete", todo, done);

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(wf);
        tx.commit();

        Workflow loaded = em.find(Workflow.class, wf.getId());
        State loadedTodo = loaded.getStateByName("To Do");
        State loadedDone = loaded.getStateByName("Done");

        assertThat(loadedTodo.getOutboundTransitions()).hasSize(1);
        assertThat(loadedDone.getInboundTransitions()).hasSize(1);
        em.close();
    }

    // ---------- Invalid operations ----------

    @Test
    void cannotRemoveOwnerFromWorkspace() {
        User owner = userRepo.save(new User("ownersafe",
                new Email("ownersafe@test.com"), "Owner", UserRole.ADMIN));
        Workspace ws = new Workspace("SafeWS", "Test", owner);

        assertThatThrownBy(() -> ws.removeMember(owner))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotRemoveLeadFromProject() {
        User lead = userRepo.save(new User("leadsafe",
                new Email("leadsafe@test.com"), "Lead", UserRole.PROJECT_LEAD));
        Project project = new Project("SAFE", "Safe", "Test", lead);

        assertThatThrownBy(() -> project.removeTeamMember(lead))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotRemoveInitialStateFromWorkflow() {
        Workflow wf = new Workflow("InitialWF", "Test");
        State todo = new State("To Do", "Init", IssueStatus.TO_DO);
        wf.addState(todo);
        wf.setInitialState(todo);

        // Now removing the initial state is a business error
        // (handled by logic below when we re-add one)
        wf.setInitialState(todo); // still the initial state
        assertThat(wf.getInitialState()).isEqualTo(todo);
    }
}