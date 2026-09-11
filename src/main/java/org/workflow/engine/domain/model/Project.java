package org.workflow.engine.domain.model;

import jakarta.persistence.*;
import org.workflow.engine.domain.enums.IssuePriority;
import org.workflow.engine.domain.enums.IssueType;
import org.workflow.engine.domain.valueobject.IssueKey;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key", nullable = false, unique = true, length = 10)
    private String key;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_lead_id", nullable = false)
    private User projectLead;

    @ManyToMany
    @JoinTable(
            name = "project_team",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> teamMembers = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Issue> issues = new HashSet<>();

    @Column(name = "next_issue_number", nullable = false)
    private long nextIssueNumber = 1;


    protected Project() {
        this.teamMembers = new HashSet<>();
        this.issues = new HashSet<>();
    }

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

        addIssue(issue);
        return issue;
    }

    public void addIssue(Issue issue) {
        if(issue == null) throw new IllegalArgumentException("Issue cannot be null");
        if(issues.add(issue)) issue.setProject(this);
    }

    public void removeIssue(Issue issue) {
        if(issue == null) throw new IllegalArgumentException("Issue cannot be null");
        if(issues.remove(issue)) issue.setProject(null);
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
    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
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
