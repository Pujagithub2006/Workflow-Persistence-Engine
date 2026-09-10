package org.workflow.engine.domain.enums;

public enum IssueStatus {
    TO_DO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE,
    CLOSED,
    REOPENED;

    public boolean isTerminal() {
        return this==DONE || this==CLOSED;
    }

}
