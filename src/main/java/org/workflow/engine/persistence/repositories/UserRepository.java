package org.workflow.engine.persistence.repositories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workflow.engine.domain.enums.UserRole;
import org.workflow.engine.domain.model.User;
import org.workflow.engine.domain.valueobject.Email;
import org.workflow.engine.persistence.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    public User save(User user) {
        if (user.getUserId() == null) {
            return insert(user);
        }
        return update(user);
    }

    private User insert(User user) {
        String sql = """
            INSERT INTO users (username, email, display_name, role, active)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail().getValue());
            stmt.setString(3, user.getDisplayName());
            stmt.setString(4, user.getRole().name());
            stmt.setBoolean(5, user.isActive());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            logger.info("Inserted user: {}", user.getUsername());
            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert user", e);
        }
    }

    private User update(User user) {
        String sql = """
            UPDATE users SET username = ?, email = ?, display_name = ?, role = ?, active = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail().getValue());
            stmt.setString(3, user.getDisplayName());
            stmt.setString(4, user.getRole().name());
            stmt.setBoolean(5, user.isActive());
            stmt.setLong(6, user.getUserId());

            stmt.executeUpdate();
            logger.info("Updated user: {}", user.getUsername());
            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user", e);
        }
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try(Connection conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
            return Optional.empty();

        } catch(Exception e) {
            throw new RuntimeException("Failed to find user by id", e);
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by username", e);
        }
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY username";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<User> users = new ArrayList<>();
            while(rs.next()) {
                users.add(mapUser(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all users", e);
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
            logger.info("Deleted user: {}", id);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getString("username"),
                new Email(rs.getString("email")),
                rs.getString("display_name"),
                UserRole.valueOf(rs.getString("role"))
        );
        user.setId(rs.getLong("id"));
        if (!rs.getBoolean("active")) {
            user.deactivate();
        }
        return user;
    }
}
