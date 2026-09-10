package org.workflow.engine.persistence.repositories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workflow.engine.domain.model.User;
import org.workflow.engine.domain.model.Workspace;
import org.workflow.engine.persistence.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkspaceRepository {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceRepository.class);
    private final UserRepository userRepository = new UserRepository();

    public Workspace save(Workspace workspace) {
        if (workspace.getId() == null) {
            return insert(workspace);
        }
        return update(workspace);
    }

    private Workspace insert(Workspace workspace) {
        String sql = """
            INSERT INTO workspaces (name, description, owner_id)
            VALUES (?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, workspace.getName());
            stmt.setString(2, workspace.getDescription());
            stmt.setLong(3, workspace.getOwner().getUserId());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    workspace.setId(keys.getLong(1));
                }
            }

            // Persist members via join table
            saveMembers(conn, workspace);

            logger.info("Inserted workspace: {}", workspace.getName());
            return workspace;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert workspace", e);
        }
    }

    private Workspace update(Workspace workspace) {
        String sql = "UPDATE workspaces SET name = ?, description = ?, owner_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, workspace.getName());
            stmt.setString(2, workspace.getDescription());
            stmt.setLong(3, workspace.getOwner().getUserId());
            stmt.setLong(4, workspace.getId());
            stmt.executeUpdate();

            // Simplest approach: clear and re-insert members
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM workspace_members WHERE workspace_id = ?")) {
                del.setLong(1, workspace.getId());
                del.executeUpdate();
            }
            saveMembers(conn, workspace);

            logger.info("Updated workspace: {}", workspace.getName());
            return workspace;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update workspace", e);
        }
    }

    private void saveMembers(Connection conn, Workspace workspace) throws SQLException {
        String sql = "INSERT INTO workspace_members (workspace_id, user_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (User member : workspace.getMembers()) {
                stmt.setLong(1, workspace.getId());
                stmt.setLong(2, member.getUserId());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public Optional<Workspace> findById(Long id) {
        String sql = "SELECT * FROM workspaces WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapWorkspace(conn, rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find workspace by id", e);
        }
    }

    public List<Workspace> findAll() {
        String sql = "SELECT * FROM workspaces ORDER BY name";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Workspace> workspaces = new ArrayList<>();
            while (rs.next()) {
                workspaces.add(mapWorkspace(conn, rs));
            }
            return workspaces;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all workspaces", e);
        }
    }

    public void delete(Long id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM workspaces WHERE id = ?")) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
            logger.info("Deleted workspace: {}", id);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete workspace", e);
        }
    }

    private Workspace mapWorkspace(Connection conn, ResultSet rs) throws SQLException {
        User owner = userRepository.findById(rs.getLong("owner_id"))
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Workspace workspace = new Workspace(
                rs.getString("name"),
                rs.getString("description"),
                owner
        );
        workspace.setId(rs.getLong("id"));

        // Load members
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT user_id FROM workspace_members WHERE workspace_id = ?")) {
            stmt.setLong(1, workspace.getId());
            try (ResultSet rs2 = stmt.executeQuery()) {
                while (rs2.next()) {
                    Long userId = rs2.getLong("user_id");
                    if (!userId.equals(owner.getUserId())) {
                        userRepository.findById(userId).ifPresent(workspace::addMember);
                    }
                }
            }
        }
        return workspace;
    }
}
