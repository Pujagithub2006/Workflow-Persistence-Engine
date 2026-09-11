package org.workflow.engine.bootstrap;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.workflow.engine.domain.enums.*;
import org.workflow.engine.domain.model.*;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.HibernateUtil;
import org.workflow.engine.persistence.SchemaInitializer;
import org.workflow.engine.persistence.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBootstrapper {
    private static final Logger logger = LoggerFactory.getLogger(DataBootstrapper.class);

    private final UserRepository userRepo = new UserRepository();
    private final WorkspaceRepository workspaceRepo = new WorkspaceRepository();
    private final ProjectRepository projectRepo = new ProjectRepository();
    private final StateRepository stateRepo = new StateRepository();
    private final IssueRepository issueRepo = new IssueRepository();

    public void bootstrap() {
        logger.info("=== Hibernate Bootstrap ===");

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

        // 2. Workflow + states (in one transaction via cascade)
        Workflow workflow = new Workflow("Standard Workflow", "Default workflow");
        State todo = new State("To Do", "Initial state", IssueStatus.TO_DO);
        State inProgress = new State("In Progress", "Working", IssueStatus.IN_PROGRESS);
        State done = new State("Done", "Complete", IssueStatus.DONE);

        workflow.addState(todo);
        workflow.addState(inProgress);
        workflow.addState(done);
        workflow.setInitialState(todo);

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            session.persist(workflow);

            tx.commit();

        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
        logger.info("Saved workflow with {} states", workflow.getStates().size());

        // 3. Workspace
        Workspace workspace = new Workspace("Acme Corp", "Main workspace", admin);
        workspace.addMember(lead);
        workspace.addMember(dev);
        workspace.addMember(tester);
        workspace = workspaceRepo.save(workspace);
        logger.info("Saved workspace: {}", workspace.getName());

        // 4. Project
        Project project = new Project("ACME", "Acme Project",
                "Main project", lead);
        project.setWorkspace(workspace);
        project.addTeamMember(dev);
        project.addTeamMember(tester);
        project = projectRepo.save(project);
        logger.info("Saved project: {}", project.getKey());

        // 5. Issues
        Issue issue1 = new Issue(project.generateIssueKey(),
                "Implement authentication",
                "OAuth2 login flow", dev,
                IssueType.TASK, IssuePriority.MAJOR, todo);
        issue1.setProject(project);
        issue1.assignTo(dev);
        issueRepo.save(issue1);

        Issue issue2 = new Issue(project.generateIssueKey(),
                "Fix login page error",
                "500 error on valid credentials", tester,
                IssueType.BUG, IssuePriority.CRITICAL, todo);
        issue2.setProject(project);
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
            HibernateUtil.shutdown();
        }
    }
}