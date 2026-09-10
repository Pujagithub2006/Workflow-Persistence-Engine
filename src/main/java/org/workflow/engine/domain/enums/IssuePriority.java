package org.workflow.engine.domain.enums;

public enum IssuePriority {
    TRIVIAL(0),
    MINOR(1),
    MAJOR(2),
    CRITICAL(3),
    BLOCKER(4);

    private final int level;

    IssuePriority(int level) {
        this.level = level;
    }

    private int getLevel() {
        return level;
    }

    public boolean isHigherThan(IssuePriority otherPrio) {
        return this.level > otherPrio.level;
    }

}
