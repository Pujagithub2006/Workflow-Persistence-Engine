-- Foreign keys (Hibernate doesn't always create these)
CREATE INDEX IF NOT EXISTS idx_projects_workspace ON projects(workspace_id);
CREATE INDEX IF NOT EXISTS idx_projects_lead ON projects(project_lead_id);
CREATE INDEX IF NOT EXISTS idx_issues_project ON issues(project_id);
CREATE INDEX IF NOT EXISTS idx_issues_assignee ON issues(assignee_id);
CREATE INDEX IF NOT EXISTS idx_issues_state ON issues(state_id);
CREATE INDEX IF NOT EXISTS idx_comments_issue ON comments(issue_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_issue ON audit_logs(issue_id);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_issues_project_state ON issues(project_id, state_id);
CREATE INDEX IF NOT EXISTS idx_issues_project_priority ON issues(project_id, priority);

-- Partial index for "active issues" queries
CREATE INDEX IF NOT EXISTS idx_issues_active ON issues(project_id, updated_at DESC)
    WHERE resolved_at IS NULL;

-- Sort indexes for common ORDER BY
CREATE INDEX IF NOT EXISTS idx_issues_created_at ON issues(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_created_at ON comments(created_at);

-- Update statistics for planner
ANALYZE users;
ANALYZE workspaces;
ANALYZE projects;
ANALYZE issues;
ANALYZE comments;
ANALYZE audit_logs;