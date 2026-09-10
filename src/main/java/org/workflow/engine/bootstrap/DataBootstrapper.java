package org.workflow.engine.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workflow.engine.domain.enums.IssuePriority;
import org.workflow.engine.domain.enums.IssueStatus;
import org.workflow.engine.domain.enums.IssueType;
import org.workflow.engine.domain.enums.UserRole;
import org.workflow.engine.domain.model.*;
import org.workflow.engine.domain.valueobject.Email;

public class DataBootstrapper {
    private static final Logger logger = LoggerFactory.getLogger(DataBootstrapper.class); // factory method to create Logger for this class

    public void bootstrap() {
        logger.info("==== DOMAIN MODEL BOOTSTRAP ====");

        // create users
        User admin = new User("admin", new Email("admin@example.com"),
                "System Admin", UserRole.ADMIN);
        User lead = new User("pujanikam", new Email("puja.nikam@example.com"),
                "Puja Nikam", UserRole.PROJECT_LEAD);
        User dev = new User("payalpatil", new Email("payal.patil@example.com"),
                "Payal Patil", UserRole.DEVELOPER);
        User tester = new User("kunaldeshmukh", new Email("kunal.deshmukh@example.com"),
                "Kunal Deshmukh", UserRole.TESTER);

        logger.info("1. Created {} users", 4);

        // create workspace
        Workspace workspace = new Workspace("Diyaja Corp.", "Main workspace", admin);
        workspace.addMember(lead);
        workspace.addMember(dev);
        workspace.addMember(tester);

        logger.info("2. Created workspace: {} ({} members)", workspace.getName(), workspace.getMembers().size());

        // create workflow with states and transitions
        Workflow workflow = createDefaultWorkflow();
        logger.info("3. Created workflow: {} ({} states)", workflow.getName(), workflow.getStates().size());

        // create project
        Project project = new Project("Diyaja", "Diyaja Project", "Main Diyaja project", lead);
        project.assignWorkflow(workflow);
        workspace.addProject(project);
        project.addTeamMember(dev);
        project.addTeamMember(tester);

        logger.info("4. Created project: {}", project.getKey());

        // create issues
        Issue issue1 = project.createIssue("Implement user authentication","Add OAuth2-based authentication", dev, IssueType.TASK, IssuePriority.MAJOR);
        issue1.assignTo(dev);

        Issue issue2 = project.createIssue("Fix login page error","Users get 500 error with valid credentials", tester, IssueType.BUG, IssuePriority.CRITICAL);
        issue2.assignTo(dev);

        logger.info("5. Created {} issues", project.getIssues().size());

        // create comments
        issue1.addComment("Started working on this", dev);
        issue1.addComment("Please use the new security library", lead);
        issue2.addComment("Found null pointer in validation", tester);

        logger.info("6. Added comments");

        // simulate workflow transitions
        State inProgress = workflow.getStateByName("In Progress");
        State inReview = workflow.getStateByName("In Review");
        State done = workflow.getStateByName("Done");

        issue1.transitionTo(inProgress, dev);
        issue1.transitionTo(inReview, dev);
        issue1.transitionTo(done, lead);

        logger.info("7.1. Transitioned {} through workflow", issue1.getIssueKey());

        issue2.updatePriority(IssuePriority.BLOCKER, lead);
        logger.info("7.2. Updated {} priority to BLOCKER", issue2.getIssueKey());

        // verify object graph
        verifyObjectGraph(workspace);

        // display summary
        logger.info("9. === Bootstrap Complete ===");
        logger.info("9.1. Workspace: {} ({} members, {} projects)",
                workspace.getName(),
                workspace.getMembers().size(),
                workspace.getProjects().size());
        logger.info("9.2. Project: {} ({} issues, {} team members)",
                project.getKey(),
                project.getIssues().size(),
                project.getTeamMembers().size());
        logger.info("9.3. Issue {}: {} audit entries, {} comments",
                issue1.getIssueKey(),
                issue1.getAuditLogs().size(),
                issue1.getComments().size());
        logger.info("9.4. Issue {} is resolved: {}",
                issue1.getIssueKey(), issue1.isResolved());
    }

    private Workflow createDefaultWorkflow() {
        State todo = new State("To Do", "Initial state", IssueStatus.TO_DO);
        State inProgress = new State("In Progress", "Work started", IssueStatus.IN_PROGRESS);
        State inReview = new State("In Review", "Awaiting review", IssueStatus.IN_REVIEW);
        State done = new State("Done", "Complete", IssueStatus.DONE);
        State closed = new State("Closed", "Closed", IssueStatus.CLOSED);

        Workflow workflow = new Workflow("Standard Workflow", "Default workflow");
        workflow.addState(todo);
        workflow.addState(inProgress);
        workflow.addState(inReview);
        workflow.addState(done);
        workflow.addState(closed);
        workflow.setInitialState(todo);

        new Transition("Start Progress", "Begin work", todo, inProgress);
        new Transition("Start Review", "Move to review", inProgress, inReview);
        new Transition("Complete", "Complete work", inReview, done);
        new Transition("Close", "Close issue", done, closed);

        return workflow;
    }

    private void verifyObjectGraph(Workspace workspace) {
        logger.info("8.1. Verifying Object Graph");

        if (workspace.getMembers().isEmpty()) throw new AssertionError("No members");
        if (workspace.getProjects().isEmpty()) throw new AssertionError("No projects");

        for (Project project : workspace.getProjects()) {
            if (project.getWorkflow() == null) throw new AssertionError("No workflow");
            for (Issue issue : project.getIssues()) {
                if (issue.getCurrentState() == null) throw new AssertionError("No state");
                if (issue.getReporter() == null) throw new AssertionError("No reporter");
                if (issue.getAuditLogs().isEmpty()) throw new AssertionError("No audit logs");
                for (Comment c : issue.getComments()) {
                    if (c.getAuthor() == null) throw new AssertionError("No author");
                }
            }
        }

        logger.info("8.2. Object graph verification passed");
    }

    public static void main(String[] args) {
        new DataBootstrapper().bootstrap();
    }
}
