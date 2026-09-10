package org.workflow.engine.persistence.repositories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workflow.engine.domain.model.Project;
import org.workflow.engine.domain.model.User;
import org.workflow.engine.persistence.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectRepository {
    private static final Logger logger = LoggerFactory.getLogger(ProjectRepository.class);
    private final UserRepository userRepository = new UserRepository();

    public Project save(Project project) {
        if (project.getId() == null) {
            return insert(project);
        }
        return update(project);
    }

    private Project insert(Project project) {
        String sql = """
            INSERT INTO projects (key, name, description, workspace_id, project_lead_id, next_issue_number)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, project.getKey());
            stmt.setString(2, project.getName());
            stmt.setString(3, project.getDescription());
            stmt.setLong(4, project.getWorkspace().getId());
            stmt.setLong(5, project.getProjectLead().getUserId());
            stmt.setLong(6, project.getIssues().size() + 1L); // simple next issue number

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    project.setId(keys.getLong(1));
                }
            }
            saveTeamMembers(conn, project);

            logger.info("Inserted project: {}", project.getKey());
            return project;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert project", e);
        }
    }

    private Project update(Project project) {
        String sql = """
            UPDATE projects SET name = ?, description = ?, workspace_id = ?, project_lead_id = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, project.getName());
            stmt.setString(2, project.getDescription());
            stmt.setLong(3, project.getWorkspace().getId());
            stmt.setLong(4, project.getProjectLead().getUserId());
            stmt.setLong(5, project.getId());
            stmt.executeUpdate();

            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM project_team WHERE project_id = ?")) {
                del.setLong(1, project.getId());
                del.executeUpdate();
            }
            saveTeamMembers(conn, project);

            logger.info("Updated project: {}", project.getKey());
            return project;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update project", e);
        }
    }

    private void saveTeamMembers(Connection conn, Project project) throws SQLException {
        String sql = "INSERT INTO project_team (project_id, user_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (User user : project.getTeamMembers()) {
                stmt.setLong(1, project.getId());
                stmt.setLong(2, user.getUserId());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public Optional<Project> findByKey(String key) {
        String sql = "SELECT * FROM projects WHERE key = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapProject(conn, rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find project by key", e);
        }
    }

    public List<Project> findByWorkspace(Long workspaceId) {
        String sql = "SELECT * FROM projects WHERE workspace_id = ? ORDER BY name";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, workspaceId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Project> projects = new ArrayList<>();
                while (rs.next()) {
                    projects.add(mapProject(conn, rs));
                }
                return projects;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find projects by workspace", e);
        }
    }

    public void delete(Long id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM projects WHERE id = ?")) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
            logger.info("Deleted project: {}", id);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete project", e);
        }
    }

    private Project mapProject(Connection conn, ResultSet rs) throws SQLException {
        User lead = userRepository.findById(rs.getLong("project_lead_id"))
                .orElseThrow(() -> new RuntimeException("Project lead not found"));

        Project project = new Project(
                rs.getString("key"),
                rs.getString("name"),
                rs.getString("description"),
                lead
        );
        project.setId(rs.getLong("id"));

        // Load team members
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT user_id FROM project_team WHERE project_id = ?")) {
            stmt.setLong(1, project.getId());
            try (ResultSet rs2 = stmt.executeQuery()) {
                while (rs2.next()) {
                    Long userId = rs2.getLong("user_id");
                    if (!userId.equals(lead.getUserId())) {
                        userRepository.findById(userId).ifPresent(project::addTeamMember);
                    }
                }
            }
        }
        return project;
    }
}
