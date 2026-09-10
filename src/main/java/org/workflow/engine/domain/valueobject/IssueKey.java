package org.workflow.engine.domain.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

public class IssueKey {
    private static final Pattern ISSUEKEY_PATTERN = Pattern.compile("^[A-Z]{2,10}-[1-9][0-9]*$");

    private final String value;

    public IssueKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Issue key cannot be null or empty");
        }
        if (!ISSUEKEY_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid issue key format: " + value);
        }
        this.value = value;
    }

    public IssueKey(String projectKey, long issueNumber) {
        this(projectKey + "-" + issueNumber);
    }

    public String getValue() {
        return value;
    }

    public String getProjectKey() {
        return value.substring(0, value.indexOf("-"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IssueKey issueKey = (IssueKey) o;
        return Objects.equals(value, issueKey.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
