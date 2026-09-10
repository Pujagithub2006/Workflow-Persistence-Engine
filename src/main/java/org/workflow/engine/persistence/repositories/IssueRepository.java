package org.workflow.engine.persistence.repositories;

import org.workflow.engine.domain.enums.IssuePriority;
import org.workflow.engine.domain.enums.IssueType;
import org.workflow.engine.domain.model.Issue;
import org.workflow.engine.domain.model.State;
import org.workflow.engine.domain.model.User;
import org.workflow.engine.domain.valueobject.IssueKey;
import org.workflow.engine.persistence.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IssueRepository {
    private static final Logger logger = LoggerFactory.getLogger(IssueRepository.class);
    private final UserRepository userRepository = new UserRepository();
    private final StateRepository stateRepository = new StateRepository();
    private final ProjectRepository projectRepository = new ProjectRepository();

    public Issue save(Issue issue) {
        if (issue.getId() == null) {
            return insert(issue);
        }
        return update(issue);
    }

    private Issue insert(Issue issue) {
        String sql = """
            INSERT INTO issues (issue_key, summary, description, reporter_id, assignee_id,
                                issue_type, priority, state_id, project_id, resolved_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, issue.getIssueKey().getValue());
            stmt.setString(2, issue.getSummary());
            stmt.setString(3, issue.getDescription());
            stmt.setLong(4, issue.getReporter().getUserId());
            if (issue.getAssignee() != null) {
                stmt.setLong(5, issue.getAssignee().getUserId());
            } else {
                stmt.setNull(5, Types.BIGINT);
            }
            stmt.setString(6, issue.getIssueType().name());
            stmt.setString(7, issue.getPriority().name());
            stmt.setLong(8, issue.getCurrentState().getId());
            stmt.setLong(9, issue.getProject().getId());
            if (issue.getResolvedAt() != null) {
                stmt.setTimestamp(10, Timestamp.valueOf(issue.getResolvedAt()));
            } else {
                stmt.setNull(10, Types.TIMESTAMP);
            }

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    issue.setId(keys.getLong(1));
                }
            }
            logger.info("Inserted issue: {}", issue.getIssueKey());
            return issue;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert issue", e);
        }
    }

    private Issue update(Issue issue) {
        String sql = """
            UPDATE issues SET summary = ?, description = ?, assignee_id = ?,
                              priority = ?, state_id = ?, resolved_at = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, issue.getSummary());
            stmt.setString(2, issue.getDescription());
            if (issue.getAssignee() != null) {
                stmt.setLong(3, issue.getAssignee().getUserId());
            } else {
                stmt.setNull(3, Types.BIGINT);
            }
            stmt.setString(4, issue.getPriority().name());
            stmt.setLong(5, issue.getCurrentState().getId());
            if (issue.getResolvedAt() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(issue.getResolvedAt()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            stmt.setLong(7, issue.getId());
            stmt.executeUpdate();

            logger.info("Updated issue: {}", issue.getIssueKey());
            return issue;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update issue", e);
        }
    }

    public Optional<Issue> findByKey(String key) {
        String sql = "SELECT * FROM issues WHERE issue_key = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapIssue(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find issue by key", e);
        }
    }

    public List<Issue> findByProject(Long projectId) {
        String sql = "SELECT * FROM issues WHERE project_id = ? ORDER BY id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Issue> issues = new ArrayList<>();
                while (rs.next()) {
                    issues.add(mapIssue(rs));
                }
                return issues;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find issues by project", e);
        }
    }

    public void delete(Long id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM issues WHERE id = ?")) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
            logger.info("Deleted issue: {}", id);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete issue", e);
        }
    }

    private Issue mapIssue(ResultSet rs) throws SQLException {
        User reporter = userRepository.findById(rs.getLong("reporter_id"))
                .orElseThrow(() -> new RuntimeException("Reporter not found"));

        State state = stateRepository.findById(rs.getLong("state_id"))
                .orElseThrow(() -> new RuntimeException("State not found"));

        Issue issue = new Issue(
                new IssueKey(rs.getString("issue_key")),
                rs.getString("summary"),
                rs.getString("description"),
                reporter,
                IssueType.valueOf(rs.getString("issue_type")),
                IssuePriority.valueOf(rs.getString("priority")),
                state
        );
        issue.setId(rs.getLong("id"));

        long assigneeId = rs.getLong("assignee_id");
        if (!rs.wasNull()) {
            userRepository.findById(assigneeId).ifPresent(issue::assignTo);
        }

        long projectId = rs.getLong("project_id");
        projectRepository.findByKey(rs.getString("issue_key").split("-")[0])
                .ifPresent(issue::setProject);

        return issue;
    }
}