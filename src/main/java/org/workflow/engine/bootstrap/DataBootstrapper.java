package org.workflow.engine.bootstrap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.workflow.engine.domain.enums.*;
import org.workflow.engine.domain.model.*;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.JpaUtil;
import org.workflow.engine.persistence.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBootstrapper {
    private static final Logger logger = LoggerFactory.getLogger(DataBootstrapper.class);

    private final UserRepository userRepo = new UserRepository();
    private final WorkspaceRepository workspaceRepo = new WorkspaceRepository();
    private final ProjectRepository projectRepo = new ProjectRepository();
    private final IssueRepository issueRepo = new IssueRepository();

    public void bootstrap() {
        logger.info("=== JPA Bootstrap ===");

        // 1. Users
        User admin = userRepo.save(new User("admin",
                new Email("admin@example.com"), "Admin", UserRole.ADMIN));
        User lead = userRepo.save(new User("johndoe",
                new Email("john@example.com"), "John Doe", UserRole.PROJECT_LEAD));
        User dev = userRepo.save(new User("janesmith",
                new Email("jane@example.com"), "Jane Smith", UserRole.DEVELOPER));
        User tester = userRepo.save(new User("bob",
                new Email("bob@example.com"), "Bob", UserRole.TESTER));
        logger.info("Inserted {} users", 4);

        // 2. Workspace
        Workspace workspace = new Workspace("Acme Corp", "Main workspace", admin);
        workspace.addMember(lead);
        workspace.addMember(dev);
        workspace.addMember(tester);
        workspace = workspaceRepo.save(workspace);
        logger.info("Saved workspace: {}", workspace.getName());

        // 3. Project
        Project project = new Project("ACME", "Acme Project",
                "Main project", lead);
        project.setWorkspace(workspace);
        project.addTeamMember(dev);
        project.addTeamMember(tester);
        project = projectRepo.save(project);
        logger.info("Saved project: {}", project.getKey());

        // 4. Workflow + states (in one transaction via cascade)
        Workflow workflow = new Workflow("Standard Workflow", "Default workflow");
        State todo = new State("To Do", "Initial state", IssueStatus.TO_DO);
        State inProgress = new State("In Progress", "Working", IssueStatus.IN_PROGRESS);
        State done = new State("Done", "Complete", IssueStatus.DONE);

        workflow.addState(todo);
        workflow.addState(inProgress);
        workflow.addState(done);
        workflow.setInitialState(todo);

        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction etx = em.getTransaction();

        try {
            etx.begin();

            em.persist(workflow);

            etx.commit();

        } catch (Exception e) {
            if (etx.isActive()) etx.rollback();
            throw e;
        } finally {
            em.close();
        }
        logger.info("Saved workflow with {} states", workflow.getStates().size());

        project.setWorkflow(workflow);
        project = projectRepo.save(project);

        // 5. Issues
        Issue issue1 = project.createIssue(
                "Implement authentication",
                "OAuth2 login flow",
                dev,
                IssueType.TASK,
                IssuePriority.MAJOR
        );

        issue1.assignTo(dev);

        issueRepo.save(issue1);

        Issue issue2 = project.createIssue(
                "Fix login page error",
                "500 error on valid credentials",
                tester,
                IssueType.BUG,
                IssuePriority.CRITICAL
        );

        issue2.assignTo(dev);

        issueRepo.save(issue2);

        logger.info("Saved {} issues", 2);

        // 6. Verify
        logger.info("=== Verification ===");
        logger.info("Users: {}", userRepo.findAll().size());
        logger.info("Workspaces: {}", workspaceRepo.findAll().size());
        logger.info("Project: {}", projectRepo.findByKey("ACME").orElseThrow().getName());
        logger.info("Issues in project: {}", issueRepo.findByProject(project.getId()).size());
        logger.info("=== Bootstrap Complete ===");
    }

    public static void main(String[] args) {
        try {
            new DataBootstrapper().bootstrap();
        } finally {
            JpaUtil.shutdown();
        }
    }
}