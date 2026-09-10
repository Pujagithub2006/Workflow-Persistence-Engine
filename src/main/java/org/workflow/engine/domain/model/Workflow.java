package org.workflow.engine.domain.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Workflow {
    private long id;
    private String name;
    private String description;
    private State initialState;
    private Set<State> states;

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
        if (state.equals(initialState)) {
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
        this.initialState = state;
    }

    public State getStateByName(String name) {
        return states.stream()
                .filter(s->s.getName().equals(name))
                .findFirst()
                .orElse(null);
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

    public Set<State> getStates() {
        return new HashSet<>(states);
    }

    public State getInitialState() {
        return initialState;
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
