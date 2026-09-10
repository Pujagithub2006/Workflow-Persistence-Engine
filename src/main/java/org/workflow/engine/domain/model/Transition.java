package org.workflow.engine.domain.model;

import java.util.Objects;

public class Transition {

    private Long id;
    private String name;
    private String description;
    private State fromState;
    private State toState;

    public Transition(String name, String description, State fromState, State toState) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Transition name cannot be null or empty");
        }
        if (fromState == null) {
            throw new IllegalArgumentException("From state cannot be null");
        }
        if (toState == null) {
            throw new IllegalArgumentException("To state cannot be null");
        }
        if(fromState.equals(toState)) {
            throw new IllegalArgumentException("From and to states must be different");
        }

        this.name = name;
        this.description = description;
        this.fromState = fromState;
        this.toState = toState;

        fromState.addOutboundTransition(this);
        toState.addInboundTransition(this);
    }

    public boolean canExecute(Issue issue) {
        return issue!=null && issue.getCurrentState().equals(fromState);
    }

    public void execute(Issue issue, User user) {
        if(!canExecute(issue)) {
            throw new IllegalStateException("Cannot execute transition: issue not in correct from state");
        }
        issue.transitionTo(toState, user);
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
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public State getFromState() {
        return fromState;
    }

    public State getToState() {
        return toState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transition that = (Transition) o;
        return Objects.equals(id, that.id) ||
                (Objects.equals(fromState, that.fromState) && Objects.equals(toState, that.toState));
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromState, toState);
    }

    @Override
    public String toString() {
        return "Transition{" + fromState.getName() + " -> " + toState.getName() + "}";
    }

}
