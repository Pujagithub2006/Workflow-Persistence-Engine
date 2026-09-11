package org.workflow.engine.domain.model;

import jakarta.persistence.*;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

@Entity
@Table(
        name = "transitions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"from_state_id", "to_state_id"}
        )

)
public class Transition {
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_state_id", nullable = false)
    private State fromState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_state_id", nullable = false)
    private State toState;

    protected Transition() {

    }

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
