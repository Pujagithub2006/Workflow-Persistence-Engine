package org.workflow.engine.domain.model;

import jakarta.persistence.*;
import org.workflow.engine.domain.enums.IssuePriority;
import org.workflow.engine.domain.enums.IssueStatus;
import org.workflow.engine.domain.enums.IssueType;
import org.workflow.engine.domain.value.IssueKeyConverter;
import org.workflow.engine.domain.valueobject.IssueKey;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = IssueKeyConverter.class)
    @Column(name = "issue_key", nullable = false, unique = true, length = 20)
    private IssueKey issueKey;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 20)
    private IssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private IssuePriority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id", nullable = false)
    private State currentState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuditLog> auditLogs = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public Issue(IssueKey issueKey, String summary, String description,
                 User reporter, IssueType type, IssuePriority priority, State initialState) {
        if (issueKey == null) {
            throw new IllegalArgumentException("Issue key cannot be null");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Summary cannot be null or empty");
        }
        if (reporter == null) {
            throw new IllegalArgumentException("Reporter cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Issue type cannot be null");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null");
        }
        if (initialState == null) {
            throw new IllegalArgumentException("Initial state cannot be null");
        }
        this.issueKey = issueKey;
        this.summary = summary;
        this.description = description;
        this.reporter = reporter;
        this.issueType = type;
        this.priority = priority;
        this.currentState = initialState;
        this.comments = new ArrayList<>();
        this.auditLogs = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        addAuditLog("Issue created", reporter, null);
    }


    @PrePersist
    void onPersist() {
        if(createdAt == null) createdAt = LocalDateTime.now();
    }

    public void assignTo(User assignee) {
        this.assignee = assignee;
        addAuditLog("Issue Assigned to ", assignee, "assignee");
    }

    public void addComment(String commentContent, User author) {
        if (commentContent == null || commentContent.isBlank()) {
            throw new IllegalArgumentException("Comment content cannot be null or empty");
        }
        if (author == null) {
            throw new IllegalArgumentException("Author cannot be null");
        }

        Comment comment = new Comment(commentContent, author, this);
        comments.add(comment);

        addAuditLog("Comment added", author, "comment");
    }

    public void transitionTo(State toState, User user) {
        if (toState == null) {
            throw new IllegalArgumentException("State to be reached cannot be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if(!currentState.canTransitionTo(toState)) {
            throw new IllegalStateException("Cannot transition from " + currentState.getName() + "to " + toState.getName());
        }

        String oldState = currentState.getName();
        this.currentState = toState;

        if(toState.getStatus() == IssueStatus.DONE) {
            this.resolvedAt = LocalDateTime.now();
        }

        addAuditLog("State" + oldState + " -> " + toState.getName(), user, "state");
    }

    public void updatePriority(IssuePriority newPriority, User user) {
        if (newPriority == null) {
            throw new IllegalArgumentException("Priority to set cannot be null");
        }
        this.priority = newPriority;
        addAuditLog("Priority changed to " + newPriority, user, "priority");
    }

    private void addAuditLog(String action, User performedBy, String field) {
        auditLogs.add(new AuditLog(this, action, performedBy, field));
    }

    public boolean isResolved() {
        return currentState.getStatus() == IssueStatus.DONE ||
                currentState.getStatus() == IssueStatus.CLOSED;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public IssueKey getIssueKey() {
        return issueKey;
    }

    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public User getReporter() {
        return reporter;
    }

    public User getAssignee() {
        return assignee;
    }

    public IssueType getIssueType() {
        return issueType;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public State getCurrentState() {
        return currentState;
    }

    public Project getProject() {
        return project;
    }
    public void setProject(Project project) {
        this.project = project;
    }

    public List<Comment> getComments() {
        return new ArrayList<>(comments);
    }

    public List<AuditLog> getAuditLogs() {
        return new ArrayList<>(auditLogs);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Issue issue = (Issue) o;
        return Objects.equals(id, issue.id) || Objects.equals(issueKey, issue.issueKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueKey);
    }

    @Override
    public String toString() {
        return "Issue{key=" + issueKey + ", summary='" + summary + "', state=" +
                currentState.getName() + "}";
    }


}
