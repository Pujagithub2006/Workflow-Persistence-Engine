package org.workflow.engine.domain.model;

import jakarta.persistence.*;
import org.workflow.engine.domain.enums.IssueStatus;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
        name = "states",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"workflow_id", "name"}
        )
)
public class State {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IssueStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @OneToMany(mappedBy = "fromState", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Transition> outboundTransitions = new HashSet<>();

    @OneToMany(mappedBy = "toState")
    private Set<Transition> inboundTransitions = new HashSet<>();

    @Column(name = "is_initial", nullable = false)
    private boolean initial;

    protected State() {
        this.outboundTransitions = new HashSet<>();
        this.inboundTransitions = new HashSet<>();
    }

    public State(String name, String description, IssueStatus status) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("State name cannot be null or empty");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.name = name;
        this.description = description;
        this.status = status;
        this.outboundTransitions = new HashSet<>();
        this.inboundTransitions = new HashSet<>();
    }

    public void addOutboundTransition(Transition transition) {
        outboundTransitions.add(transition);
    }

    public void addInboundTransition(Transition transition) {
        inboundTransitions.add(transition);
    }

    public boolean canTransitionTo(State targetState) {
        return outboundTransitions.stream()
                .anyMatch(t->t.getToState().equals(targetState));
    }

    public boolean isTerminal() {
        return status.isTerminal();
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

    public IssueStatus getStatus() {
        return status;
    }
    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public Workflow getWorkflow() {
        return workflow;
    }
    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public Set<Transition> getOutboundTransitions() {
        return new HashSet<>(outboundTransitions);
    }

    public Set<Transition> getInboundTransitions() {
        return new HashSet<>(inboundTransitions);
    }

    public boolean isInitial() {
        return initial;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return Objects.equals(id, state.id) ||
                (Objects.equals(name, state.name) && Objects.equals(workflow, state.workflow)); // combination of a state and workflow makes it unique
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, workflow);
    }

    @Override
    public String toString() {
        return "State{name='" + name + "', status=" + status + "}";
    }

}
