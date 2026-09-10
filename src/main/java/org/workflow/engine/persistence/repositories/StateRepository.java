package org.workflow.engine.persistence.repositories;

import org.workflow.engine.domain.enums.IssueStatus;
import org.workflow.engine.domain.model.State;
import org.workflow.engine.persistence.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StateRepository {

    public State save(State state, Long workflowId) {
        if (state.getId() == null) {
            return insert(state, workflowId);
        }
        return update(state, workflowId);
    }

    private State insert(State state, Long workflowId) {
        String sql = """
            INSERT INTO states (name, description, status, workflow_id, is_initial)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, state.getName());
            stmt.setString(2, state.getDescription());
            stmt.setString(3, state.getStatus().name());
            stmt.setLong(4, workflowId);
            stmt.setBoolean(5, state.isInitial());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    state.setId(keys.getLong(1));
                }
            }
            return state;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert state", e);
        }
    }

    private State update(State state, Long workflowId) {
        String sql = """
            UPDATE states SET name = ?, description = ?, status = ?, workflow_id = ?, is_initial = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, state.getName());
            stmt.setString(2, state.getDescription());
            stmt.setString(3, state.getStatus().name());
            stmt.setLong(4, workflowId);
            stmt.setBoolean(5, state.isInitial());
            stmt.setLong(6, state.getId());
            stmt.executeUpdate();
            return state;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update state", e);
        }
    }

    public Optional<State> findById(Long id) {
        String sql = "SELECT * FROM states WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapState(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find state by id", e);
        }
    }

    public List<State> findByWorkflow(Long workflowId) {
        String sql = "SELECT * FROM states WHERE workflow_id = ? ORDER BY id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, workflowId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<State> states = new ArrayList<>();
                while (rs.next()) {
                    states.add(mapState(rs));
                }
                return states;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find states by workflow", e);
        }
    }

    private State mapState(ResultSet rs) throws SQLException {
        State state = new State(
                rs.getString("name"),
                rs.getString("description"),
                IssueStatus.valueOf(rs.getString("status"))
        );
        state.setId(rs.getLong("id"));
        state.setInitial(rs.getBoolean("is_initial"));
        return state;
    }
}