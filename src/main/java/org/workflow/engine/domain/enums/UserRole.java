package org.workflow.engine.domain.enums;

public enum UserRole {
    ADMIN,
    PROJECT_LEAD,
    DEVELOPER,
    TESTER,
    VIEWER;

    public boolean canManageProject() {
        return this==ADMIN || this==PROJECT_LEAD;
    }

    public boolean canEditIssues() {
        return this!=VIEWER;
    }

}
