package org.workflow.engine.testutil;

import org.workflow.engine.domain.enums.*;
import org.workflow.engine.domain.model.*;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.TransactionManager;
import org.workflow.engine.persistence.repositories.*;

public class TestData {

    private static final UserRepository userRepo = new UserRepository();
    private static final WorkspaceRepository workspaceRepo = new WorkspaceRepository();
    private static final ProjectRepository projectRepo = new ProjectRepository();
    private static final IssueRepository issueRepo = new IssueRepository();

    private static int counter = 0;

    public static User user() {
        return user(UserRole.DEVELOPER);
    }

    public static User user(UserRole role) {
        int id = ++counter;
        return userRepo.save(new User("u" + id,
                new Email("u" + id + "@test.com"),
                "User " + id, role));
    }

    public static Workspace workspace(User owner) {
        int id = ++counter;
        return workspaceRepo.save(new Workspace("WS" + id, "Test workspace", owner));
    }

    public static Workflow workflow() {
        int id = ++counter;
        Workflow wf = new Workflow("WF" + id, "Test workflow");
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
        return wf;
    }

    public static Project project(Workspace ws, Workflow wf, User lead) {
        int id = ++counter;
        Project p = new Project("P" + id, "Project " + id, "Test project", lead);
        p.setWorkflow(wf);
        ws.addProject(p);
        workspaceRepo.save(ws);
        return projectRepo.findByKey("P" + id).orElseThrow();
    }

    public static Issue issue(Project project, User reporter, Workflow wf) {
        State initialState = wf.getInitialState();
        Issue issue = new Issue(project.generateIssueKey(),
                "Issue " + counter, "Test issue", reporter,
                IssueType.TASK, IssuePriority.MAJOR, initialState);
        issue.setProject(project);
        return issueRepo.save(issue);
    }
}