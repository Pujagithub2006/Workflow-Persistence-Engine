package org.workflow.engine.domain.model;

import org.workflow.engine.domain.enums.IssuePriority;
import org.workflow.engine.domain.enums.IssueType;
import org.workflow.engine.domain.valueobject.IssueKey;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Project {
    private Long id;
    private String key;
    private String name;
    private String description;
    private Workspace workspace;
    private Workflow workflow;
    private User projectLead;
    private Set<User> teamMembers;
    private Set<Issue> issues;
    private long nextIssueNumber;

    public Project(String key, String name, String description, User projectLead) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Project key cannot be null or empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        }
        if (projectLead == null) {
            throw new IllegalArgumentException("Project lead cannot be null");
        }
        this.key = key.toUpperCase();
        this.name = name;
        this.description = description;
        this.projectLead = projectLead;
        this.teamMembers = new HashSet<>();
        this.issues = new HashSet<>();
        this.nextIssueNumber = 1;
        this.teamMembers.add(projectLead);
    }

    public IssueKey generateIssueKey() {
        return new IssueKey(key, nextIssueNumber++); // refers to one of the parameterised contructor in IssueKey class
    }

    public Issue createIssue(String summary, String description, User reporter, IssueType type, IssuePriority priority) {
        if (workflow == null) {
            throw new IllegalStateException("Project must have a workflow assigned");
        }
        State initialState = workflow.getInitialState();
        if (initialState == null) {
            throw new IllegalStateException("Workflow does not have an initial state");
        }

        Issue issue = new Issue(generateIssueKey(), summary, description, reporter, type, priority, initialState);

        issues.add(issue);
        issue.setProject(this);
        return issue;
    }

    public void assignWorkflow(Workflow workflow) {
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow cannot be null");
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Cannot change workflow when project has issues");
        }
        this.workflow = workflow;
    }

    public void addTeamMember(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        teamMembers.add(user);
    }

    public void removeTeamMember(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.equals(projectLead)) {
            throw new IllegalStateException("Cannot remove project lead from team");
        }
        teamMembers.remove(user);
    }

    public Issue findIssueKey(IssueKey issueKey) {
        return issues.stream()
                .filter(i->i.getIssueKey().equals(issueKey))
                .findFirst()
                .orElse(null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKey() { return key; }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public Workspace getWorkspace() {
        return workspace;
    }
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public User getProjectLead() {
        return projectLead;
    }
    public void setProjectLead(User projectLead) {
        if (projectLead == null) {
            throw new IllegalArgumentException("Project lead cannot be null");
        }
        this.projectLead = projectLead;
    }

    public Set<User> getTeamMembers() {
        return new HashSet<>(teamMembers);
    }

    public Set<Issue> getIssues() {
        return new HashSet<>(issues);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id) || Objects.equals(key, project.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", key='" + key + "', name='" + name + "'}";
    }

}
