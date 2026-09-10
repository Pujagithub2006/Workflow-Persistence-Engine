package org.workflow.engine.domain.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Workspace {
    private Long id;
    private String name;
    private String description;
    private User owner;
    private Set<User> members;
    private Set<Project> projects;

    public Workspace(String name, String description, User owner) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workspace name cannot be null or empty");
        }
        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }

        this.name = name;
        this.description = description;
        this.owner = owner;
        this.members = new HashSet<>();
        this.projects = new HashSet<>();

        // bidirectional relationship
        this.members.add(owner);
        owner.addToWorkspace(this);
    }

    public void addMember(User user) {
        if(user==null) {
            throw new IllegalArgumentException(("User cannot be null"));
        }
        if(members.add(user)) {
            user.addToWorkspace(this);
        }
    }

    public void removeMember(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.equals(owner)) {
            throw new IllegalStateException("Cannot remove workspace owner");
        }
        if (members.remove(user)) {
            user.removeFromWorkspace(this);
        }
    }

    public void addProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        if (projects.add(project)) {
            project.setWorkspace(this);
        }
    }

    public void removeProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        if (projects.remove(project)) {
            project.setWorkspace(null);
        }
    }

    public boolean hasMember(User user) {
        return members.contains(user);
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
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

    public User getOwner() {
        return owner;
    }

    public Set<User> getMembers() {
        return new HashSet<>(members);
    }

    public Set<Project> getProjects() {
        return new HashSet<>(projects);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Workspace workspace = (Workspace) o;
        return Objects.equals(id, workspace.id) || Objects.equals(name, workspace.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Workspace{id=" + id + ", name='" + name + "'}";
    }

}

