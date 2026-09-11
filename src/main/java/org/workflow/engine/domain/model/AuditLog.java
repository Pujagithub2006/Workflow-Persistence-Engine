package org.workflow.engine.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(name = "action", nullable = false, length = 255)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id")
    private User performedBy;

    @Column(name = "field", length = 50)
    private String field;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public AuditLog(Issue issue, String action, User performedBy, String field) {
        if (issue == null) {
            throw new IllegalArgumentException("Issue cannot be null");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Action cannot be null or empty");
        }
        this.issue = issue;
        this.action = action;
        this.performedBy = performedBy;
        this.field = field;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Issue getIssue() {
        return issue;
    }

    public String getAction() {
        return action;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    public String getField() {
        return field;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AuditLog{action='" + action + "', by=" +
                (performedBy != null ? performedBy.getUsername() : "system") + "}";
    }
}
