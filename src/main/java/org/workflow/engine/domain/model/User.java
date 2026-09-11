package org.workflow.engine.domain.model;


import jakarta.persistence.*;
import org.workflow.engine.domain.enums.UserRole;
import org.workflow.engine.domain.value.EmailConverter;
import org.workflow.engine.domain.valueobject.Email;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Convert(converter = EmailConverter.class)
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private Email email;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "active", nullable = false)
    private boolean active;

    @ManyToMany(mappedBy = "members")
    private Set<Workspace> workspaces = new HashSet<>();

    protected User() {
        this.workspaces = new HashSet<>();
    }

    public User(String username, Email email, String displayName, UserRole role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        this.username = username;
        this.email = email;
        this.displayName = displayName!=null ? displayName : username;
        this.role = role;
        this.active = true;
        this.workspaces = new HashSet<>();
    }

    public void addToWorkspace(Workspace workspace) {
        if(workspace==null) {
            throw new IllegalArgumentException("Workspace cannot be null");
        }
        workspaces.add(workspace);
    }

    public void removeFromWorkspace(Workspace workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("Workspace cannot be null");
        }
        workspaces.remove(workspace);
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public Long getVersion() { return version; }

    public Long getUserId() {
        return userId;
    }
    public void setId(Long id) {
        this.userId = id;
    }

    public String getUsername() {
        return username;
    }

    public Email getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        if (role == null) throw new IllegalArgumentException("Role cannot be null");
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public Set<Workspace> getWorkspaces() {
        return new HashSet<>(workspaces);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId) ||
                Objects.equals(username, user.username) ||
                Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, email);
    }

    @Override
    public String toString() {
        return "User{user_id=" + userId + ", username='" + username + "', role=" + role + "}";
    }

}
