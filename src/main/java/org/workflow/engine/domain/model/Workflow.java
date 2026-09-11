package org.workflow.engine.domain.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "workflows")
public class Workflow {
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<State> states = new HashSet<>();

    protected Workflow() {
        this.states = new HashSet<>();
    }

    public Workflow(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workflow name cannot be null or empty");
        }
        this.name = name;
        this.description = description;
        this.states = new HashSet<>();
    }

    public void addState(State state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        if(states.add(state)) {
            state.setWorkflow(this);
        }
    }

    public void removeState(State state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        if (state.equals(
                states.stream()
                        .filter(State::isInitial)
                        .findFirst()
                        .orElse(null)
        )) {
            throw new IllegalStateException("Cannot remove initial state");
        }
        if (states.remove(state)) {
            state.setWorkflow(null);
        }
    }

    public void setInitialState(State state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        if (!states.contains(state)) {
            throw new IllegalArgumentException("State must be part of this workflow");
        }

        // clear old initial flag
        states.stream()
                .filter(State::isInitial)
                .forEach(s->s.setInitial(false));
        // set new initial
        state.setInitial(true);
    }

    public State getStateByName(String name) {
        return states.stream()
                .filter(s->s.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public Long getVersion() { return version; }

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

    public Set<State> getStates() {
        return new HashSet<>(states);
    }

    public State getInitialState() {
        return states.stream()
                .filter(State::isInitial)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Workflow workflow = (Workflow) o;
        return Objects.equals(id, workflow.id) || Objects.equals(name, workflow.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Workflow{id=" + id + ", name='" + name + "'}";
    }
}
