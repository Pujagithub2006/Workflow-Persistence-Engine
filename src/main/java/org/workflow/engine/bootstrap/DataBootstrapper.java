package org.workflow.engine.bootstrap;

import org.workflow.engine.domain.enums.*;
import org.workflow.engine.domain.model.*;
import org.workflow.engine.domain.valueobject.Email;
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
        logger.info("=== JDBC Bootstrap ===");

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
        workspaceRepo.save(workspace);
        logger.info("Inserted workspace: {}", workspace.getName());

        // 3. States (attach to a workflow created implicitly — see note)
        // For simplicity, create states without a workflow row in this issue
        State todo = stateRepo.save(
                new State("To Do", "Initial state", IssueStatus.TO_DO), 1L);
        State inProgress = stateRepo.save(
                new State("In Progress", "Working", IssueStatus.IN_PROGRESS), 1L);
        State inReview = stateRepo.save(
                new State("In Review", "Review", IssueStatus.IN_REVIEW), 1L);
        State done = stateRepo.save(
                new State("Done", "Complete", IssueStatus.DONE), 1L);
        logger.info("Inserted {} states", 4);

        // 4. Project
        Project project = new Project("ACME", "Acme Project",
                "Main Acme project", lead);
        project.setWorkspace(workspace);
        project.addTeamMember(dev);
        project.addTeamMember(tester);
        projectRepo.save(project);
        logger.info("Inserted project: {}", project.getKey());

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
        logger.info("Inserted {} issues", 2);

        // 6. Verify persistence
        logger.info("=== Verification ===");
        logger.info("Users in DB: {}", userRepo.findAll().size());
        logger.info("Workspaces in DB: {}", workspaceRepo.findAll().size());
        logger.info("Project loaded: {}", projectRepo.findByKey("ACME").orElseThrow());
        logger.info("Issues in project: {}",
                issueRepo.findByProject(project.getId()).size());

        logger.info("=== Bootstrap Complete ===");
    }

    public static void main(String[] args) {
        SchemaInitializer.initialize();
        new DataBootstrapper().bootstrap();
    }
}